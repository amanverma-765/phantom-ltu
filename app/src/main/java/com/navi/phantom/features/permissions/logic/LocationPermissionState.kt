package com.navi.phantom.features.permissions.logic

sealed class LocationPermissionState {
    data object Granted : LocationPermissionState()
    data object CoarseOnly : LocationPermissionState()
    data object RequiresRationale : LocationPermissionState()
    data object NotRequested : LocationPermissionState()
    data object PermanentlyDenied : LocationPermissionState()
    data object Denied : LocationPermissionState()

    val isGranted: Boolean
        get() = this is Granted || this is CoarseOnly

    val isFineLocation: Boolean
        get() = this is Granted

    val isCoarseOnly: Boolean
        get() = this is CoarseOnly
}
