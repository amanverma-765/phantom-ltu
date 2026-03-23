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
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import org.json.JSONObject
import com.navi.phantom.shared.Constants
import com.navi.phantom.shared.Constants.ORIGINAL_APK_ASSET_PATH
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

object SigBypass {

    private val log = Logger.withTag("Phantom-SigBypass")

    private val signatures = java.util.concurrent.ConcurrentHashMap<String, String>()
    private const val NO_SIGNATURE = ""

    private fun replaceSignature(context: Context, packageInfo: PackageInfo) {
        val signingInfo = packageInfo.signingInfo ?: return

        val packageName = packageInfo.packageName ?: return
        var replacement: String? = signatures[packageName]?.takeIf { it.isNotEmpty() }

        if (replacement == null && !signatures.containsKey(packageName)) {
            try {
                val metaData = context.packageManager
                    .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                    .metaData
                val encoded = metaData?.getString("phantom")
                if (encoded != null) {
                    val json = String(Base64.decode(encoded, Base64.DEFAULT), StandardCharsets.UTF_8)
                    try {
                        val patchConfig = JSONObject(json)
                        replacement = patchConfig.getString("originalSignature")
                    } catch (e: Exception) {
                        log.w(e) { "fail to get originalSignature" }
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                log.d { "Package not found: $packageName" }
            }
            signatures[packageName] = replacement ?: NO_SIGNATURE
        }

        if (replacement != null) {
            val signaturesArray = signingInfo.apkContentsSigners
            if (signaturesArray != null && signaturesArray.isNotEmpty()) {
                log.d { "Replace signature info for `$packageName`" }
                signaturesArray[0] = Signature(replacement)
            }
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

    @JvmStatic
    @Throws(IOException::class)
    fun doSigBypass(context: Context, sigBypassLevel: Int) {
        if (sigBypassLevel >= Constants.SIGBYPASS_LV_PM) {
            hookPackageParser(context)
            proxyPackageInfoCreator(context)
            hookHasSigningCertificate(context)
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
    }

    /**
     * Hook PackageManager.hasSigningCertificate(String, byte[], int) (API 28+).
     * Compares against the original signature so the app's own cert check passes.
     */
    private fun hookHasSigningCertificate(context: Context) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", null,
                "hasSigningCertificate",
                String::class.java, ByteArray::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        val queriedPackage = param.args[0] as? String ?: return
                        val certBytes = param.args[1] as? ByteArray ?: return
                        val originalSig = getOriginalSignature(context, queriedPackage) ?: return

                        // Convert stored hex signature to byte array for comparison
                        val originalBytes = Signature(originalSig).toByteArray()
                        param.result = originalBytes.contentEquals(certBytes)
                    }
                }
            )
            log.d { "Hooked hasSigningCertificate" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook hasSigningCertificate" }
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
            log.d { "Hooked checkSignatures(String, String)" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook checkSignatures(String, String)" }
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
            log.d { "Hooked checkSignatures(int, int)" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook checkSignatures(int, int)" }
        }
    }

    /**
     * Retrieves the original signature hex string for the given package.
     * Uses the cached signatures map.
     */
    private fun getOriginalSignature(context: Context, packageName: String): String? {
        signatures[packageName]?.takeIf { it.isNotEmpty() }?.let { return it }
        if (signatures.containsKey(packageName)) return null

        return try {
            val metaData = context.packageManager
                .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                .metaData
            val encoded = metaData?.getString("phantom")
            if (encoded != null) {
                val json = String(Base64.decode(encoded, Base64.DEFAULT), StandardCharsets.UTF_8)
                val patchConfig = JSONObject(json)
                val sig: String? = patchConfig.optString("originalSignature", null as String?)
                signatures[packageName] = sig ?: NO_SIGNATURE
                sig
            } else {
                signatures[packageName] = NO_SIGNATURE
                null
            }
        } catch (e: Exception) {
            signatures[packageName] = NO_SIGNATURE
            null
        }
    }
}