package com.navi.phantom.features.bootstrap.logic

sealed interface BootstrapUiEvent {
    data class LoadApp(val packageName: String) : BootstrapUiEvent
    data object StartBootstrap : BootstrapUiEvent
    data object Install : BootstrapUiEvent
    data object Cancel : BootstrapUiEvent
    data object Retry : BootstrapUiEvent
    data object CopyError : BootstrapUiEvent
    data object ConfirmUninstall : BootstrapUiEvent
    data object DismissUninstallDialog : BootstrapUiEvent
    data object LaunchApp : BootstrapUiEvent
}
