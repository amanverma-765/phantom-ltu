package com.navi.phantom.metaloader

import android.annotation.SuppressLint
import android.app.ActivityThread
import android.app.AppComponentFactory
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.os.Build
import android.os.Process
import android.os.ServiceManager
import co.touchlab.kermit.Logger
import com.navi.phantom.shared.Constants
import org.json.JSONObject
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

@SuppressLint("UnsafeDynamicallyLoadedCode", "DiscouragedPrivateApi")
class LSPAppComponentFactoryStub : AppComponentFactory() {

    companion object {
        private val log = Logger.withTag("Phantom-MetaLoader")

        private val archToLib = mapOf(
            "arm" to "armeabi-v7a",
            "arm64" to "arm64-v8a",
            "x86" to "x86",
            "x86_64" to "x86_64"
        )

        @JvmField
        var dex: ByteArray? = null

        init {
            val appZygote = ActivityThread.currentActivityThread() == null
            if (appZygote) {
                log.i { "Skip loading libphantom.so for appZygote" }
            } else {
                bootstrap()
            }
        }

        @Volatile
        private var loaded = false

        private fun bootstrap() {
            if (loaded) return
            try {
                bootstrapInternal()
                loaded = true
            } catch (e: UnsatisfiedLinkError) {
                if (e.message?.contains("already opened by ClassLoader") == true) {
                    log.i { "Native library libphantom.so already opened by ClassLoader, continuing: ${e.message}" }
                    loaded = true
                } else {
                    throw ExceptionInInitializerError(e)
                }
            } catch (e: Throwable) {
                throw ExceptionInInitializerError(e)
            }
        }

        private fun bootstrapInternal() {
            val vmRuntime = Class.forName("dalvik.system.VMRuntime")
            val getRuntime = vmRuntime.getDeclaredMethod("getRuntime").apply { isAccessible = true }
            val vmInstructionSet = vmRuntime.getDeclaredMethod("vmInstructionSet").apply { isAccessible = true }
            val arch = vmInstructionSet.invoke(getRuntime.invoke(null)) as String
            val libName = archToLib[arch]

            log.i { "Bootstrap loader from manager" }
            val managerApkPath = findManagerApk()
            log.d { "Manager APK: $managerApkPath" }

            ZipFile(File(managerApkPath)).use { zip ->
                val entry = requireNotNull(zip.getEntry(Constants.LOADER_DEX_ASSET_PATH)) {
                    "Loader DEX not found in manager APK ($managerApkPath)"
                }
                zip.getInputStream(entry).use { input ->
                    ByteArrayOutputStream().use { output ->
                        input.copyTo(output)
                        dex = output.toByteArray()
                    }
                }
            }
            val soPath = "$managerApkPath!/assets/phantom/so/$libName/libphantom.so"
            try {
                System.load(soPath)
            } catch (e: UnsatisfiedLinkError) {
                if (e.message?.contains("already opened by ClassLoader") == true) {
                    log.i { "libphantom.so already resident in process namespace: ${e.message}" }
                } else {
                    throw e
                }
            }
        }

        /**
         * Find the manager APK path using a 3-tier fallback:
         * 1. Embedded path from patch config (fastest, works on all OEMs)
         * 2. Filesystem scan of /data/app/ (bypasses AppsFilter)
         * 3. IPackageManager query
         */
        private fun findManagerApk(): String {
            val (embeddedPath, recordedPkg) = readConfigDetails()

            // 1. Try embedded path from config
            if (embeddedPath != null && File(embeddedPath).exists() && carriesLoader(embeddedPath)) {
                log.d { "Found manager APK via config" }
                return embeddedPath
            }

            val candidates = if (recordedPkg != null && recordedPkg != Constants.MANAGER_PACKAGE_NAME) {
                listOf(recordedPkg, Constants.MANAGER_PACKAGE_NAME)
            } else {
                listOf(Constants.MANAGER_PACKAGE_NAME)
            }

            // 2. Try filesystem scan
            for (pkg in candidates) {
                findManagerApkFromFilesystem(pkg)?.let { path ->
                    if (carriesLoader(path)) {
                        log.d { "Found manager APK via filesystem: $path" }
                        return path
                    }
                }
            }

            // 3. Fallback to IPackageManager
            for (pkg in candidates) {
                getManagerApkFromPackageManager(pkg)?.let { path ->
                    if (carriesLoader(path)) {
                        log.d { "Found manager APK via IPackageManager: $path" }
                        return path
                    }
                }
            }

            throw IllegalStateException(
                "No installed Phantom manager carries the loader (tried ${candidates.joinToString()}); re-patch this app or reinstall the manager"
            )
        }

        private fun carriesLoader(sourceDir: String): Boolean {
            return try {
                ZipFile(File(sourceDir)).use { zip ->
                    zip.getEntry(Constants.LOADER_DEX_ASSET_PATH) != null
                }
            } catch (_: Throwable) {
                false
            }
        }

        /**
         * Read managerApkPath and managerPackageName from embedded config.json.
         */
        private fun readConfigDetails(): Pair<String?, String?> {
            return try {
                val cl = LSPAppComponentFactoryStub::class.java.classLoader ?: return null to null
                cl.getResourceAsStream(Constants.CONFIG_ASSET_PATH)?.use { stream ->
                    val json = JSONObject(stream.bufferedReader(StandardCharsets.UTF_8).readText())
                    val path = json.optString("managerApkPath").takeIf { it.isNotEmpty() }
                    val pkg = json.optString("managerPackageName").takeIf { it.isNotEmpty() }
                    path to pkg
                } ?: (null to null)
            } catch (_: Exception) {
                null to null
            }
        }

        /**
         * Scan /data/app/ for the manager package directory.
         */
        private fun findManagerApkFromFilesystem(packageName: String): String? {
            return try {
                val dataApp = File("/data/app")
                if (!dataApp.exists()) return null

                dataApp.listFiles()?.forEach { randomDir ->
                    randomDir.listFiles()?.forEach { pkgDir ->
                        if (pkgDir.name.startsWith(packageName)) {
                            val baseApk = File(pkgDir, "base.apk")
                            if (baseApk.exists()) return baseApk.absolutePath
                        }
                    }
                }
                null
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Query IPackageManager for the manager's ApplicationInfo.
         */
        private fun getManagerApkFromPackageManager(packageName: String): String? {
            return try {
                val ipm = IPackageManager.Stub.asInterface(ServiceManager.getService("package")) ?: return null
                val manager: ApplicationInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    HiddenApiBypass.invoke(
                        IPackageManager::class.java, ipm, "getApplicationInfo",
                        packageName, 0L, Process.myUid() / 100000
                    ) as? ApplicationInfo
                } else {
                    ipm.getApplicationInfo(packageName, 0, Process.myUid() / 100000)
                }
                manager?.sourceDir
            } catch (_: Throwable) {
                null
            }
        }
    }
}
