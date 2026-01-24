package com.navi.phantom.features.map.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.navi.phantom.features.map.components.LocationInfoSheet
import com.navi.phantom.features.map.components.MapControlCluster
import com.navi.phantom.features.map.components.MapLoadingSkeleton
import com.navi.phantom.features.map.components.MapTopBar
import com.navi.phantom.features.map.components.SelectLocationPill
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
    viewModel: MapPickerViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val isDarkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    var showBottomSheet by remember { mutableStateOf(false) }
    var currentCenter by remember { mutableStateOf(LatLng(DEFAULT_LAT, DEFAULT_LNG)) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }
    var showPreciseLocationDialog by remember { mutableStateOf(false) }
    var isSatelliteMode by remember { mutableStateOf(false) }
    var requestLocationPermission by remember { mutableStateOf(false) }
    var isMapMoving by remember { mutableStateOf(false) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val locationPermissionState = rememberLocationPermissionState()
    val isGpsEnabled = rememberGpsEnabled()

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
                val target = LatLng(location.latitude, location.longitude)
                mapRef?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(target, 17.0),
                    1000
                )
            },
            onError = { isLoadingLocation = false }
        )
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            showBottomSheet = false
            viewModel.onEvent(MapPickerUiEvent.ResetSaveSuccess)
            onNavigateBack()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Map layer
        if (!isMapLoaded) {
            MapLoadingSkeleton()
        }

        MapContent(
            isDarkTheme = isDarkTheme,
            isSatelliteMode = isSatelliteMode,
            onCameraMove = { center -> currentCenter = center },
            onCameraMoving = { moving -> isMapMoving = moving },
            onMapReady = { map ->
                mapRef = map
                isMapLoaded = true
            }
        )

        // Crosshair overlay - centered
        val crosshairScale by animateFloatAsState(
            targetValue = if (isMapMoving) 1.15f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "crosshairScale"
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_scope),
            contentDescription = "Location crosshair",
            tint = Color.Unspecified,
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .scale(crosshairScale)
        )

        // Top bar - floating glass panel
        MapTopBar(
            latitude = currentCenter.latitude,
            longitude = currentCenter.longitude,
            onBackClick = onNavigateBack,
            onSearch = { query ->
                // TODO: Implement search
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        )

        // Control cluster - right side
        MapControlCluster(
            isSatelliteMode = isSatelliteMode,
            isLoadingLocation = isLoadingLocation,
            onLayerToggle = { isSatelliteMode = !isSatelliteMode },
            onMyLocation = { goToMyLocation() },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )

        // Select location pill - bottom center
        SelectLocationPill(
            onClick = {
                viewModel.onEvent(
                    MapPickerUiEvent.SelectLocation(
                        currentCenter.latitude,
                        currentCenter.longitude
                    )
                )
                showBottomSheet = true
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        )
    }

    // Bottom sheet for location details
    if (showBottomSheet) {
        LocationInfoSheet(
            sheetState = sheetState,
            latitude = currentCenter.latitude,
            longitude = currentCenter.longitude,
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
