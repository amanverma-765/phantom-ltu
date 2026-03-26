package com.navi.phantom.loader

import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import co.touchlab.kermit.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.util.concurrent.ThreadLocalRandom

/**
 * Fakes GNSS satellite data so apps that verify satellite count
 * see a realistic GPS constellation instead of 0 satellites.
 */
object GnssStatusHook {

    private val log = Logger.withTag("Phantom-GNSS")
    private val random: ThreadLocalRandom get() = ThreadLocalRandom.current()

    // Pool of satellites to randomly select from (larger than what we report)
    private data class SatelliteInfo(val prn: Int, val elevation: Float, val azimuth: Float, val snr: Float)

    private val SATELLITE_POOL = listOf(
        SatelliteInfo(1, 30f, 45f, 25f),
        SatelliteInfo(3, 45f, 90f, 30f),
        SatelliteInfo(6, 60f, 135f, 35f),
        SatelliteInfo(9, 25f, 200f, 24f),
        SatelliteInfo(11, 75f, 180f, 40f),
        SatelliteInfo(14, 40f, 225f, 28f),
        SatelliteInfo(17, 55f, 270f, 33f),
        SatelliteInfo(19, 35f, 315f, 27f),
        SatelliteInfo(22, 50f, 20f, 38f),
        SatelliteInfo(24, 65f, 160f, 32f),
        SatelliteInfo(27, 42f, 110f, 29f),
        SatelliteInfo(30, 58f, 250f, 34f),
        SatelliteInfo(32, 70f, 300f, 36f),
    )

    @JvmStatic
    fun apply() {
        hookGnssStatusCallback()
        hookGetGpsStatus()
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
                        param.args[0] = 2000 + random.nextInt(3000) // 2-5s realistic warm start
                    }
                }
            )
            log.d { "Hooked GnssStatus.Callback.onFirstFix" }
        } catch (t: Throwable) {
            log.w(t) { "Failed to hook onFirstFix" }
        }
    }

    /**
     * Hook legacy LocationManager.getGpsStatus() to return null when spoofing is active.
     * This prevents apps from reading real satellite data via the deprecated GpsStatus API.
     */
    @Suppress("DEPRECATION")
    private fun hookGetGpsStatus() {
        try {
            XposedBridge.hookAllMethods(
                LocationManager::class.java, "getGpsStatus",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam<*>) {
                        if (LocationHelper.getConfig() == null) return
                        param.result = null
                    }
                }
            )
            log.d { "Hooked getGpsStatus" }
        } catch (t: Throwable) {
            log.d { "getGpsStatus not available" }
        }
    }

    /**
     * Hook LocationManager.registerGnssStatusCallback to ensure our hooks catch the callback.
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
     * Select a random subset of satellites and apply jitter to their values.
     */
    private fun randomizeSatellites(): List<SatelliteInfo> {
        val count = 8 + random.nextInt(5) // 8-12 satellites
        return SATELLITE_POOL.shuffled(random).take(count).map { sat ->
            sat.copy(
                elevation = (sat.elevation + random.nextFloat() * 4f - 2f).coerceIn(5f, 85f),
                azimuth = ((sat.azimuth + random.nextFloat() * 6f - 3f) % 360f + 360f) % 360f,
                snr = (sat.snr + random.nextFloat() * 4f - 2f).coerceIn(15f, 45f),
            )
        }
    }

    /**
     * Create a fake GnssStatus via reflection (constructor is hidden).
     */
    private fun createFakeGnssStatus(): GnssStatus? {
        val satellites = randomizeSatellites()
        val count = satellites.size
        val prns = satellites.map { it.prn }.toIntArray()
        val snrs = satellites.map { it.snr }.toFloatArray()
        val elevations = satellites.map { it.elevation }.toFloatArray()
        val azimuths = satellites.map { it.azimuth }.toFloatArray()

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
            val svidWithFlags = IntArray(count) { i ->
                prns[i] or (1 shl 4) or (0x4 shl 8)
            }
            val carrierFreqs = FloatArray(count) { 1575.42f } // L1 frequency

            val status = constructor.newInstance(
                count, svidWithFlags, snrs, elevations, azimuths, carrierFreqs
            ) as GnssStatus
            log.d { "Created fake GnssStatus via constructor ($count satellites)" }
            return status
        } catch (e: Exception) {
            log.d { "GnssStatus constructor failed: ${e.message}" }
        }

        // Try GnssStatus.Builder (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val builder = GnssStatus.Builder()
                for (sat in satellites) {
                    builder.addSatellite(
                        /* constellationType */ GnssStatus.CONSTELLATION_GPS,
                        /* svid */ sat.prn,
                        /* cn0DbHz */ sat.snr,
                        /* elevation */ sat.elevation,
                        /* azimuth */ sat.azimuth,
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
                log.d { "Created fake GnssStatus via Builder ($count satellites)" }
                return status
            } catch (e: Exception) {
                log.d { "GnssStatus.Builder failed: ${e.message}" }
            }
        }

        log.w { "Could not create fake GnssStatus on API ${Build.VERSION.SDK_INT}" }
        return null
    }
}
