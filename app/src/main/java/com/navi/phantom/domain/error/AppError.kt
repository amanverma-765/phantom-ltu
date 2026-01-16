package com.navi.phantom.domain.error

sealed class AppError(val message: String) {
    data object PermissionDenied : AppError("Permission required to read installed apps")
    data object AppDetailsLoadFailed : AppError("Failed to load app details")
    data object Unknown : AppError("Something went wrong. Please try again")
}
