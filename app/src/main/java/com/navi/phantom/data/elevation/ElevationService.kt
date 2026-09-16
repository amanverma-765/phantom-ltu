package com.navi.phantom.data.elevation

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object ElevationService {

    private val log = Logger.withTag("ElevationService")
    private const val BASE_URL = "https://api.open-meteo.com/v1/elevation"

    /**
     * Query ground elevation (in meters) above mean sea level for the given coordinates.
     * Uses Open-Meteo elevation API (free, open-access, no API key required).
     * Returns null if network request fails or coordinates are invalid.
     */
    suspend fun getElevation(latitude: Double, longitude: Double): Double? = withContext(Dispatchers.IO) {
        try {
            val urlString = String.format(
                Locale.US,
                "%s?latitude=%.6f&longitude=%.6f",
                BASE_URL,
                latitude,
                longitude
            )
            val connection = URL(urlString).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 4000
                connection.readTimeout = 4000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val text = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(text)
                    val elevationArray = json.optJSONArray("elevation")
                    if (elevationArray != null && elevationArray.length() > 0) {
                        elevationArray.getDouble(0)
                    } else {
                        null
                    }
                } else {
                    log.d { "Elevation API returned response code: ${connection.responseCode}" }
                    null
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            log.d(e) { "Failed to fetch elevation for ($latitude, $longitude)" }
            null
        }
    }
}
