package com.navi.phantom.features.map.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.navi.phantom.BuildConfig
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

private const val MAPTILER_STREETS_LIGHT_URL =
    "https://api.maptiler.com/maps/streets-v2/style.json?key="
private const val MAPTILER_STREETS_DARK_URL =
    "https://api.maptiler.com/maps/streets-v2-dark/style.json?key="
private const val MAPTILER_HYBRID_URL =
    "https://api.maptiler.com/maps/hybrid/style.json?key="

private const val DEFAULT_LAT = 28.6139
private const val DEFAULT_LNG = 77.2090
private const val DEFAULT_ZOOM = 10.0

@Composable
internal fun MapContent(
    isDarkTheme: Boolean,
    isSatelliteMode: Boolean,
    onCameraMove: (LatLng) -> Unit,
    onCameraMoving: (Boolean) -> Unit,
    onMapReady: (MapLibreMap) -> Unit
) {
    val context = LocalContext.current
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    val styleUrl = when {
        isSatelliteMode -> MAPTILER_HYBRID_URL
        isDarkTheme -> MAPTILER_STREETS_DARK_URL
        else -> MAPTILER_STREETS_LIGHT_URL
    } + BuildConfig.MAPTILER_API_KEY

    LaunchedEffect(styleUrl) {
        mapLibreMap?.setStyle(styleUrl)
    }

    AndroidView(
        factory = { _ ->
            mapView.apply {
                getMapAsync { map ->
                    mapLibreMap = map
                    onMapReady(map)
                    map.setStyle(styleUrl) {
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(DEFAULT_LAT, DEFAULT_LNG))
                            .zoom(DEFAULT_ZOOM)
                            .build()

                        map.addOnCameraMoveStartedListener {
                            onCameraMoving(true)
                        }

                        map.addOnCameraIdleListener {
                            onCameraMoving(false)
                            map.cameraPosition.target?.let { target ->
                                onCameraMove(target)
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
