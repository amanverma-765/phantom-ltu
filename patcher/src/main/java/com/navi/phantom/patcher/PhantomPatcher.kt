package com.navi.phantom.patcher

import co.touchlab.kermit.Logger
import com.android.tools.build.apkzlib.sign.SigningExtension
import com.android.tools.build.apkzlib.sign.SigningOptions
import com.android.tools.build.apkzlib.zip.AlignmentRules
import com.android.tools.build.apkzlib.zip.ZFile
import com.android.tools.build.apkzlib.zip.ZFileOptions
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.navi.phantom.patcher.util.ApkSignatureHelper
import com.navi.phantom.patcher.util.ManifestParser
import com.navi.phantom.shared.ApksBundleHelper
import com.navi.phantom.shared.Constants
import com.navi.phantom.shared.Constants.CONFIG_ASSET_PATH
import com.navi.phantom.shared.Constants.ORIGINAL_APK_ASSET_PATH
import com.navi.phantom.shared.Constants.PROXY_APP_COMPONENT_FACTORY
import com.navi.phantom.shared.LSPConfig
import com.navi.phantom.shared.PatchConfig
import com.wind.meditor.core.ManifestEditor
import com.wind.meditor.property.AttributeItem
import com.wind.meditor.property.ModificationProperty
import com.wind.meditor.utils.NodeValue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FilenameUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.Locale

class PhantomPatcher(args: Array<String>) {

    private val log = Logger.withTag("Phantom")

    /**
     * Callback interface for reporting patching progress.
     */
    fun interface ProgressCallback {
        fun onProgress(step: String, details: String)
    }

    /**
     * Represents a recoverable patching error.
     * Extends Exception (not Error) because these are application-level errors
     * that should be caught and displayed to the user, not JVM errors.
     */
    class PatchError : Exception {
        constructor(message: String, cause: Throwable) : super(message, cause)
        constructor(message: String) : super(message)
    }

    @Parameter(description = "apk(s) or .apks bundle(s)")
    private var apkPaths: MutableList<String> = mutableListOf()

    @Parameter(names = ["-h", "--help"], help = true, order = 0, description = "Print this message")
    private var help = false

    @Parameter(names = ["-o", "--output"], description = "Output directory")
    private var outputPath = "."

    @Parameter(names = ["-f", "--force"], description = "Force overwrite exists output file")
    private var forceOverwrite = false

    @Parameter(names = ["-d", "--debuggable"], description = "Set app to be debuggable")
    private var debuggableFlag = false

    @Parameter(names = ["-l", "--sigbypasslv"], description = "Signature bypass level. 0 (disable), 1 (pm), 2 (pm+openat). default 0")
    private var sigbypassLevel = 0

    @Parameter(names = ["--injectdex"], description = "Inject directly the loader dex file into the original application package")
    private var injectDex = false

    @Parameter(names = ["-k", "--keystore"], arity = 4, description = "Set custom signature keystore. Followed by 4 arguments: keystore path, keystore password, keystore alias, keystore alias password")
    private var keystoreArgs: MutableList<String?> = mutableListOf(null, "123456", "key0", "123456")

    @Parameter(names = ["-r", "--allowdown"], description = "Allow downgrade installation by overriding versionCode to 1 (In most cases, the app can still get the correct versionCode)")
    private var overrideVersionCode = false

    @Parameter(names = ["-v", "--verbose"], description = "Verbose output")
    private var verbose = false

    private val jCommander: JCommander

    init {
        jCommander = JCommander.newBuilder().addObject(this).build()
        try {
            jCommander.parse(*args)
        } catch (e: ParameterException) {
            log.e { e.message ?: "Parameter error" }
            help = true
        }
        if (apkPaths.isEmpty()) {
            log.e { "No apk or .apks bundle specified" }
            help = true
        }
    }

