package com.navi.phantom.domain.errors

sealed class AppError(val message: String) {
    // Apps feature errors
    data object PermissionDenied : AppError("Permission required to read installed apps")

    // Bootstrap feature errors
    data object AppDetailsLoadFailed : AppError("Failed to load app details")
    data object BootstrapFailed : AppError("Bootstrap failed. Please try again")

    // Generic errors
    data object Unknown : AppError("Something went wrong. Please try again")
}