package com.navi.phantom.features.patcher.logic

sealed interface PatcherUiEvent {
    data class LoadApp(val packageName: String) : PatcherUiEvent
    data object StartPatching : PatcherUiEvent
    data object Install : PatcherUiEvent
    data object Cancel : PatcherUiEvent
    data object Retry : PatcherUiEvent
    data object CopyError : PatcherUiEvent
    data object ConfirmUninstall : PatcherUiEvent
    data object DismissUninstallDialog : PatcherUiEvent
    data object LaunchApp : PatcherUiEvent
}
