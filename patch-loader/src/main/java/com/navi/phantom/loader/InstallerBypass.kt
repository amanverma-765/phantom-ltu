package com.navi.phantom.loader

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.InstallSourceInfo
import android.os.Build
import co.touchlab.kermit.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * Spoofs the installer source to make the app appear as if installed from Google Play Store.
 * Hooks PackageManager methods that return installer information.
 */
object InstallerBypass {

    private val log = Logger.withTag("Phantom-Installer")
    private const val PLAY_STORE = "com.android.vending"

    @JvmStatic
    fun apply(context: Context) {
        val packageName = context.packageName
        hookGetInstallerPackageName(packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hookGetInstallSourceInfo(packageName)
        }
    }

    @Suppress("DEPRECATION")
    private fun hookGetInstallerPackageName(packageName: String) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", null,
                "getInstallerPackageName", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val queriedPackage = param.args[0] as? String
                        if (queriedPackage == packageName) {
                            param.result = PLAY_STORE
                        }
                    }
                }
            )
            log.d { "Hooked getInstallerPackageName" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook getInstallerPackageName" }
        }
    }

    private fun hookGetInstallSourceInfo(packageName: String) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", null,
                "getInstallSourceInfo", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val queriedPackage = param.args[0] as? String
                        if (queriedPackage == packageName) {
                            param.result = createInstallSourceInfo()
                        }
                    }
                }
            )
            log.d { "Hooked getInstallSourceInfo" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook getInstallSourceInfo" }
        }
    }

    @SuppressLint("NewApi")
    private fun createInstallSourceInfo(): InstallSourceInfo {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+: InstallSourceInfo(String, SigningInfo, String, String, String, SigningInfo, int)
                // Try the newest constructor first
                val constructor = InstallSourceInfo::class.java.getDeclaredConstructor(
                    String::class.java,                         // initiatingPackageName
                    android.content.pm.SigningInfo::class.java, // initiatingPackageSigningInfo
                    String::class.java,                         // originatingPackageName
                    String::class.java,                         // installingPackageName
                    String::class.java,                         // updateOwnerPackageName
                    android.content.pm.SigningInfo::class.java, // updateOwnerPackageSigningInfo
                    Int::class.javaPrimitiveType                // packageSource
                )
                constructor.isAccessible = true
                constructor.newInstance(PLAY_STORE, null, PLAY_STORE, PLAY_STORE, PLAY_STORE, null, 0)
            } else {
                // API 30-32: InstallSourceInfo(String, SigningInfo, String, String)
                val constructor = InstallSourceInfo::class.java.getDeclaredConstructor(
                    String::class.java,                         // initiatingPackageName
                    android.content.pm.SigningInfo::class.java, // initiatingPackageSigningInfo
                    String::class.java,                         // originatingPackageName
                    String::class.java                          // installingPackageName
                )
                constructor.isAccessible = true
                constructor.newInstance(PLAY_STORE, null, PLAY_STORE, PLAY_STORE)
            }
        } catch (e: Exception) {
            log.w(e) { "Failed to create InstallSourceInfo via constructor, trying fallback" }
            // Fallback: create via any available constructor and set fields
            try {
                val constructors = InstallSourceInfo::class.java.declaredConstructors
                val constructor = constructors.first()
                constructor.isAccessible = true
                val params = constructor.parameterTypes.map<Class<*>, Any?> { type ->
                    when (type) {
                        String::class.java -> PLAY_STORE
                        Int::class.javaPrimitiveType -> 0
                        else -> null
                    }
                }.toTypedArray()
                constructor.newInstance(*params) as InstallSourceInfo
            } catch (e2: Exception) {
                log.e(e2) { "All InstallSourceInfo construction attempts failed" }
                throw e2
            }
        }
    }
}
