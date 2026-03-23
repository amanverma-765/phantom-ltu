package com.navi.phantom.features.map.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.navi.phantom.BuildConfig
import com.navi.phantom.R
import com.navi.phantom.features.map.components.LocationInfoCard
import com.navi.phantom.features.map.components.LocationInfoSheet
import com.navi.phantom.features.map.components.MapControlCluster
import com.navi.phantom.features.map.components.MapLoadingSkeleton
import com.navi.phantom.features.map.components.SearchSuggestionList
import com.navi.phantom.features.map.components.MapTopBar
import com.navi.phantom.features.map.logic.MapPickerUiEvent
import com.navi.phantom.features.map.logic.MapPickerViewModel
import com.navi.phantom.features.map.logic.requestCurrentLocation
import com.navi.phantom.features.permissions.RequestLocationAccess
import com.navi.phantom.features.permissions.RequestLocationPermission
import com.navi.phantom.features.permissions.components.EnableGpsDialog
import com.navi.phantom.features.permissions.components.PreciseLocationDialog
import com.navi.phantom.features.permissions.logic.checkGpsEnabled
import com.navi.phantom.features.permissions.logic.openAppSettings
import com.navi.phantom.features.permissions.logic.openLocationSettings
import com.navi.phantom.features.permissions.rememberGpsEnabled
import com.navi.phantom.features.permissions.rememberLocationPermissionState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    placeId: Long? = null,
    viewModel: MapPickerViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val isDarkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    var showBottomSheet by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }
    var showPreciseLocationDialog by remember { mutableStateOf(false) }
    var isSatelliteMode by remember { mutableStateOf(false) }
    var requestLocationPermission by remember { mutableStateOf(false) }
    var isSearchMode by remember { mutableStateOf(false) }
    var isMapMoving by remember { mutableStateOf(false) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val locationPermissionState = rememberLocationPermissionState()

    RequestLocationAccess(
        onReady = { hasLocationPermission = true },
        onPermissionDenied = { },
        onGpsDisabled = { hasLocationPermission = true }
    )

    fun goToMyLocation() {
        if (!locationPermissionState.isGranted) {
            requestLocationPermission = true
            return
        }
        if (locationPermissionState.isCoarseOnly) {
            showPreciseLocationDialog = true
            return
        }
        if (!checkGpsEnabled(context)) {
            showGpsDialog = true
            return
        }
        isLoadingLocation = true
        requestCurrentLocation(
            context = context,
            onLocationReceived = { location ->
                isLoadingLocation = false
                viewModel.setAccuracyFromGps(location.accuracy)
                val target = LatLng(location.latitude, location.longitude)
                val zoom = accuracyToZoom(location.accuracy, location.latitude)
                mapRef?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(target, zoom),
                    1000
                )
            },
            onError = { isLoadingLocation = false }
        )
    }

    // Load existing place if editing
    LaunchedEffect(placeId) {
        if (placeId != null) {
            viewModel.onEvent(MapPickerUiEvent.LoadPlace(placeId))
        }
    }

    // Animate camera to loaded place
    LaunchedEffect(uiState.editingPlaceId) {
        if (uiState.editingPlaceId != null && mapRef != null) {
            val target = LatLng(uiState.currentLatitude, uiState.currentLongitude)
            val zoom = accuracyToZoom(uiState.accuracy, uiState.currentLatitude)
            mapRef?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(target, zoom),
                1000
            )
        }
    }

    // Animate camera to search result
    LaunchedEffect(uiState.navigateToSearchResult) {
        if (uiState.navigateToSearchResult && mapRef != null) {
            val target = LatLng(uiState.currentLatitude, uiState.currentLongitude)
            mapRef?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(target, 17.0),
                1000
            )
            viewModel.onEvent(MapPickerUiEvent.ClearSearch)
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            showBottomSheet = false
            viewModel.onEvent(MapPickerUiEvent.ResetSaveSuccess)
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            LocationInfoCard(
                latitude = uiState.currentLatitude,
                longitude = uiState.currentLongitude,
                accuracy = uiState.accuracy,
                address = uiState.address,
                isLoadingAddress = uiState.isLoadingAddress,
                onSelectLocation = {
                    viewModel.onEvent(
                        MapPickerUiEvent.SelectLocation(
                            uiState.currentLatitude,
                            uiState.currentLongitude
                        )
                    )
                    showBottomSheet = true
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Map layer
            if (!isMapLoaded) {
                MapLoadingSkeleton()
            }

            MapContent(
                isDarkTheme = isDarkTheme,
                isSatelliteMode = isSatelliteMode,
                onCameraMove = { center ->
                    viewModel.onEvent(
                        MapPickerUiEvent.UpdateCameraPosition(center.latitude, center.longitude)
                    )
                },
                onCameraMoving = { moving -> isMapMoving = moving },
                onMapReady = { map ->
                    mapRef = map
                    isMapLoaded = true
                }
            )

            // Crosshair overlay - full screen lines + red center dot
            CrosshairOverlay(isMapMoving = isMapMoving, isDarkTheme = isDarkTheme)

            // Top bar - floating search bar
            MapTopBar(
                onBackClick = onNavigateBack,
                onQueryChange = { query ->
                    viewModel.onEvent(MapPickerUiEvent.SearchLocation(query))
                },
                onClearSearch = { viewModel.onEvent(MapPickerUiEvent.ClearSearch) },
                isSearchMode = isSearchMode,
                onSearchModeChange = { isSearchMode = it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            )

            // Search suggestions dropdown
            if (isSearchMode && uiState.searchSuggestions.isNotEmpty()) {
                SearchSuggestionList(
                    suggestions = uiState.searchSuggestions,
                    onSuggestionClick = { placeId ->
                        viewModel.onEvent(MapPickerUiEvent.SelectSearchResult(placeId))
                        isSearchMode = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 72.dp)
                )
            }

            // Control cluster - right side, above bottom bar
            MapControlCluster(
                isSatelliteMode = isSatelliteMode,
                isLoadingLocation = isLoadingLocation,
                onLayerToggle = { isSatelliteMode = !isSatelliteMode },
                onMyLocation = { goToMyLocation() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp)
                    .padding(bottom = innerPadding.calculateBottomPadding() + 12.dp)
            )
        }
    }

    // Bottom sheet for saving location
    if (showBottomSheet) {
        LocationInfoSheet(
            sheetState = sheetState,
            latitude = uiState.currentLatitude,
            longitude = uiState.currentLongitude,
            accuracy = uiState.accuracy,
            address = uiState.address,
            placeName = uiState.placeName,
            onPlaceNameChange = { viewModel.onEvent(MapPickerUiEvent.UpdatePlaceName(it)) },
            onSave = { viewModel.onEvent(MapPickerUiEvent.SavePlace) },
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    showBottomSheet = false
                }
            },
            isSaving = uiState.isSaving,
            canSave = uiState.canSave,
            errorMessage = uiState.saveError
        )
    }

    // Dialogs
    if (showGpsDialog) {
        EnableGpsDialog(
            onEnableGps = {
                showGpsDialog = false
                context.openLocationSettings()
            },
            onDismiss = { showGpsDialog = false }
        )
    }

    if (showPreciseLocationDialog) {
        PreciseLocationDialog(
            onOpenSettings = {
                showPreciseLocationDialog = false
                context.openAppSettings()
            },
            onDismiss = { showPreciseLocationDialog = false }
        )
    }

    if (requestLocationPermission) {
        RequestLocationPermission(
            onGranted = {
                requestLocationPermission = false
                hasLocationPermission = true
                goToMyLocation()
            },
            onDenied = { requestLocationPermission = false },
            onPermanentlyDenied = { requestLocationPermission = false },
            requestOnStart = true
        )
    }
}

