package com.navi.phantom.data.geocoding

import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Parses coordinates from raw text input or Google Maps URLs.
 * Returns null if the input doesn't match any known format.
 */
object CoordinateParser {

    data class ParsedCoordinate(
        val latitude: Double,
        val longitude: Double,
        val needsResolve: Boolean = false,
        val originalUrl: String? = null
    )

    // Decimal coordinate pair: "28.6139, 77.2090" or "28.6139 77.2090"
    private val COORD_PAIR = Regex(
        """^\s*(-?\d{1,3}(?:\.\d+)?)\s*[,\s]\s*(-?\d{1,3}(?:\.\d+)?)\s*$"""
    )

    // Google Maps URLs with @lat,lng or q=lat,lng
    private val MAPS_AT = Regex("""@(-?\d+\.?\d*),(-?\d+\.?\d*)""")
    private val MAPS_Q = Regex("""[?&]q=(-?\d+\.?\d*),(-?\d+\.?\d*)""")

    // Short Google Maps URLs that need redirect resolution
    private val SHORT_URL = Regex("""https?://(goo\.gl/maps/|maps\.app\.goo\.gl/).+""")

    fun parse(input: String): ParsedCoordinate? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        // Try direct coordinate pair
        COORD_PAIR.matchEntire(trimmed)?.let { match ->
            return validCoord(match.groupValues[1].toDouble(), match.groupValues[2].toDouble())
        }

        // Try Google Maps URL patterns
        if (trimmed.startsWith("http")) {
            MAPS_AT.find(trimmed)?.let { match ->
                return validCoord(match.groupValues[1].toDouble(), match.groupValues[2].toDouble())
            }
            MAPS_Q.find(trimmed)?.let { match ->
                return validCoord(match.groupValues[1].toDouble(), match.groupValues[2].toDouble())
            }

            // Short URL — needs resolution
            if (SHORT_URL.matches(trimmed)) {
                return ParsedCoordinate(
                    latitude = 0.0,
                    longitude = 0.0,
                    needsResolve = true,
                    originalUrl = trimmed
                )
            }
        }

        return null
    }

    // Extract place name from /maps/place/<name>/ URLs
    private val MAPS_PLACE_NAME = Regex("""/maps/place/([^/?]+)""")

    /**
     * Resolve a short Google Maps URL to coordinates.
     *
     * 1. Follows the HTTP redirect to get the full Google Maps URL
     * 2. Tries to extract @lat,lng or ?q=lat,lng from the redirect URL
     * 3. Falls back to extracting the place name and geocoding via Android's Geocoder
     */
    suspend fun resolveShortUrl(url: String, geocoder: Geocoder): ParsedCoordinate? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")

            val redirectUrl = if (connection.responseCode in 301..302) {
                connection.getHeaderField("Location")
            } else null
            connection.disconnect()

            if (redirectUrl == null) return@withContext null

            // 1. Try extracting coordinates directly from the redirect URL
            MAPS_AT.find(redirectUrl)?.let { match ->
                validCoord(match.groupValues[1].toDouble(), match.groupValues[2].toDouble())
                    ?.let { return@withContext it }
            }
            MAPS_Q.find(redirectUrl)?.let { match ->
                validCoord(match.groupValues[1].toDouble(), match.groupValues[2].toDouble())
                    ?.let { return@withContext it }
            }

            // 2. Extract place name and geocode via Android's Geocoder
            MAPS_PLACE_NAME.find(redirectUrl)?.let { match ->
                val placeName = java.net.URLDecoder.decode(match.groupValues[1], "UTF-8")
                    .replace("+", " ")
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(placeName, 1)
                val first = results?.firstOrNull()
                if (first != null) {
                    validCoord(first.latitude, first.longitude)?.let { return@withContext it }
                }
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    private fun validCoord(lat: Double, lng: Double): ParsedCoordinate? {
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return ParsedCoordinate(latitude = lat, longitude = lng)
    }
}
