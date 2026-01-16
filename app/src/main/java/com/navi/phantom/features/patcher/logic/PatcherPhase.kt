package com.navi.phantom.features.patcher.logic

import com.navi.phantom.domain.error.PatchingError
import com.navi.phantom.domain.error.InstallationError
import com.navi.phantom.domain.error.PhantomError
import com.navi.phantom.domain.model.PatchingStep

sealed interface PatcherPhase {
    data object Ready : PatcherPhase

    data class Patching(
        val step: PatchingStep,
        val message: String
    ) : PatcherPhase

    data class Patched(val apkPath: String) : PatcherPhase

    data class Installing(
        val progress: Int,
        val max: Int,
        val message: String = "Installing..."
    ) : PatcherPhase {
        val progressPercent: Float
            get() = if (max > 0) (progress.toFloat() / max) * 100f else 0f
    }

    data object Installed : PatcherPhase

    data class AwaitingUninstallConfirm(
        val packageName: String,
        val reason: String
    ) : PatcherPhase

    data class Uninstalling(val packageName: String) : PatcherPhase

    data class Failed(
        val error: PhaseError,
        val failedDuring: FailedPhase,
        val canRetry: Boolean = true
    ) : PatcherPhase

    data object Cancelled : PatcherPhase
}

enum class FailedPhase {
    PATCHING,
    INSTALL,
    UNINSTALL
}

sealed class PhaseError(
    override val title: String,
    override val message: String,
    override val cause: Throwable? = null
) : PhantomError {

    data class Patching(val error: PatchingError) : PhaseError(
        title = error.title,
        message = error.message,
        cause = error.cause
    )

    data class Installation(val error: InstallationError) : PhaseError(
        title = error.title,
        message = error.message,
        cause = error.cause
    )

    data class Uninstall(override val cause: Throwable? = null) : PhaseError(
        title = "Uninstall Failed",
        message = cause?.message ?: "Failed to uninstall app",
        cause = cause
    )

    data class Generic(
        val errorMessage: String,
        override val cause: Throwable? = null
    ) : PhaseError(
        title = "Error",
        message = errorMessage,
        cause = cause
    )
}

val PatcherPhase.canStartPatching: Boolean
    get() = this is PatcherPhase.Ready ||
            this is PatcherPhase.Cancelled ||
            (this is PatcherPhase.Failed && failedDuring == FailedPhase.PATCHING)

val PatcherPhase.canInstall: Boolean
    get() = this is PatcherPhase.Patched ||
            (this is PatcherPhase.Failed && failedDuring == FailedPhase.INSTALL)
