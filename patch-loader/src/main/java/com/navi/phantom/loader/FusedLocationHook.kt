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
     * Hook Task listeners and getResult() to intercept location delivery.
     */
    private fun hookTaskResult(task: Any?) {
        if (task == null) return

        // Hook addOnSuccessListener
        try {
            XposedBridge.hookAllMethods(
                task.javaClass, "addOnSuccessListener",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        val config = LocationHelper.getConfig() ?: return
                        val originalListener = param.args.firstOrNull() ?: return
                        try {
                            XposedBridge.hookAllMethods(
                                originalListener.javaClass, "onSuccess",
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
                        } catch (_: Throwable) { }
                    }
                }
            )
        } catch (_: Throwable) { }

        // Hook addOnCompleteListener — callback receives Task, so hook getResult()
        try {
            XposedBridge.hookAllMethods(
                task.javaClass, "addOnCompleteListener",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() == null) return
                        // Hook getResult on the task class to return fake location
                        hookTaskGetResult(param.thisObject)
                    }
                }
            )
        } catch (_: Throwable) { }

        // Also hook getResult directly for any code that calls task.getResult()
        hookTaskGetResult(task)
    }

    private val hookedTaskClasses = java.util.Collections.synchronizedSet(mutableSetOf<Class<*>>())

    private fun hookTaskGetResult(task: Any) {
        val taskClass = task.javaClass
        if (!hookedTaskClasses.add(taskClass)) return
        try {
            XposedBridge.hookAllMethods(
                taskClass, "getResult",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (param.result is Location) {
                            val fake = LocationHelper.createFakeLocation("fused") ?: return
                            param.result = fake
                        }
                    }
                }
            )
        } catch (_: Throwable) { }
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
                        } catch (t: Throwable) {
                            log.w(t) { "LocationResult.create() failed — real location may leak" }
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
