package com.navi.phantom.features.map.logic

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LatLngResult(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val timestamp: Long? = null
)

private const val LOCATION_TIMEOUT_MS = 30_000L
private const val MAX_CACHE_AGE_MS = 60_000L

@SuppressLint("MissingPermission")
fun requestCurrentLocation(
    context: Context,
    onLocationReceived: (LatLngResult) -> Unit,
    onError: () -> Unit = {}
) {
    if (!hasFineLocationPermission(context)) {
        onError()
        return
    }

    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    var hasDeliveredResult = false

    fun deliverResult(result: LatLngResult) {
        if (!hasDeliveredResult) {
            hasDeliveredResult = true
            onLocationReceived(result)
        }
    }

    fun deliverError() {
        if (!hasDeliveredResult) {
            hasDeliveredResult = true
            onError()
        }
    }

    // First try getCurrentLocation API for a fresh fix
    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location ->
            if (location != null) {
                deliverResult(
                    LatLngResult(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = if (location.hasAltitude()) location.altitude else null,
                        accuracy = location.accuracy,
                        timestamp = location.time
                    )
                )
            } else {
                // No immediate location, request updates
                requestLocationUpdates(
                    context,
                    onSuccess = { deliverResult(it) },
                    onError = {
                        // Last resort: try cached location
                        tryGetCachedLocation(
                            context,
                            onResult = { deliverResult(it) },
                            onNoLocation = { deliverError() }
                        )
                    }
                )
            }
        }
        .addOnFailureListener {
            // API failed, try location updates
            requestLocationUpdates(
                context,
                onSuccess = { deliverResult(it) },
                onError = {
                    tryGetCachedLocation(
                        context,
                        onResult = { deliverResult(it) },
                        onNoLocation = { deliverError() }
                    )
                }
            )
        }
}

@SuppressLint("MissingPermission")
private fun requestLocationUpdates(
    context: Context,
    onSuccess: (LatLngResult) -> Unit,
    onError: () -> Unit
) {
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    var hasDeliveredResult = false
    var timeoutJob: Job? = null

    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1_000L
    )
        .setMaxUpdates(1)
        .setDurationMillis(LOCATION_TIMEOUT_MS)
        .build()

    val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (hasDeliveredResult) return
            hasDeliveredResult = true
            timeoutJob?.cancel()

            val location = result.lastLocation
            if (location != null) {
                onSuccess(
                    LatLngResult(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        timestamp = location.time
                    )
                )
            } else {
                onError()
            }
            fusedClient.removeLocationUpdates(this)
        }
    }

    fusedClient.requestLocationUpdates(
        locationRequest,
        callback,
        Looper.getMainLooper()
    )

    // Timeout using coroutine (cancellable - no memory leak)
    timeoutJob = CoroutineScope(Dispatchers.Main).launch {
        delay(LOCATION_TIMEOUT_MS)
        if (!hasDeliveredResult) {
            hasDeliveredResult = true
            fusedClient.removeLocationUpdates(callback)
            onError()
        }
    }
}

@SuppressLint("MissingPermission")
private fun tryGetCachedLocation(
    context: Context,
    onResult: (LatLngResult) -> Unit,
    onNoLocation: () -> Unit
) {
    if (!hasFineLocationPermission(context)) {
        onNoLocation()
        return
    }

    val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    fusedClient.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                val age = System.currentTimeMillis() - location.time
                if (age < MAX_CACHE_AGE_MS) {
                    onResult(
                        LatLngResult(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            timestamp = location.time
                        )
                    )
                } else {
                    onNoLocation()
                }
            } else {
                onNoLocation()
            }
        }
        .addOnFailureListener {
            onNoLocation()
        }
}

private fun hasFineLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}
