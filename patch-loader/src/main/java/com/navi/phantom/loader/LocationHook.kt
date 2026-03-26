package com.navi.phantom.loader

import android.location.Location
import android.os.Build
import android.os.Bundle
import co.touchlab.kermit.Logger
import com.navi.phantom.shared.ConfigKeys
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ThreadLocalRandom

/**
 * Hooks individual Location object methods to return spoofed values.
 * This ensures that even if an app gets a Location object from somewhere
 * we didn't hook, the coordinates it reads are still spoofed.
 *
 * Skips Location objects already created by [LocationHelper.createFakeLocation]
 * (tracked via [LocationHelper.isPhantom]) to preserve their jitter values
 * and avoid unnecessary IPC.
 */
object LocationHook {

    private val log = Logger.withTag("Phantom-Location")
    private val random: ThreadLocalRandom get() = ThreadLocalRandom.current()

    /** Cached extras bundles per Location object to avoid returning different values on repeated reads. */
    private val cachedExtras: MutableMap<Location, Bundle> = Collections.synchronizedMap(WeakHashMap())

    @JvmStatic
    fun apply() {
        // Core coordinate getters — add micro-jitter to match LocationHelper.createFakeLocation()
        hookMethod("getLatitude") { config ->
            config.getDouble(ConfigKeys.LATITUDE) + random.nextGaussian() * 0.000008
        }
        hookMethod("getLongitude") { config ->
            config.getDouble(ConfigKeys.LONGITUDE) + random.nextGaussian() * 0.000008
        }
        hookMethod("getAccuracy") { config ->
            (config.getFloat(ConfigKeys.ACCURACY, 5f) + random.nextFloat() * 4f - 2f).coerceAtLeast(1f)
        }
        hookMethod("getSpeed") { config ->
            (config.getFloat(ConfigKeys.SPEED, 0f) + random.nextFloat() * 0.3f).coerceAtLeast(0f)
        }
        hookMethod("getBearing") { config ->
            val baseSpeed = config.getFloat(ConfigKeys.SPEED, 0f)
            val baseBearing = config.getFloat(ConfigKeys.BEARING, 0f)
            val jitteredSpeed = baseSpeed + random.nextFloat() * 0.3f
            if (jitteredSpeed > 0.5f) {
                val b = if (baseBearing == 0f) random.nextFloat() * 360f else baseBearing
                ((b + random.nextFloat() * 10f - 5f) % 360f + 360f) % 360f
            } else {
                random.nextFloat() * 360f
            }
        }
        hookMethod("getAltitude") { _ -> 100.0 + random.nextGaussian() * 3.0 }

        // Provider — return "gps" to match what createFakeLocation sets
        hookMethod("getProvider") { _ -> "gps" }

        // has* checks — return true so apps see complete location data
        hookBoolean("hasAccuracy")
        hookBoolean("hasAltitude")
        hookBoolean("hasSpeed")
        hookBoolean("hasBearing")

        // API 26+ accuracy fields
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            hookMethod("getVerticalAccuracyMeters") { _ -> 10f + random.nextFloat() * 20f }
            hookMethod("getSpeedAccuracyMetersPerSecond") { _ -> 0.3f + random.nextFloat() * 0.4f }
            hookMethod("getBearingAccuracyDegrees") { _ -> 5f + random.nextFloat() * 10f }
            hookBoolean("hasVerticalAccuracy")
            hookBoolean("hasSpeedAccuracy")
            hookBoolean("hasBearingAccuracy")
        }

        // Hook getTime to return current time
        hookSimple("getTime") { System.currentTimeMillis() }

        // Hook getElapsedRealtimeNanos
        hookSimple("getElapsedRealtimeNanos") { android.os.SystemClock.elapsedRealtimeNanos() }

        // Hook getExtras to return satellite bundle (strips mock flag)
        // Cached per Location instance so repeated reads return consistent values
        try {
            XposedHelpers.findAndHookMethod(
                Location::class.java, "getExtras",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (shouldSkip(param)) return
                        val location = param.thisObject as Location
                        param.result = cachedExtras.getOrPut(location) {
                            Bundle().apply {
                                putInt("satellites", 8 + random.nextInt(5))
                                putInt("maxCn0", 30 + random.nextInt(15))
                                putInt("meanCn0", 20 + random.nextInt(10))
                            }
                        }
                    }
                }
            )
            log.d { "Hooked Location.getExtras()" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook Location.getExtras()" }
        }
    }

    /** Check if we should skip this Location (already fake, or spoofing inactive). */
    private fun shouldSkip(param: XC_MethodHook.MethodHookParam<*>): Boolean {
        if (!LocationHelper.isActive) return true
        val location = param.thisObject as? Location ?: return true
        return LocationHelper.isPhantom(location)
    }

    /** Hook a getter that reads from the config bundle. */
    private fun hookMethod(methodName: String, valueProvider: (Bundle) -> Any) {
        try {
            XposedHelpers.findAndHookMethod(
                Location::class.java, methodName,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (shouldSkip(param)) return
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

    /** Hook a has* method to return true when spoofing is active. */
    private fun hookBoolean(methodName: String) {
        try {
            XposedHelpers.findAndHookMethod(
                Location::class.java, methodName,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (shouldSkip(param)) return
                        param.result = true
                    }
                }
            )
            log.d { "Hooked Location.$methodName()" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook Location.$methodName()" }
        }
    }

    /** Hook a method with a simple value provider (no config needed, just check active). */
    private fun hookSimple(methodName: String, valueProvider: () -> Any) {
        try {
            XposedHelpers.findAndHookMethod(
                Location::class.java, methodName,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (shouldSkip(param)) return
                        param.result = valueProvider()
                    }
                }
            )
            log.d { "Hooked Location.$methodName()" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook Location.$methodName()" }
        }
    }
}