@Composable
private fun MapContent(
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

@Composable
private fun CrosshairOverlay(
    isMapMoving: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (isDarkTheme) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f)
    val dotColor = Color.Red

    // Animate dot scale: grows when dragging, settles with bounce
    val dotScale by animateFloatAsState(
        targetValue = if (isMapMoving) 1.5f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dotScale"
    )

    // Animate dashed circle: expands when dragging, shrinks on settle
    val circleScale by animateFloatAsState(
        targetValue = if (isMapMoving) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "circleScale"
    )

    // Animate circle opacity: more visible when dragging
    val circleAlpha by animateFloatAsState(
        targetValue = if (isMapMoving) 0.8f else 0.45f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "circleAlpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val center = androidx.compose.ui.geometry.Offset(centerX, centerY)

        // Horizontal line — full width
        drawLine(
            color = lineColor,
            start = androidx.compose.ui.geometry.Offset(0f, centerY),
            end = androidx.compose.ui.geometry.Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx()
        )

        // Vertical line — full height
        drawLine(
            color = lineColor,
            start = androidx.compose.ui.geometry.Offset(centerX, 0f),
            end = androidx.compose.ui.geometry.Offset(centerX, size.height),
            strokeWidth = 1.dp.toPx()
        )

        // Dashed circle around the dot
        val circleRadius = 18.dp.toPx() * circleScale
        val dashLength = 8.dp.toPx()
        val gapLength = 6.dp.toPx()
        drawCircle(
            color = if (isDarkTheme) Color.White.copy(alpha = circleAlpha) else Color.Black.copy(alpha = circleAlpha * 0.5f),
            radius = circleRadius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(dashLength, gapLength),
                    0f
                )
            )
        )

        // Red center dot — animated scale
        val dotRadiusPx = 5.dp.toPx() * dotScale
        drawCircle(
            color = dotColor,
            radius = dotRadiusPx,
            center = center
        )
    }
}

/**
 * Convert GPS accuracy (meters) to a MapLibre zoom level.
 * Zooms in tight for high accuracy (small radius) and zooms out for low accuracy.
 * At zoom z, 1 pixel ≈ (earthCircumference * cos(lat)) / (256 * 2^z) meters.
 * We want the accuracy circle to fit comfortably on screen (~200px radius).
 */
private fun accuracyToZoom(accuracyMeters: Float?, latitudeDeg: Double): Double {
    if (accuracyMeters == null || accuracyMeters <= 0f) return 17.0

    val latRad = Math.toRadians(latitudeDeg)
    val earthCircumference = 40_075_016.686
    val tileSize = 256.0
    val metersPerPixelAtZoom0 = earthCircumference * kotlin.math.cos(latRad) / tileSize

    // We want accuracyMeters to span ~200 pixels on screen
    val targetPixels = 200.0
    val zoom = kotlin.math.ln(metersPerPixelAtZoom0 * targetPixels / accuracyMeters) / kotlin.math.ln(2.0)

    return zoom.coerceIn(14.0, 20.0)
}
