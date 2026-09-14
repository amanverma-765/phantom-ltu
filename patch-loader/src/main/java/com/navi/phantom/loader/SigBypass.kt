package com.navi.phantom.loader

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageParser
import android.content.pm.Signature
import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import co.touchlab.kermit.Logger
import com.navi.phantom.shared.Constants
import com.navi.phantom.shared.Constants.ORIGINAL_APK_ASSET_PATH
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Collections
import java.util.HashMap
import java.util.zip.ZipFile

object SigBypass {

    private val log = Logger.withTag("Phantom-SigBypass")

    // Synchronized: afterHookedMethod runs on arbitrary caller threads
    private val signatures: MutableMap<String, String?> =
        Collections.synchronizedMap(HashMap())

    private fun getReplacement(context: Context, packageName: String): String? {
        if (signatures.containsKey(packageName)) return signatures[packageName]
        var replacement: String? = null
        try {
            val metaData = context.packageManager
                .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                .metaData
            val encoded = metaData?.getString("phantom") ?: metaData?.getString("lspatch")
            if (encoded != null) {
                val json = String(Base64.decode(encoded, Base64.DEFAULT), StandardCharsets.UTF_8)
                try {
                    val patchConfig = JSONObject(json)
                    replacement = patchConfig.optString("originalSignature").takeIf { it.isNotEmpty() }
                } catch (e: JSONException) {
                    log.w(e) { "fail to get originalSignature" }
                }
            }
        } catch (_: PackageManager.NameNotFoundException) {
        } catch (_: RuntimeException) {
        }
        signatures[packageName] = replacement
        return replacement
    }

    private fun replaceSignature(context: Context, packageInfo: PackageInfo) {
        val hasSignature = (!packageInfo.signatures.isNullOrEmpty()) || packageInfo.signingInfo != null
        if (!hasSignature) return

        val packageName = packageInfo.packageName
        val replacement = getReplacement(context, packageName) ?: return
        val original = Signature(replacement)

        // Every signer, not only the first.
        val sigs = packageInfo.signatures
        if (sigs != null && sigs.isNotEmpty()) {
            log.d { "Replace signatures[] for `$packageName`" }
            for (i in sigs.indices) {
                sigs[i] = original
            }
        }

        val signingInfo = packageInfo.signingInfo
        if (signingInfo != null) {
            log.d { "Replace signingInfo for `$packageName`" }
            val signers = signingInfo.apkContentsSigners
            if (signers != null) {
                for (i in signers.indices) {
                    signers[i] = original
                }
            }
            replaceSigningDetails(signingInfo, original)
        }
    }

    /**
     * Overwrites the signature array inside a SigningInfo's backing SigningDetails.
     * Android 13+ uses android.content.pm.SigningDetails (field mSignatures),
     * while Android 9-12 uses PackageParser$SigningDetails (field signatures).
     */
    private fun replaceSigningDetails(signingInfo: Any, original: Signature) {
        try {
            val details = XposedHelpers.getObjectField(signingInfo, "mSigningDetails") ?: return
            for (fieldName in arrayOf("mSignatures", "signatures")) {
                try {
                    val current = XposedHelpers.getObjectField(details, fieldName)
                    if (current is Array<*>) {
                        val len = maxOf(current.size, 1)
                        @Suppress("UNCHECKED_CAST")
                        val replaced = java.lang.reflect.Array.newInstance(Signature::class.java, len) as Array<Signature>
                        for (i in 0 until len) {
                            replaced[i] = original
                        }
                        XposedHelpers.setObjectField(details, fieldName, replaced)
                        return
                    }
                } catch (_: Throwable) {
                }
            }
        } catch (t: Throwable) {
            log.d { "replaceSigningDetails skipped: ${t.message}" }
        }
    }

