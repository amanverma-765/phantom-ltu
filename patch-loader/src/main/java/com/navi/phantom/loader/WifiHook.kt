package com.navi.phantom.loader

import android.net.wifi.WifiManager
import co.touchlab.kermit.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * Hooks WiFi scan results to prevent WiFi-based location detection.
 * Returns empty scan results so apps can't triangulate position via WiFi APs.
 * Does NOT hook getConnectionInfo — apps use that for connectivity checks
 * and returning null crashes many apps / prevents map tile loading.
 */
object WifiHook {

    private val log = Logger.withTag("Phantom-Wifi")

    @JvmStatic
    fun apply() {
        hookGetScanResults()
    }

    /**
     * Return empty scan results — prevents WiFi-based location triangulation.
     * This only blocks location inference from WiFi APs, not WiFi connectivity itself.
     */
    private fun hookGetScanResults() {
        try {
            XposedHelpers.findAndHookMethod(
                WifiManager::class.java, "getScanResults",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() == null) return
                        param.result = emptyList<Any>()
                    }
                }
            )
            log.d { "Hooked WifiManager.getScanResults" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook getScanResults" }
        }
    }
}
