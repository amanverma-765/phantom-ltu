package com.navi.phantom.domain.errors

sealed class AppError(val message: String) {
    data object PermissionDenied :
        AppError("Permission required to read installed apps")

    data object NoAppsFound :
        AppError("No installed apps found")

    data object Unknown :
        AppError("Something went wrong. Please try again")
}