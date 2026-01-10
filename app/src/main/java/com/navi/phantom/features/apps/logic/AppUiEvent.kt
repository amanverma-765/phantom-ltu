package com.navi.phantom.features.apps.logic

import com.navi.phantom.domain.models.InstalledApp

sealed class AppUiEvent {
    data object GetAllInstalledApps : AppUiEvent()
    data class UpdateSearchQuery(val query: String) : AppUiEvent()
    data class SelectApp(val app: InstalledApp) : AppUiEvent()
}