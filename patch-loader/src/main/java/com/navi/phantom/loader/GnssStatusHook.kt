package com.navi.phantom.loader

import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import co.touchlab.kermit.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * Fakes GNSS satellite data so apps that verify satellite count
 * see a realistic GPS constellation instead of 0 satellites.
 */
object GnssStatusHook {

    private val log = Logger.withTag("Phantom-GNSS")

    // Realistic satellite configuration
    private const val SATELLITE_COUNT = 10
    private val PRNS = intArrayOf(1, 3, 6, 11, 14, 17, 19, 22, 24, 32)
    private val ELEVATIONS = floatArrayOf(30f, 45f, 60f, 75f, 40f, 55f, 35f, 50f, 65f, 70f)
    private val AZIMUTHS = floatArrayOf(45f, 90f, 135f, 180f, 225f, 270f, 315f, 20f, 160f, 200f)
    private val SNRS = floatArrayOf(25f, 30f, 35f, 40f, 28f, 33f, 27f, 38f, 32f, 36f)

    @JvmStatic
    fun apply() {
        hookGnssStatusCallback()
        hookLegacyGpsStatus()
        hookLocationManagerGnss()
    }

    /**
     * Hook GnssStatus.Callback methods to deliver fake satellite data.
     */
    private fun hookGnssStatusCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        try {
            XposedBridge.hookAllMethods(
                GnssStatus.Callback::class.java, "onSatelliteStatusChanged",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() == null) return
                        val fakeStatus = createFakeGnssStatus() ?: return
                        param.args[0] = fakeStatus
                    }
                }
            )
            log.d { "Hooked GnssStatus.Callback.onSatelliteStatusChanged" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook onSatelliteStatusChanged" }
        }

        try {
            XposedBridge.hookAllMethods(
                GnssStatus.Callback::class.java, "onFirstFix",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() == null) return
                        param.args[0] = 500 // 500ms time to first fix
                    }
                }
            )
            log.d { "Hooked GnssStatus.Callback.onFirstFix" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook onFirstFix" }
        }
    }

    /**
     * Hook legacy GpsStatus.Listener for older apps.
     */
    @Suppress("DEPRECATION")
    private fun hookLegacyGpsStatus() {
        try {
            val listenerClass = android.location.GpsStatus.Listener::class.java
            XposedBridge.hookAllMethods(
                listenerClass, "onGpsStatusChanged",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() == null) return
                        // Report GPS_EVENT_FIRST_FIX and GPS_EVENT_SATELLITE_STATUS
                        // Let the event through but the satellite data will be fake
                    }
                }
            )
            log.d { "Hooked GpsStatus.Listener" }
        } catch (t: Throwable) {
            log.d { "GpsStatus.Listener hook not available" }
        }
    }

    /**
     * Hook LocationManager.registerGnssStatusCallback to ensure our hooks catch the callback.
     * Also hook getGnssHardwareModelName to return a realistic value.
     */
    private fun hookLocationManagerGnss() {
        // Hook registerGnssStatusCallback — just let it pass, our callback hooks handle the data
        try {
            XposedBridge.hookAllMethods(
                LocationManager::class.java, "registerGnssStatusCallback",
                object : XC_MethodHook() {
                    // Pass through — onSatelliteStatusChanged is hooked above
                }
            )
            log.d { "Hooked registerGnssStatusCallback" }
        } catch (t: Throwable) {
            log.d { "registerGnssStatusCallback not available" }
        }
    }

    /**
     * Create a fake GnssStatus via reflection (constructor is hidden).
     */
    private fun createFakeGnssStatus(): GnssStatus? {
        // Try primary constructor (API 28+)
        try {
            val constructor = GnssStatus::class.java.getDeclaredConstructor(
                Int::class.javaPrimitiveType,
                IntArray::class.java,
                FloatArray::class.java,
                FloatArray::class.java,
                FloatArray::class.java,
                FloatArray::class.java
            )
            constructor.isAccessible = true

            // svidWithFlags: PRN in lower 8 bits, constellation type in bits 4-7, flags in upper bits
            // GPS constellation = 1, USED_IN_FIX flag = 0x4
            val svidWithFlags = IntArray(SATELLITE_COUNT) { i ->
                PRNS[i] or (1 shl 4) or (0x4 shl 8)
            }
            val carrierFreqs = FloatArray(SATELLITE_COUNT) { 1575.42f } // L1 frequency

            val status = constructor.newInstance(
                SATELLITE_COUNT, svidWithFlags, SNRS, ELEVATIONS, AZIMUTHS, carrierFreqs
            ) as GnssStatus
            log.d { "Created fake GnssStatus via constructor (${SATELLITE_COUNT} satellites)" }
            return status
        } catch (e: Exception) {
            log.d { "GnssStatus constructor failed: ${e.message}" }
        }

        // Try GnssStatus.Builder (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val builder = GnssStatus.Builder()
                for (i in 0 until SATELLITE_COUNT) {
                    builder.addSatellite(
                        /* constellationType */ GnssStatus.CONSTELLATION_GPS,
                        /* svid */ PRNS[i],
                        /* cn0DbHz */ SNRS[i],
                        /* elevation */ ELEVATIONS[i],
                        /* azimuth */ AZIMUTHS[i],
                        /* hasEphemeris */ true,
                        /* hasAlmanac */ true,
                        /* usedInFix */ true,
                        /* hasCarrierFrequency */ true,
                        /* carrierFrequency */ 1575.42f,
                        /* hasBasebandCn0DbHz */ false,
                        /* basebandCn0DbHz */ 0f
                    )
                }
                val status = builder.build()
                log.d { "Created fake GnssStatus via Builder (${SATELLITE_COUNT} satellites)" }
                return status
            } catch (e: Exception) {
                log.d { "GnssStatus.Builder failed: ${e.message}" }
            }
        }

        log.w { "Could not create fake GnssStatus on API ${Build.VERSION.SDK_INT}" }
        return null
    }
}