    private fun hookPackageParser(context: Context) {
        XposedBridge.hookAllMethods(
            PackageParser::class.java,
            "generatePackageInfo",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam<*>) {
                    val packageInfo = param.result as? PackageInfo ?: return
                    replaceSignature(context, packageInfo)
                }
            }
        )
    }

    private fun proxyPackageInfoCreator(context: Context) {
        val originalCreator = PackageInfo.CREATOR
        val proxiedCreator = object : Parcelable.Creator<PackageInfo> {
            override fun createFromParcel(source: Parcel): PackageInfo {
                val packageInfo = originalCreator.createFromParcel(source)
                replaceSignature(context, packageInfo)
                return packageInfo
            }

            override fun newArray(size: Int): Array<PackageInfo?> {
                return originalCreator.newArray(size)
            }
        }

        XposedHelpers.setStaticObjectField(PackageInfo::class.java, "CREATOR", proxiedCreator)

        runCatching {
            @Suppress("UNCHECKED_CAST")
            val mCreators = XposedHelpers.getStaticObjectField(Parcel::class.java, "mCreators") as? MutableMap<*, *>
            mCreators?.clear()
        }.onFailure { e ->
            if (e !is NoSuchFieldError) {
                log.w(e) { "fail to clear Parcel.mCreators" }
            }
        }

        runCatching {
            @Suppress("UNCHECKED_CAST")
            val sPairedCreators = XposedHelpers.getStaticObjectField(Parcel::class.java, "sPairedCreators") as? MutableMap<*, *>
            sPairedCreators?.clear()
        }.onFailure { e ->
            if (e !is NoSuchFieldError) {
                log.w(e) { "fail to clear Parcel.sPairedCreators" }
            }
        }
    }

    /**
     * Spoofs the app-local getPackageInfo path on ApplicationPackageManager.
     */
    private fun hookApplicationPackageManager(context: Context) {
        try {
            val apm = Class.forName("android.app.ApplicationPackageManager")
            XposedBridge.hookAllMethods(apm, "getPackageInfo", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam<*>) {
                    val info = param.result as? PackageInfo ?: return
                    replaceSignature(context, info)
                }
            })
        } catch (t: Throwable) {
            log.d { "hookApplicationPackageManager skipped: ${t.message}" }
        }
    }

    /**
     * Spoofs in-process archive parsing via PackageManager.getPackageArchiveInfo.
     */
    private fun hookPackageArchiveInfo(context: Context) {
        try {
            XposedBridge.hookAllMethods(PackageManager::class.java, "getPackageArchiveInfo", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam<*>) {
                    val info = param.result as? PackageInfo ?: return
                    replaceSignature(context, info)
                }
            })
        } catch (t: Throwable) {
            log.d { "hookPackageArchiveInfo skipped: ${t.message}" }
        }
    }

    /**
     * Hook PackageManager.hasSigningCertificate (API 28+).
     * Supports both (String, byte[], int) and (int, byte[], int) overloads,
     * comparing against raw X509 and SHA-256 certificate hashes.
     */
    private fun hookSigningCertificateCheck(context: Context) {
        try {
            val apm = Class.forName("android.app.ApplicationPackageManager")
            XposedBridge.hookAllMethods(apm, "hasSigningCertificate", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam<*>) {
                    if (param.result == true) return
                    val args = param.args ?: return
                    var presented: ByteArray? = null
                    var type = 0 // CERT_INPUT_RAW_X509
                    var pkg: String? = null
                    for (a in args) {
                        when (a) {
                            is ByteArray -> presented = a
                            is String -> pkg = a
                            is Int -> type = a
                        }
                    }
                    if (presented == null) return
                    val self = context.packageName
                    if (pkg != null && pkg != self) return
                    val replacement = signatures[self] ?: return
                    val original = Signature(replacement).toByteArray()
                    val expected = if (type == 1 /* CERT_INPUT_SHA256 */) sha256(original) else original
                    if (expected != null && MessageDigest.isEqual(expected, presented)) {
                        log.d { "hasSigningCertificate spoofed true for `$self`" }
                        param.result = true
                    }
                }
            })
        } catch (t: Throwable) {
            log.d { "hookSigningCertificateCheck skipped: ${t.message}" }
        }
    }

    /**
     * Hook PackageManager.checkSignatures to return SIGNATURE_MATCH
     * when one of the packages is ours.
     */
    private fun hookCheckSignatures(context: Context) {
        val packageName = context.packageName

        // checkSignatures(String, String)
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", null,
                "checkSignatures",
                String::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val pkg1 = param.args[0] as? String
                        val pkg2 = param.args[1] as? String
                        if (pkg1 == packageName || pkg2 == packageName) {
                            param.result = PackageManager.SIGNATURE_MATCH
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log.d { "Failed to hook checkSignatures(String, String): ${t.message}" }
        }

        // checkSignatures(int, int)
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", null,
                "checkSignatures",
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val uid1 = param.args[0] as Int
                        val uid2 = param.args[1] as Int
                        val myUid = android.os.Process.myUid()
                        if (uid1 == myUid || uid2 == myUid) {
                            param.result = PackageManager.SIGNATURE_MATCH
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log.d { "Failed to hook checkSignatures(int, int): ${t.message}" }
        }
    }

    private fun sha256(input: ByteArray): ByteArray? {
        return try {
            MessageDigest.getInstance("SHA-256").digest(input)
        } catch (_: Throwable) {
            null
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun doSigBypass(context: Context, sigBypassLevel: Int) {
        if (sigBypassLevel >= Constants.SIGBYPASS_LV_PM) {
            // Prime cache so hasSigningCertificate has the original on hand from the first call
            getReplacement(context, context.packageName)
            hookPackageParser(context)
            proxyPackageInfoCreator(context)
            hookApplicationPackageManager(context)
            hookPackageArchiveInfo(context)
            hookSigningCertificateCheck(context)
            hookCheckSignatures(context)
        }
        if (sigBypassLevel >= Constants.SIGBYPASS_LV_PM_OPENAT) {
            val cacheApkPath = ZipFile(context.packageResourcePath).use { sourceFile ->
                val entry = sourceFile.getEntry(ORIGINAL_APK_ASSET_PATH)
                    ?: throw IOException("Original APK asset not found in patched APK")
                "${context.cacheDir}/phantom/origin/${entry.crc}.apk"
            }
            org.lsposed.lspd.nativebridge.SigBypass.enableOpenatHook(context.packageResourcePath, cacheApkPath)
        }
        if (sigBypassLevel >= Constants.SIGBYPASS_LV_PM_OPENAT_SVC) {
            // Reuses the apk paths enableOpenatHook just recorded; must run after it
            org.lsposed.lspd.nativebridge.SigBypass.enableSvcRedirect()
        }
    }
}