    @Throws(PatchError::class, IOException::class)
    fun doCommandLine() {
        val outputDir = File(outputPath)
        outputDir.mkdirs()

        val expandedApkPaths = mutableListOf<String>()
        var tempDir: File? = null

        for (path in apkPaths) {
            if (ApksBundleHelper.isApksBundleAny(path)) {
                log.i { "Extracting bundle: $path" }
                if (tempDir == null) {
                    tempDir = Files.createTempDirectory("phantom-bundle-").toFile()
                    Runtime.getRuntime().addShutdownHook(Thread {
                        tempDir?.walkBottomUp()?.forEach { it.delete() }
                    })
                }
                val extracted = ApksBundleHelper.extractBundle(File(path), tempDir)
                log.i { "Extracted ${extracted.size} APK(s) from bundle" }
                for (apkPath in extracted) {
                    log.d { "  - ${File(apkPath).name}" }
                }
                expandedApkPaths.addAll(extracted)
            } else {
                expandedApkPaths.add(path)
            }
        }

        apkPaths = expandedApkPaths

        val patchedFiles = mutableListOf<File>()
        val originalNames = mutableMapOf<File, String>()
        var basePackageName: String? = null

        for (apk in apkPaths) {
            val srcApkFile = File(apk).absoluteFile
            val apkFileName = srcApkFile.name

            val outputFile = File(
                outputDir,
                String.format(
                    Locale.getDefault(),
                    "%s-%d%s",
                    FilenameUtils.getBaseName(apkFileName),
                    LSPConfig.instance.VERSION_CODE,
                    Constants.PATCH_FILE_SUFFIX
                )
            ).absoluteFile

            if (outputFile.exists() && !forceOverwrite) {
                throw PatchError("$outputPath exists. Use --force to overwrite")
            }
            log.i { "Processing $srcApkFile -> $outputFile" }

            patch(srcApkFile, outputFile)
            patchedFiles.add(outputFile)
            originalNames[outputFile] = apkFileName

            if (basePackageName == null && !apkFileName.startsWith("split_")) {
                try {
                    ZFile.openReadOnly(srcApkFile).use { zf ->
                        val manifestEntry = zf.get(ANDROID_MANIFEST_XML)
                        if (manifestEntry != null) {
                            manifestEntry.open().use { inputStream ->
                                val parsed = ManifestParser.parseManifestFile(inputStream).getOrNull()
                                if (parsed?.packageName != null) {
                                    basePackageName = parsed.packageName
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.d { "Could not extract package name: ${e.message}" }
                }
            }
        }

        if (patchedFiles.size > 1) {
            val bundleName = basePackageName ?: "bundle"
            val bundleFile = File(
                outputDir,
                String.format(
                    Locale.getDefault(),
                    "%s-%d%s",
                    bundleName,
                    LSPConfig.instance.VERSION_CODE,
                    Constants.PATCH_BUNDLE_SUFFIX
                )
            ).absoluteFile

            log.i { "Creating bundle: ${bundleFile.name}" }
            for (apkFile in patchedFiles) {
                val originalName = originalNames[apkFile] ?: apkFile.name
                log.d { "Adding to bundle: ${apkFile.name} -> $originalName" }
            }

            ApksBundleHelper.createBundle(patchedFiles, bundleFile, originalNames, true)
            log.i { "Done. Output bundle: ${bundleFile.absolutePath}" }
        } else if (patchedFiles.size == 1) {
            log.i { "Done. Output APK: ${patchedFiles[0].absolutePath}" }
        }
    }

    @Throws(PatchError::class, IOException::class)
    fun patch(srcApkFile: File, outputFile: File, onProgress: ProgressCallback? = null) {
        if (!srcApkFile.exists()) {
            throw PatchError("The source apk file does not exist. Please provide a correct path.")
        }

        outputFile.delete()

        log.d { "apk path: $srcApkFile" }
        log.i { "Parsing original apk..." }
        onProgress?.onProgress("PARSE_APK", srcApkFile.name)

        ZFile.openReadWrite(outputFile, Z_FILE_OPTIONS).use { dstZFile ->
            val srcZFile = dstZFile.addNestedZip({ ORIGINAL_APK_ASSET_PATH }, srcApkFile, false)

            try {
                onProgress?.onProgress("SETUP_SIGNING", "Configuring APK signature")
                // Use BKS (BouncyCastle KeyStore) format for Android
                // The keystore file was created with: keytool -storetype BKS -providerclass org.bouncycastle.jce.provider.BouncyCastleProvider
                val keyStore = KeyStore.getInstance("BKS")
                if (keystoreArgs[0] == null) {
                    log.i { "Register apk signer with default keystore..." }
                    val keystoreStream = javaClass.classLoader?.getResourceAsStream("assets/keystore")
                        ?: throw PatchError("Default keystore resource not found")
                    keystoreStream.use { inputStream ->
                        keyStore.load(inputStream, keystoreArgs[1]!!.toCharArray())
                    }
                } else {
                    log.i { "Register apk signer with custom keystore..." }
                    FileInputStream(keystoreArgs[0]!!).use { inputStream ->
                        keyStore.load(inputStream, keystoreArgs[1]!!.toCharArray())
                    }
                }
                val entry = keyStore.getEntry(
                    keystoreArgs[2],
                    KeyStore.PasswordProtection(keystoreArgs[3]!!.toCharArray())
                ) as KeyStore.PrivateKeyEntry

                SigningExtension(
                    SigningOptions.builder()
                        .setMinSdkVersion(28)
                        .setV2SigningEnabled(true)
                        .setCertificates(*entry.certificateChain.filterIsInstance<X509Certificate>().toTypedArray())
                        .setKey(entry.privateKey)
                        .build()
                ).register(dstZFile)
            } catch (e: Exception) {
                throw PatchError("Failed to register signer", e)
            }

            var originalSignature: String? = null
            if (sigbypassLevel > 0) {
                onProgress?.onProgress("EXTRACT_SIGNATURE", "For signature bypass")
                originalSignature = ApkSignatureHelper.getApkSignInfo(srcApkFile.absolutePath)
                if (originalSignature.isNullOrEmpty()) {
                    throw PatchError("get original signature failed")
                }
                log.d { "Original signature\n$originalSignature" }
            }

            val manifestEntry = srcZFile.get(ANDROID_MANIFEST_XML)
                ?: throw PatchError("Provided file is not a valid apk")

            val appComponentFactory: String?
            val minSdkVersion: Int
            manifestEntry.open().use { inputStream ->
                val pair = ManifestParser.parseManifestFile(inputStream)
                    .getOrElse { throw PatchError("Failed to parse AndroidManifest.xml", it) }
                appComponentFactory = pair.appComponentFactory
                minSdkVersion = pair.minSdkVersion
                log.d { "original appComponentFactory class: $appComponentFactory" }
                log.d { "original minSdkVersion: $minSdkVersion" }
            }

            val skipSplit = apkPaths.size > 1 && srcApkFile.name.startsWith("split_") && appComponentFactory == null
            if (skipSplit) {
                log.i { "Packing split apk..." }

                if (overrideVersionCode) {
                    log.d { "Updating split apk versionCode to 1" }
                    val property = ModificationProperty()
                    property.addManifestAttribute(AttributeItem(NodeValue.Manifest.VERSION_CODE, 1))
                    val os = ByteArrayOutputStream()
                    ManifestEditor(manifestEntry.open(), os, property).processManifest()
                    os.flush()
                    os.close()
                    dstZFile.add(ANDROID_MANIFEST_XML, ByteArrayInputStream(os.toByteArray()))
                }

                for (entry in srcZFile.entries()) {
                    val name = entry.centralDirectoryHeader.name
                    if (dstZFile.get(name) != null) continue
                    if (name.startsWith("META-INF") && (name.endsWith(".SF") || name.endsWith(".MF") || name.endsWith(".RSA"))) {
                        continue
                    }
                    srcZFile.addFileLink(name, name)
                }
                return
            }

            log.i { "Patching apk..." }
            onProgress?.onProgress("MODIFY_MANIFEST", "Injecting component factory")

            val config = PatchConfig(
                debuggableFlag,
                overrideVersionCode,
                sigbypassLevel,
                originalSignature,
                appComponentFactory
            )
            val configBytes = Json.encodeToString(config).toByteArray(StandardCharsets.UTF_8)
            val metadata = Base64.getEncoder().encodeToString(configBytes)

            try {
                ByteArrayInputStream(modifyManifestFile(manifestEntry.open(), metadata, minSdkVersion)).use { inputStream ->
                    dstZFile.add(ANDROID_MANIFEST_XML, inputStream)
                }
            } catch (e: Throwable) {
                throw PatchError("Error when modifying manifest", e)
            }

            log.i { "Adding config..." }
            onProgress?.onProgress("ADD_CONFIG", "Embedding bootstrap configuration")
            try {
                ByteArrayInputStream(configBytes).use { inputStream ->
                    dstZFile.add(CONFIG_ASSET_PATH, inputStream)
                }
            } catch (e: Throwable) {
                throw PatchError("Error when saving config")
            }

            log.i { "Adding metaloader dex..." }
            onProgress?.onProgress("ADD_METALOADER", "Injecting metaloader.dex")
            try {
                val metaLoaderStream = javaClass.classLoader?.getResourceAsStream(Constants.META_LOADER_DEX_ASSET_PATH)
                    ?: throw PatchError("Meta loader dex resource not found")
                metaLoaderStream.use { inputStream ->
                    if (!injectDex) {
                        dstZFile.add("classes.dex", inputStream)
                    } else {
                        val dexCount = srcZFile.entries().count { entry ->
                            val name = entry.centralDirectoryHeader.name
                            name.startsWith("classes") && name.endsWith(".dex")
                        } + 1
                        dstZFile.add("classes$dexCount.dex", inputStream)
                    }
                }
            } catch (e: Throwable) {
                throw PatchError("Error when adding dex", e)
            }

            log.d { "Creating nested apk link..." }
            onProgress?.onProgress("CREATE_LINKS", "Linking original APK entries")

            for (entry in srcZFile.entries()) {
                val name = entry.centralDirectoryHeader.name
                if (dstZFile.get(name) != null) continue
                if (!injectDex && name.startsWith("classes") && name.endsWith(".dex")) continue
                if (name == "AndroidManifest.xml") continue
                if (name.startsWith("META-INF") && (name.endsWith(".SF") || name.endsWith(".MF") || name.endsWith(".RSA"))) continue
                srcZFile.addFileLink(name, name)
            }

            dstZFile.realign()

            log.i { "Writing apk..." }
            onProgress?.onProgress("WRITE_APK", "Finalizing bootstrapped APK")
        }
        onProgress?.onProgress("COMPLETE", outputFile.name)
        log.i { "Done. Output APK: ${outputFile.absolutePath}" }
    }

    @Throws(IOException::class)
    private fun modifyManifestFile(inputStream: InputStream, metadata: String, minSdkVersion: Int): ByteArray {
        val property = ModificationProperty()

        if (overrideVersionCode) {
            property.addManifestAttribute(AttributeItem(NodeValue.Manifest.VERSION_CODE, 1))
        }
        if (minSdkVersion < 28) {
            property.addUsesSdkAttribute(AttributeItem(NodeValue.UsesSDK.MIN_SDK_VERSION, 28))
        }
        property.addApplicationAttribute(AttributeItem(NodeValue.Application.DEBUGGABLE, debuggableFlag))
        property.addApplicationAttribute(AttributeItem("appComponentFactory", PROXY_APP_COMPONENT_FACTORY))
        property.addMetaData(ModificationProperty.MetaData("phantom", metadata))
        property.addUsesPermission("android.permission.QUERY_ALL_PACKAGES")

        return inputStream.use { input ->
            ByteArrayOutputStream().use { os ->
                ManifestEditor(input, os, property).processManifest()
                os.toByteArray()
            }
        }
    }

    companion object {
        private const val ANDROID_MANIFEST_XML = "AndroidManifest.xml"

        private val Z_FILE_OPTIONS = ZFileOptions().setAlignmentRule(
            AlignmentRules.compose(
                AlignmentRules.constantForSuffix(".so", 4096),
                AlignmentRules.constantForSuffix(ORIGINAL_APK_ASSET_PATH, 4096)
            )
        )

        @JvmStatic
        @Throws(IOException::class)
        fun main(vararg args: String) {
            val log = Logger.withTag("Phantom")
            val patcher = PhantomPatcher(arrayOf(*args))
            if (patcher.help) {
                patcher.jCommander.usage()
                return
            }
            try {
                patcher.doCommandLine()
            } catch (e: PatchError) {
                e.printStackTrace(System.err)
            }
        }
    }
}
