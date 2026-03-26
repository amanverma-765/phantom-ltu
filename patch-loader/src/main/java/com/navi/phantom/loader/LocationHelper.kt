package com.navi.phantom.loader

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import com.navi.phantom.shared.ConfigKeys
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ThreadLocalRandom

/**
 * Shared utility for creating realistic fake Location objects from the manager's config bundle.
 * Produces Location objects that pass isComplete() checks, include satellite data,
 * and have realistic jitter on all values to appear like genuine GPS fixes.
 */
object LocationHelper {

    private val random: ThreadLocalRandom get() = ThreadLocalRandom.current()

    /** Tracks Location objects we created, so LocationHook can skip them. */
    private val phantomLocations: MutableSet<Location> = Collections.synchronizedSet(
        Collections.newSetFromMap(WeakHashMap())
    )

    /** Check if a Location object was created by us (no hooked method calls — safe from recursion). */
    fun isPhantom(location: Location): Boolean = phantomLocations.contains(location)

    // Short-lived cache to avoid repeated IPC calls when an app reads
    // multiple Location fields in quick succession
    private var cachedConfig: Bundle? = null
    private var cacheTimestamp = 0L
    private const val CACHE_TTL_MS = 1000L

    /** Volatile fast-path flag — avoids lock acquisition when spoofing is inactive. */
    @Volatile
    var isActive: Boolean = false
        private set

    /**
     * Get the active location config from the manager via IPC.
     * Returns null if no active location is assigned for this app.
     */
    @Synchronized
    fun getConfig(): Bundle? {
        val now = System.currentTimeMillis()
        val cached = cachedConfig
        if (cached != null && now - cacheTimestamp < CACHE_TTL_MS) {
            val active = cached.getBoolean(ConfigKeys.HAS_LOCATION, false)
            isActive = active
            return if (active) cached else null
        }

        val config = try {
            LSPApplication.applicationService?.configBundle
        } catch (_: Exception) {
            null
        }
        cachedConfig = config
        cacheTimestamp = now

        val active = config != null && config.getBoolean(ConfigKeys.HAS_LOCATION, false)
        isActive = active
        return if (active) config else null
    }


    /**
     * Create a realistic fake Location object from config data.
     */
    fun createFakeLocation(provider: String = "gps"): Location? {
        val config = getConfig() ?: return null
        return createFakeLocation(provider, config)
    }

    fun createFakeLocation(provider: String, config: Bundle): Location {
        val baseLat = config.getDouble(ConfigKeys.LATITUDE)
        val baseLng = config.getDouble(ConfigKeys.LONGITUDE)
        val baseAccuracy = config.getFloat(ConfigKeys.ACCURACY, 5f)
        val baseSpeed = config.getFloat(ConfigKeys.SPEED, 0f)
        val baseBearing = config.getFloat(ConfigKeys.BEARING, 0f)

        // Time with jitter (±50ms)
        val now = System.currentTimeMillis() + random.nextInt(100) - 50
        val elapsedNanos = SystemClock.elapsedRealtimeNanos() +
            (random.nextInt(100_000) - 50_000).toLong() // ±50μs

        // Speed with GPS noise (real GPS shows 0.1-0.3 m/s even stationary)
        val jitteredSpeed = (baseSpeed + random.nextFloat() * 0.3f).coerceAtLeast(0f)

        // Bearing: correlate with speed. If moving, bearing must be non-zero.
        // If stationary, bearing drifts randomly (real GPS behavior)
        val jitteredBearing = if (jitteredSpeed > 0.5f) {
            // Moving — use configured bearing with ±5° jitter
            val b = if (baseBearing == 0f) random.nextFloat() * 360f else baseBearing
            ((b + random.nextFloat() * 10f - 5f) % 360f + 360f) % 360f
        } else {
            // Stationary — random slow drift
            random.nextFloat() * 360f
        }

        // Satellite count: 8-12 (consistent with GnssStatusHook's 10)
        val satelliteCount = 8 + random.nextInt(5)

        return Location(provider).apply {
            // Coordinates with micro-jitter (~1m = 0.000008°)
            latitude = baseLat + random.nextGaussian() * 0.000008
            longitude = baseLng + random.nextGaussian() * 0.000008

            // Accuracy with ±2m jitter, minimum 1m
            accuracy = (baseAccuracy + random.nextFloat() * 4f - 2f).coerceAtLeast(1f)

            // Altitude: ~100m above sea level with ±3m jitter
            altitude = 100.0 + random.nextGaussian() * 3.0

            speed = jitteredSpeed
            bearing = jitteredBearing

            time = now
            elapsedRealtimeNanos = elapsedNanos

            // API 26+ accuracy fields (required for isComplete())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                verticalAccuracyMeters = 10f + random.nextFloat() * 20f  // 10-30m
                speedAccuracyMetersPerSecond = 0.3f + random.nextFloat() * 0.4f  // 0.3-0.7 m/s
                bearingAccuracyDegrees = 5f + random.nextFloat() * 10f  // 5-15°
            }

            // API 26+ elapsed realtime uncertainty
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                elapsedRealtimeUncertaintyNanos =
                    30_000_000.0 + random.nextDouble() * 40_000_000.0  // 30-70ms
            }

            // Extras with satellite count (real GPS always has this)
            extras = Bundle().apply {
                putInt("satellites", satelliteCount)
                putInt("maxCn0", 30 + random.nextInt(15))  // max signal strength
                putInt("meanCn0", 20 + random.nextInt(10))  // mean signal strength
            }
        }.also { phantomLocations.add(it) }
    }
}
