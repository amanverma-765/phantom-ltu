package com.navi.phantom.data.routing

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class RouteResult(
    val points: List<Pair<Double, Double>>,
    val distanceText: String?,
    val durationText: String?
)

/**
 * Keyless routing via the public OSRM demo server. Returns the full road-following
 * geometry (GeoJSON), plus distance/duration. No API key.
 *
 * Google's keyless directions endpoint only exposes coarse turn-points (no detailed
 * polyline — its own map draws the route from vector-tile overlays), so OSRM is used
 * for correctly-formed geometry. Kept as a stateless `object`, mirroring GeocodingService.
 */
object RoutingService {

    private val log = Logger.withTag("RoutingService")

    // lng,lat order is intentional — OSRM takes {lng},{lat};{lng},{lat}
    private const val OSRM_BASE = "https://router.project-osrm.org/route/v1/driving"

    suspend fun getRoute(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double
    ): RouteResult? = withContext(Dispatchers.IO) {
        try {
            val url = "$OSRM_BASE/$originLng,$originLat;$destLng,$destLat" +
                "?overview=full&geometries=geojson"

            val body = getText(url) ?: return@withContext null
            val json = JSONObject(body)
            if (json.optString("code") != "Ok") return@withContext null

            val route = json.optJSONArray("routes")?.optJSONObject(0) ?: return@withContext null
            val coords = route.optJSONObject("geometry")?.optJSONArray("coordinates")
                ?: return@withContext null

            val points = ArrayList<Pair<Double, Double>>(coords.length())
            for (i in 0 until coords.length()) {
                val pair = coords.optJSONArray(i) ?: continue
                // GeoJSON is [lng, lat] — flip to (lat, lng) for Leaflet
                points.add(pair.getDouble(1) to pair.getDouble(0))
            }
            if (points.size < 2) return@withContext null

            RouteResult(
                points = points,
                distanceText = formatDistance(route.optDouble("distance", 0.0)),
                durationText = formatDuration(route.optDouble("duration", 0.0))
            )
        } catch (e: Exception) {
            log.d(e) { "Route fetch failed" }
            null
        }
    }

    private fun formatDistance(meters: Double): String =
        if (meters >= 1000) "%.1f km".format(meters / 1000) else "${meters.roundToInt()} m"

    private fun formatDuration(seconds: Double): String {
        val mins = (seconds / 60).roundToInt()
        return if (mins >= 60) "${mins / 60} h ${mins % 60} min" else "$mins min"
    }

    private fun getText(urlString: String): String? {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            // OSRM's public demo server 403s the default Java/Android User-Agent.
            connection.setRequestProperty("User-Agent", "phantom-ltu")
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().readText()
            } else {
                log.w { "Routing HTTP $code" }
                null
            }
        } finally {
            connection.disconnect()
        }
    }
}
