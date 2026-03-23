package com.navi.phantom.loader

import android.location.Location
import android.os.Bundle
import co.touchlab.kermit.Logger
import com.navi.phantom.shared.ConfigKeys
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * Hooks individual Location object methods to return spoofed values.
 * This ensures that even if an app gets a Location object from somewhere
 * we didn't hook, the coordinates it reads are still spoofed.
 */
object LocationHook {

    private val log = Logger.withTag("Phantom-Location")

    @JvmStatic
    fun apply() {
        hookMethod("getLatitude") { config -> config.getDouble(ConfigKeys.LATITUDE) }
        hookMethod("getLongitude") { config -> config.getDouble(ConfigKeys.LONGITUDE) }
        hookMethod("getAccuracy") { config -> config.getFloat(ConfigKeys.ACCURACY, 5f) }
        hookMethod("getSpeed") { config -> config.getFloat(ConfigKeys.SPEED, 0f) }
        hookMethod("getBearing") { config -> config.getFloat(ConfigKeys.BEARING, 0f) }

        // Hook getTime to return current time
        try {
            XposedHelpers.findAndHookMethod(
                Location::class.java, "getTime",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() != null) {
                            param.result = System.currentTimeMillis()
                        }
                    }
                }
            )
            log.d { "Hooked Location.getTime()" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook Location.getTime()" }
        }

        // Hook getElapsedRealtimeNanos
        try {
            XposedHelpers.findAndHookMethod(
                Location::class.java, "getElapsedRealtimeNanos",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() != null) {
                            param.result = android.os.SystemClock.elapsedRealtimeNanos()
                        }
                    }
                }
            )
            log.d { "Hooked Location.getElapsedRealtimeNanos()" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook Location.getElapsedRealtimeNanos()" }
        }

        // Hook getExtras to return empty bundle (strips mock flag)
        try {
            XposedHelpers.findAndHookMethod(
                Location::class.java, "getExtras",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() != null) {
                            param.result = Bundle()
                        }
                    }
                }
            )
            log.d { "Hooked Location.getExtras()" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook Location.getExtras()" }
        }
    }

    private fun hookMethod(methodName: String, valueProvider: (Bundle) -> Any) {
        try {
            XposedHelpers.findAndHookMethod(
                Location::class.java, methodName,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val config = LocationHelper.getConfig() ?: return
                        param.result = valueProvider(config)
                    }
                }
            )
            log.d { "Hooked Location.$methodName()" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook Location.$methodName()" }
        }
    }
}
