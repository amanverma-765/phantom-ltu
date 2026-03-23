package com.navi.phantom.loader

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import co.touchlab.kermit.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * Clears FLAG_DEBUGGABLE from the app's own ApplicationInfo so
 * the patched app doesn't see itself as debuggable.
 */
object DebuggableBypass {

    private val log = Logger.withTag("Phantom-Debuggable")

    @JvmStatic
    fun apply(context: Context) {
        val packageName = context.packageName
        hookGetApplicationInfo(packageName)
        hookGetPackageInfo(packageName)
    }

    private fun hookGetApplicationInfo(packageName: String) {
        // getApplicationInfo(String, int)
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", null,
                "getApplicationInfo",
                String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val pkg = param.args[0] as? String ?: return
                        if (pkg == packageName) {
                            clearDebuggable(param.result as? ApplicationInfo)
                        }
                    }
                }
            )
            log.d { "Hooked getApplicationInfo(String, int)" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook getApplicationInfo(String, int)" }
        }

        // getApplicationInfo(String, ApplicationInfoFlags) - API 33+
        try {
            val flagsClass = Class.forName("android.content.pm.PackageManager\$ApplicationInfoFlags")
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", null,
                "getApplicationInfo",
                String::class.java, flagsClass,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val pkg = param.args[0] as? String ?: return
                        if (pkg == packageName) {
                            clearDebuggable(param.result as? ApplicationInfo)
                        }
                    }
                }
            )
            log.d { "Hooked getApplicationInfo(String, ApplicationInfoFlags)" }
        } catch (t: Throwable) {
            // Expected on API < 33
            log.d { "getApplicationInfo(String, ApplicationInfoFlags) not available" }
        }
    }

    private fun hookGetPackageInfo(packageName: String) {
        // getPackageInfo(String, int)
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", null,
                "getPackageInfo",
                String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val pkg = param.args[0] as? String ?: return
                        if (pkg == packageName) {
                            val pkgInfo = param.result as? PackageInfo ?: return
                            clearDebuggable(pkgInfo.applicationInfo)
                        }
                    }
                }
            )
            log.d { "Hooked getPackageInfo(String, int)" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook getPackageInfo(String, int)" }
        }

        // getPackageInfo(String, PackageInfoFlags) - API 33+
        try {
            val flagsClass = Class.forName("android.content.pm.PackageManager\$PackageInfoFlags")
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", null,
                "getPackageInfo",
                String::class.java, flagsClass,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val pkg = param.args[0] as? String ?: return
                        if (pkg == packageName) {
                            val pkgInfo = param.result as? PackageInfo ?: return
                            clearDebuggable(pkgInfo.applicationInfo)
                        }
                    }
                }
            )
            log.d { "Hooked getPackageInfo(String, PackageInfoFlags)" }
        } catch (t: Throwable) {
            log.d { "getPackageInfo(String, PackageInfoFlags) not available" }
        }
    }

    private fun clearDebuggable(appInfo: ApplicationInfo?) {
        if (appInfo == null) return
        if (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            appInfo.flags = appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        }
    }
}
