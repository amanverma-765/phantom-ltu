package com.navi.phantom.domain.model

import com.navi.phantom.domain.error.InstallationError

sealed interface InstallationState {
    data object Idle : InstallationState
    data object Preparing : InstallationState
    data class Installing(val progress: Int, val max: Int) : InstallationState
    data object AwaitingConfirmation : InstallationState
    data object Succeeded : InstallationState
    data class Failed(val error: InstallationError) : InstallationState
    data object Cancelled : InstallationState
}
