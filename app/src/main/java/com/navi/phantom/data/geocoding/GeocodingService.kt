package com.navi.phantom.data.geocoding

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class SearchSuggestion(
    val placeId: String,
    val name: String,
    val description: String
)

data class PlaceDetails(
    val latitude: Double,
    val longitude: Double,
    val address: String?
)

object GeocodingService {

    private val log = Logger.withTag("GeocodingService")

    private const val AUTOCOMPLETE_URL =
        "https://m.rapido.bike/pwa/api/unup/autocomplete/location"
    private const val PLACE_DETAILS_URL =
        "https://m.rapido.bike/pwa/api/unup/location/geocode/placeId"

    suspend fun searchLocation(
        query: String,
        lat: Double,
        lng: Double
    ): List<SearchSuggestion> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("lat", lat)
                put("lng", lng)
                put("searchWord", query)
            }
            val response = postJson(AUTOCOMPLETE_URL, body) ?: return@withContext emptyList()
            val status = response.optJSONObject("info")?.optString("status")
            if (status != "success") return@withContext emptyList()

            val data = response.optJSONArray("data") ?: return@withContext emptyList()
            (0 until data.length()).map { i ->
                val item = data.getJSONObject(i)
                SearchSuggestion(
                    placeId = item.getString("placeId"),
                    name = item.optString("name", ""),
                    description = item.getString("description")
                )
            }
        } catch (e: Exception) {
            log.d(e) { "Search failed" }
            emptyList()
        }
    }

    suspend fun getPlaceDetails(placeId: String): PlaceDetails? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("placeId", placeId)
            }
            val response = postJson(PLACE_DETAILS_URL, body) ?: return@withContext null
            val status = response.optJSONObject("info")?.optString("status")
            if (status != "success") return@withContext null

            val data = response.optJSONObject("data") ?: return@withContext null
            PlaceDetails(
                latitude = data.getDouble("lat"),
                longitude = data.getDouble("lng"),
                address = data.optString("address", null as String?)
            )
        } catch (e: Exception) {
            log.d(e) { "Place details failed" }
            null
        }
    }

    private fun postJson(urlString: String, body: JSONObject): JSONObject? {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val text = connection.inputStream.bufferedReader().readText()
                JSONObject(text)
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }
}
