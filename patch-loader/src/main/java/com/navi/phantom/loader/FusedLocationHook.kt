package com.navi.phantom.loader

import android.location.Location
import co.touchlab.kermit.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * Hooks Google Play Services FusedLocationProviderClient.
 * ~80% of modern apps use this instead of LocationManager.
 * All classes accessed via reflection since GMS isn't a compile dependency.
 * Silently skips if GMS isn't present in the app.
 */
object FusedLocationHook {

    private val log = Logger.withTag("Phantom-FusedLocation")

    private const val FUSED_CLIENT = "com.google.android.gms.location.FusedLocationProviderClient"
    private const val LOCATION_RESULT = "com.google.android.gms.location.LocationResult"
    private const val LOCATION_CALLBACK = "com.google.android.gms.location.LocationCallback"
    private const val LOCATION_AVAILABILITY = "com.google.android.gms.location.LocationAvailability"

    @JvmStatic
    fun apply(classLoader: ClassLoader) {
        if (!hasGms(classLoader)) {
            log.d { "GMS location classes not found, skipping" }
            return
        }
        log.d { "GMS location classes found, applying hooks" }

        hookFusedClient(classLoader)
        hookLocationResult(classLoader)
        hookLocationCallback(classLoader)
    }

    private fun hasGms(cl: ClassLoader): Boolean {
        return XposedHelpers.findClassIfExists(FUSED_CLIENT, cl) != null
    }

    /**
     * Hook FusedLocationProviderClient.getLastLocation() and getCurrentLocation().
     * These return Task<Location> — we hook afterHookedMethod to intercept
     * the Task's success callbacks.
     */
    private fun hookFusedClient(cl: ClassLoader) {
        val fusedClass = XposedHelpers.findClassIfExists(FUSED_CLIENT, cl) ?: return

        // Hook getLastLocation — intercept the returned Task
        try {
            XposedBridge.hookAllMethods(
                fusedClass, "getLastLocation",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        hookTaskResult(param.result)
                    }
                }
            )
            log.d { "Hooked FusedLocationProviderClient.getLastLocation" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook getLastLocation on FusedClient" }
        }

        // Hook getCurrentLocation
        try {
            XposedBridge.hookAllMethods(
                fusedClass, "getCurrentLocation",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        hookTaskResult(param.result)
                    }
                }
            )
            log.d { "Hooked FusedLocationProviderClient.getCurrentLocation" }
        } catch (t: Throwable) {
            log.d { "getCurrentLocation not available on FusedClient" }
        }

        // Hook requestLocationUpdates — the callback will be handled by hookLocationCallback
        try {
            XposedBridge.hookAllMethods(
                fusedClass, "requestLocationUpdates",
                object : XC_MethodHook() {
                    // Just let it pass — LocationCallback.onLocationResult is hooked separately
                }
            )
            log.d { "Hooked FusedLocationProviderClient.requestLocationUpdates" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook requestLocationUpdates on FusedClient" }
        }
    }

    /**
     * Hook Task.addOnSuccessListener to intercept the location delivery.
     */
    private fun hookTaskResult(task: Any?) {
        if (task == null) return
        try {
            XposedBridge.hookAllMethods(
                task.javaClass, "addOnSuccessListener",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        val config = LocationHelper.getConfig() ?: return
                        // Wrap the listener to deliver fake location
                        val originalListener = param.args.firstOrNull() ?: return
                        val listenerClass = originalListener.javaClass

                        try {
                            XposedBridge.hookAllMethods(
                                listenerClass, "onSuccess",
                                object : XC_MethodHook() {
                                    override fun beforeHookedMethod(innerParam: MethodHookParam<*>) {
                                        if (innerParam.thisObject !== originalListener) return
                                        val fake = LocationHelper.createFakeLocation("fused", config)
                                        if (innerParam.args.isNotEmpty()) {
                                            innerParam.args[0] = fake
                                        }
                                    }
                                }
                            )
                        } catch (_: Throwable) {
                            // Listener may use lambda or be unhookable
                        }
                    }
                }
            )
        } catch (_: Throwable) {
            // Task class may vary
        }
    }

    /**
     * Hook LocationResult.getLastLocation() and getLocations() to return fake data.
     * This is the most reliable way to spoof fused location —
     * all code reading from LocationResult gets spoofed values.
     */
    private fun hookLocationResult(cl: ClassLoader) {
        val resultClass = XposedHelpers.findClassIfExists(LOCATION_RESULT, cl) ?: return

        try {
            XposedHelpers.findAndHookMethod(
                resultClass, "getLastLocation",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val fake = LocationHelper.createFakeLocation("fused") ?: return
                        param.result = fake
                    }
                }
            )
            log.d { "Hooked LocationResult.getLastLocation" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LocationResult.getLastLocation" }
        }

        try {
            XposedHelpers.findAndHookMethod(
                resultClass, "getLocations",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val fake = LocationHelper.createFakeLocation("fused") ?: return
                        param.result = listOf(fake)
                    }
                }
            )
            log.d { "Hooked LocationResult.getLocations" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LocationResult.getLocations" }
        }
    }

    /**
     * Hook LocationCallback.onLocationResult to replace the LocationResult.
     * Also hook onLocationAvailability to always report available.
     */
    private fun hookLocationCallback(cl: ClassLoader) {
        val callbackClass = XposedHelpers.findClassIfExists(LOCATION_CALLBACK, cl) ?: return
        val availabilityClass = XposedHelpers.findClassIfExists(LOCATION_AVAILABILITY, cl)

        try {
            XposedBridge.hookAllMethods(
                callbackClass, "onLocationResult",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        val config = LocationHelper.getConfig() ?: return
                        val resultClass = XposedHelpers.findClassIfExists(LOCATION_RESULT, cl) ?: return

                        // Create a new LocationResult with fake location
                        try {
                            val fake = LocationHelper.createFakeLocation("fused", config)
                            val fakeResult = XposedHelpers.callStaticMethod(
                                resultClass, "create", listOf(fake)
                            )
                            if (param.args.isNotEmpty()) {
                                param.args[0] = fakeResult
                            }
                        } catch (_: Throwable) {
                            // LocationResult.create() may not exist on all GMS versions
                        }
                    }
                }
            )
            log.d { "Hooked LocationCallback.onLocationResult" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook LocationCallback.onLocationResult" }
        }

        // Hook onLocationAvailability to always report available
        if (availabilityClass != null) {
            try {
                XposedBridge.hookAllMethods(
                    callbackClass, "onLocationAvailability",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam<*>) {
                            if (LocationHelper.getConfig() == null) return
                            try {
                                val fakeAvailability = XposedHelpers.callStaticMethod(
                                    availabilityClass, "create", true
                                )
                                if (param.args.isNotEmpty()) {
                                    param.args[0] = fakeAvailability
                                }
                            } catch (_: Throwable) {
                                // Fallback — just let original through
                            }
                        }
                    }
                )
                log.d { "Hooked LocationCallback.onLocationAvailability" }
            } catch (t: Throwable) {
                log.d { "Failed to hook onLocationAvailability" }
            }
        }
    }
}
