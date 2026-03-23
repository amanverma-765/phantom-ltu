package com.navi.phantom.loader

import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import com.navi.phantom.shared.ConfigKeys

/**
 * Shared utility for creating fake Location objects from the manager's config bundle.
 * Used by LocationHook, LocationManagerHook, and FusedLocationHook.
 */
object LocationHelper {

    // Short-lived cache to avoid repeated IPC calls when an app reads
    // multiple Location fields in quick succession (lat, lng, acc, speed, etc.)
    @Volatile private var cachedConfig: Bundle? = null
    @Volatile private var cacheTimestamp = 0L
    private const val CACHE_TTL_MS = 1000L // 1 second

    /**
     * Get the active location config from the manager via IPC.
     * Returns null if no active location is assigned for this app.
     * Caches for 1 second to avoid IPC spam when multiple Location fields are read.
     */
    fun getConfig(): Bundle? {
        val now = System.currentTimeMillis()
        val cached = cachedConfig
        if (cached != null && now - cacheTimestamp < CACHE_TTL_MS) {
            return if (cached.getBoolean(ConfigKeys.HAS_LOCATION, false)) cached else null
        }

        val config = try {
            LSPApplication.applicationService?.configBundle
        } catch (_: Exception) {
            null
        }
        cachedConfig = config
        cacheTimestamp = now

        if (config == null || !config.getBoolean(ConfigKeys.HAS_LOCATION, false)) return null
        return config
    }

    /**
     * Create a realistic fake Location object from config data.
     */
    fun createFakeLocation(provider: String = "gps"): Location? {
        val config = getConfig() ?: return null
        return createFakeLocation(provider, config)
    }

    fun createFakeLocation(provider: String, config: Bundle): Location {
        return Location(provider).apply {
            latitude = config.getDouble(ConfigKeys.LATITUDE)
            longitude = config.getDouble(ConfigKeys.LONGITUDE)
            accuracy = config.getFloat(ConfigKeys.ACCURACY, 5f)
            speed = config.getFloat(ConfigKeys.SPEED, 0f)
            bearing = config.getFloat(ConfigKeys.BEARING, 0f)
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            extras = Bundle() // Empty extras — strips mock provider flag
        }
    }
}
