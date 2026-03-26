package com.navi.phantom.features.map.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.navi.phantom.features.map.components.CrosshairOverlay
import com.navi.phantom.features.map.components.LocationInfoCard
import com.navi.phantom.features.map.components.LocationInfoSheet
import com.navi.phantom.features.map.components.MapContent
import com.navi.phantom.features.map.components.MapControlCluster
import com.navi.phantom.features.map.components.MapLoadingSkeleton
import com.navi.phantom.features.map.components.MapTopBar
import com.navi.phantom.features.map.components.SearchSuggestionList
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
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

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
                isSearching = uiState.isSearching,
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
