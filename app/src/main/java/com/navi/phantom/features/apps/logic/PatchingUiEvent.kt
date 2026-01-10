package com.navi.phantom.features.apps.logic

sealed interface PatchingUiEvent {
    data class LoadApp(val packageName: String) : PatchingUiEvent
    data object StartPatching : PatchingUiEvent
    data object InstallPatchedApp : PatchingUiEvent
    data object CancelPatching : PatchingUiEvent
}
