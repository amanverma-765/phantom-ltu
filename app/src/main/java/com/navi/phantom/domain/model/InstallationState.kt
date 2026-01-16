package com.navi.phantom.domain.model

import com.navi.phantom.domain.error.InstallationError

/**
 * Represents the state of an installation operation.
 */
sealed interface InstallationState {
    data object Idle : InstallationState
    data object Preparing : InstallationState
    data class Installing(val progress: Int, val max: Int) : InstallationState {
        val progressPercent: Float
            get() = if (max > 0) (progress.toFloat() / max) * 100f else 0f
    }
    data object AwaitingConfirmation : InstallationState
    data object Succeeded : InstallationState
    data class Failed(val error: InstallationError) : InstallationState
    data object Cancelled : InstallationState
}
