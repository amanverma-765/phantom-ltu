package com.navi.phantom.features.bootstrap.logic

sealed interface BootstrapUiEvent {
    data class LoadApp(val packageName: String) : BootstrapUiEvent
    data object StartBootstrap : BootstrapUiEvent
    data object InstallBootstrappedApp : BootstrapUiEvent
    data object CancelBootstrap : BootstrapUiEvent
    data object CopyErrorLog : BootstrapUiEvent
    data object DismissError : BootstrapUiEvent
}
