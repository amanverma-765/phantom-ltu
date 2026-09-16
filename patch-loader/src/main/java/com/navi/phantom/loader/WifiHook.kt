package com.navi.phantom.loader

import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import co.touchlab.kermit.Logger
import com.navi.phantom.shared.ConfigKeys
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.util.Locale
import java.util.Random

/**
 * Hooks WiFi APIs to prevent WiFi-based location triangulation and leakage:
 * 1. Returns empty scan results for [WifiManager.getScanResults] so apps cannot triangulate via nearby APs.
 * 2. Randomizes/masks connected router BSSID via [WifiInfo.getBSSID] and [WifiManager.getConnectionInfo]
 *    so apps cannot determine physical location via WiGLE or Google Geolocation databases.
 * Does NOT return null for getConnectionInfo, preserving normal network connectivity, link speed, RSSI, and IP checks.
 */
object WifiHook {

    private val log = Logger.withTag("Phantom-Wifi")

    @Volatile
    private var lastLat: Double? = null
    @Volatile
    private var lastLng: Double? = null
    @Volatile
    private var cachedSpoofedBssid: String? = null

    @JvmStatic
    fun apply() {
        hookGetScanResults()
        hookWifiInfoBssid()
        hookGetConnectionInfo()
    }

    /**
     * Return empty scan results — prevents WiFi-based location triangulation.
     * This only blocks location inference from nearby WiFi APs, not WiFi connectivity itself.
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

    /**
     * Hooks [WifiInfo.getBSSID] to return a stable, randomized locally-administered MAC address
     * when spoofing is active. This covers calls made through [WifiManager.getConnectionInfo]
     * as well as [android.net.ConnectivityManager.getNetworkCapabilities] -> [android.net.NetworkCapabilities.getTransportInfo].
     */
    private fun hookWifiInfoBssid() {
        try {
            XposedHelpers.findAndHookMethod(
                WifiInfo::class.java, "getBSSID",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val config = LocationHelper.getConfig() ?: return
                        val currentBssid = param.result as? String ?: return
                        if (currentBssid.isEmpty() || currentBssid == "<none>") return

                        val lat = config.getDouble(ConfigKeys.LATITUDE)
                        val lng = config.getDouble(ConfigKeys.LONGITUDE)
                        val spoofed = getOrCreateSpoofedBssid(lat, lng)
                        param.result = spoofed

                        try {
                            XposedHelpers.setObjectField(param.thisObject, "mBSSID", spoofed)
                        } catch (_: Throwable) {}
                    }
                }
            )
            log.d { "Hooked WifiInfo.getBSSID" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook WifiInfo.getBSSID" }
        }
    }

    /**
     * Hooks [WifiManager.getConnectionInfo] to sanitize the internal mBSSID field on the
     * returned WifiInfo object without returning null (which would break network connectivity).
     */
    private fun hookGetConnectionInfo() {
        try {
            XposedHelpers.findAndHookMethod(
                WifiManager::class.java, "getConnectionInfo",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        val wifiInfo = param.result ?: return
                        val config = LocationHelper.getConfig() ?: return
                        val lat = config.getDouble(ConfigKeys.LATITUDE)
                        val lng = config.getDouble(ConfigKeys.LONGITUDE)
                        val spoofed = getOrCreateSpoofedBssid(lat, lng)
                        try {
                            val currentBssid = XposedHelpers.getObjectField(wifiInfo, "mBSSID") as? String
                            if (!currentBssid.isNullOrEmpty() && currentBssid != "<none>") {
                                XposedHelpers.setObjectField(wifiInfo, "mBSSID", spoofed)
                            }
                        } catch (_: Throwable) {}
                    }
                }
            )
            log.d { "Hooked WifiManager.getConnectionInfo" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook WifiManager.getConnectionInfo" }
        }
    }

    /**
     * Generates a stable, randomized, locally-administered MAC address (02:xx:xx:xx:xx:xx)
     * derived from the configured spoofed location coordinates.
     */
    @Synchronized
    private fun getOrCreateSpoofedBssid(lat: Double, lng: Double): String {
        val currentCached = cachedSpoofedBssid
        if (currentCached != null && lastLat == lat && lastLng == lng) {
            return currentCached
        }
        val bits = java.lang.Double.doubleToLongBits(lat) xor (java.lang.Double.doubleToLongBits(lng) * 31L)
        val random = Random(bits)
        val bytes = ByteArray(5)
        random.nextBytes(bytes)
        val bssid = String.format(
            Locale.US,
            "02:%02x:%02x:%02x:%02x:%02x",
            bytes[0], bytes[1], bytes[2], bytes[3], bytes[4]
        )
        lastLat = lat
        lastLng = lng
        cachedSpoofedBssid = bssid
        return bssid
    }
}
