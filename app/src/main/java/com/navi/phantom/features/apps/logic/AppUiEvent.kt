package com.navi.phantom.features.apps.logic

import com.navi.phantom.domain.model.PatchedApp

sealed class AppUiEvent {
    data object GetAllDeviceApps : AppUiEvent()
    data class UpdateSearchQuery(val query: String) : AppUiEvent()
    data class DeletePatchedApp(val patchedApp: PatchedApp) : AppUiEvent()
}