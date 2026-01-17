package com.navi.phantom.domain.model

sealed interface UninstallState {
    data object Idle : UninstallState
    data object Preparing : UninstallState
    data object Succeeded : UninstallState
    data class Failed(val message: String, val cause: Throwable? = null) : UninstallState
    data object Cancelled : UninstallState
}
