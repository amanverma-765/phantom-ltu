package com.navi.phantom.loader

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import co.touchlab.kermit.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * Hooks LocationManager methods to return spoofed locations.
 * This is the primary Android location API — all standard location requests go through here.
 */
object LocationManagerHook {

    private val log = Logger.withTag("Phantom-LocationManager")

    @JvmStatic
    fun apply() {
        hookGetLastKnownLocation()
        hookGetLastLocation()
        hookRequestLocationUpdates()
        hookRequestSingleUpdate()
        hookProviderMethods()
    }

    private fun hookGetLastKnownLocation() {
        try {
            XposedHelpers.findAndHookMethod(
                LocationManager::class.java, "getLastKnownLocation",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val fake = LocationHelper.createFakeLocation(
                            param.args[0] as? String ?: "gps"
                        ) ?: return
                        param.result = fake
                    }
                }
            )
            log.d { "Hooked getLastKnownLocation" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook getLastKnownLocation" }
        }
    }

    private fun hookGetLastLocation() {
        try {
            XposedHelpers.findAndHookMethod(
                LocationManager::class.java, "getLastLocation",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val fake = LocationHelper.createFakeLocation() ?: return
                        param.result = fake
                    }
                }
            )
            log.d { "Hooked getLastLocation" }
        } catch (t: Throwable) {
            // getLastLocation may not exist on older APIs
            log.d { "getLastLocation not available" }
        }
    }

    private fun hookRequestLocationUpdates() {
        // Hook all overloads of requestLocationUpdates
        try {
            XposedBridge.hookAllMethods(
                LocationManager::class.java, "requestLocationUpdates",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        // Find the LocationListener in the args and hook it
                        // so future updates get spoofed locations
                        for (arg in param.args) {
                            if (arg is LocationListener) {
                                hookLocationListener(arg)
                                break
                            }
                        }
                    }
                }
            )
            log.d { "Hooked requestLocationUpdates" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook requestLocationUpdates" }
        }
    }

    private fun hookRequestSingleUpdate() {
        try {
            XposedBridge.hookAllMethods(
                LocationManager::class.java, "requestSingleUpdate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        for (arg in param.args) {
                            if (arg is LocationListener) {
                                deliverFakeLocation(arg)
                                break
                            }
                        }
                    }
                }
            )
            log.d { "Hooked requestSingleUpdate" }
        } catch (t: Throwable) {
            log.d { "requestSingleUpdate not available" }
        }
    }

    private fun hookProviderMethods() {
        // isProviderEnabled — return true for gps and network
        try {
            XposedHelpers.findAndHookMethod(
                LocationManager::class.java, "isProviderEnabled",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() == null) return
                        val provider = param.args[0] as? String
                        if (provider == LocationManager.GPS_PROVIDER || provider == LocationManager.NETWORK_PROVIDER) {
                            param.result = true
                        }
                    }
                }
            )
            log.d { "Hooked isProviderEnabled" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook isProviderEnabled" }
        }

        // getBestProvider — return gps
        try {
            XposedBridge.hookAllMethods(
                LocationManager::class.java, "getBestProvider",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() != null) {
                            param.result = LocationManager.GPS_PROVIDER
                        }
                    }
                }
            )
            log.d { "Hooked getBestProvider" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook getBestProvider" }
        }
    }

    /**
     * Hook a LocationListener's onLocationChanged to swap in fake location.
     */
    private val hookedListeners = java.util.Collections.synchronizedSet(
        java.util.Collections.newSetFromMap(
            java.util.WeakHashMap<LocationListener, Boolean>()
        )
    )

    private fun hookLocationListener(listener: LocationListener) {
        if (!hookedListeners.add(listener)) return // already hooked

        try {
            XposedBridge.hookAllMethods(
                listener.javaClass, "onLocationChanged",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        val fake = LocationHelper.createFakeLocation() ?: return
                        if (param.args.isNotEmpty() && param.args[0] is Location) {
                            param.args[0] = fake
                        }
                    }
                }
            )
        } catch (_: Throwable) {
            // Some listeners may not be hookable
        }
    }

    private fun deliverFakeLocation(listener: LocationListener) {
        val fake = LocationHelper.createFakeLocation() ?: return
        try {
            listener.onLocationChanged(fake)
        } catch (_: Throwable) {
            // Listener may not be ready yet
        }
    }
}
