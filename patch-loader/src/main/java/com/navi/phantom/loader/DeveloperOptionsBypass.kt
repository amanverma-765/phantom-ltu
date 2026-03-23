package com.navi.phantom.loader

import android.content.ContentResolver
import android.provider.Settings
import co.touchlab.kermit.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * Hides developer options and USB debugging status from the patched app.
 * Hooks Settings.Secure and Settings.Global to return disabled values
 * for development_settings_enabled and adb_enabled.
 */
object DeveloperOptionsBypass {

    private val log = Logger.withTag("Phantom-DevOptions")

    private val SPOOFED_SETTINGS = mapOf(
        "development_settings_enabled" to 0,
        "adb_enabled" to 0,
    )

    @JvmStatic
    fun apply() {
        hookSettingsClass(Settings.Secure::class.java, "Settings.Secure")
        hookSettingsClass(Settings.Global::class.java, "Settings.Global")
    }

    private fun hookSettingsClass(clazz: Class<*>, name: String) {
        // getInt(ContentResolver, String)
        try {
            XposedHelpers.findAndHookMethod(
                clazz, "getInt",
                ContentResolver::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val key = param.args[1] as? String ?: return
                        SPOOFED_SETTINGS[key]?.let { param.result = it }
                    }
                }
            )
            log.d { "Hooked $name.getInt(CR, String)" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook $name.getInt(CR, String)" }
        }

        // getInt(ContentResolver, String, int)
        try {
            XposedHelpers.findAndHookMethod(
                clazz, "getInt",
                ContentResolver::class.java, String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val key = param.args[1] as? String ?: return
                        SPOOFED_SETTINGS[key]?.let { param.result = it }
                    }
                }
            )
            log.d { "Hooked $name.getInt(CR, String, int)" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook $name.getInt(CR, String, int)" }
        }

        // getString(ContentResolver, String)
        try {
            XposedHelpers.findAndHookMethod(
                clazz, "getString",
                ContentResolver::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val key = param.args[1] as? String ?: return
                        if (SPOOFED_SETTINGS.containsKey(key)) {
                            param.result = "0"
                        }
                    }
                }
            )
            log.d { "Hooked $name.getString(CR, String)" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook $name.getString(CR, String)" }
        }
    }
}
