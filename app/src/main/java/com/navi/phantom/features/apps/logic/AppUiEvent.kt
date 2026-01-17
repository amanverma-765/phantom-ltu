package com.navi.phantom.features.apps.logic

sealed class AppUiEvent {
    data object GetAllDeviceApps : AppUiEvent()
    data class UpdateSearchQuery(val query: String) : AppUiEvent()
}
