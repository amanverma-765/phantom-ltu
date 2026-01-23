package com.navi.phantom.features.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.navi.phantom.features.permissions.logic.GpsStateHandler
import com.navi.phantom.features.permissions.logic.LocationPermissionHandler
import com.navi.phantom.features.permissions.logic.LocationPermissionState
import com.navi.phantom.features.permissions.logic.openLocationSettings
import com.navi.phantom.features.permissions.logic.rememberCurrentPermissionState
import com.navi.phantom.features.permissions.logic.rememberGpsEnabledState

/**
 * Composable that handles the complete location permission flow including:
 * - Checking current permission state
 * - Showing rationale dialog when needed
 * - Requesting permissions via system dialog
 * - Handling permanently denied state with Settings redirect
 *
 * @param onGranted Called when permission is granted. [isFineLocation] is true for fine location,
 *                  false for coarse-only.
 * @param onDenied Called when user denies permission but can retry later.
 * @param onPermanentlyDenied Called when user has permanently denied and dismissed the Settings dialog.
 * @param requestOnStart If true, automatically starts the permission flow when composed.
 */
@Composable
fun RequestLocationPermission(
    onGranted: (isFineLocation: Boolean) -> Unit,
    onDenied: () -> Unit = {},
    onPermanentlyDenied: () -> Unit = {},
    requestOnStart: Boolean = true
) {
    LocationPermissionHandler(
        onGranted = onGranted,
        onDenied = onDenied,
        onPermanentlyDenied = onPermanentlyDenied,
        requestOnStart = requestOnStart
    )
}

/**
 * Remember the current location permission state. Updates automatically when
 * returning to the app (e.g., after changing settings).
 *
 * @return Current [LocationPermissionState]
 */
@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    return rememberCurrentPermissionState()
}

/**
 * Remember whether GPS/location services are enabled. Updates automatically
 * when returning to the app.
 *
 * @return true if GPS or network location is enabled
 */
@Composable
fun rememberGpsEnabled(): Boolean {
    return rememberGpsEnabledState()
}

/**
 * Composable that handles the complete location access flow:
 * 1. First checks and requests location permission
 * 2. Then checks if GPS is enabled and prompts to enable if not
 *
 * @param onReady Called when both permission is granted AND GPS is enabled.
 *                [isFineLocation] indicates the precision level.
 * @param onPermissionDenied Called if user denies permission.
 * @param onGpsDisabled Called if user skips enabling GPS.
 */
@Composable
fun RequestLocationAccess(
    onReady: (isFineLocation: Boolean) -> Unit,
    onPermissionDenied: () -> Unit = {},
    onGpsDisabled: () -> Unit = {}
) {
    var permissionGranted by remember { mutableStateOf(false) }
    var isFineLocation by remember { mutableStateOf(false) }
    var checkGps by remember { mutableStateOf(false) }

    val currentPermissionState = rememberLocationPermissionState()

    // Check if already granted using LaunchedEffect to avoid state mutation during composition
    LaunchedEffect(currentPermissionState) {
        if (currentPermissionState.isGranted && !permissionGranted) {
            permissionGranted = true
            isFineLocation = currentPermissionState.isFineLocation
            checkGps = true
        }
    }

    // Only request if not already granted
    if (!currentPermissionState.isGranted && !permissionGranted) {
        RequestLocationPermission(
            onGranted = { fine ->
                permissionGranted = true
                isFineLocation = fine
                checkGps = true
            },
            onDenied = onPermissionDenied,
            onPermanentlyDenied = onPermissionDenied,
            requestOnStart = true
        )
    }

    // After permission granted, check GPS
    if (checkGps) {
        GpsStateHandler(
            onGpsEnabled = { onReady(isFineLocation) },
            onGpsDisabled = onGpsDisabled,
            showDialogIfDisabled = true
        )
    }
}
