package com.navi.phantom.features.bootstrap.logic

import com.navi.phantom.domain.error.BootstrapError
import com.navi.phantom.domain.error.InstallationError
import com.navi.phantom.domain.error.PhantomError
import com.navi.phantom.domain.model.BootstrapStep

sealed interface BootstrapPhase {
    data object Ready : BootstrapPhase

    data class Bootstrapping(
        val step: BootstrapStep,
        val message: String
    ) : BootstrapPhase

    data class Bootstrapped(val apkPath: String) : BootstrapPhase

    data class Installing(
        val progress: Int,
        val max: Int,
        val message: String = "Installing..."
    ) : BootstrapPhase {
        val progressPercent: Float
            get() = if (max > 0) (progress.toFloat() / max) * 100f else 0f
    }

    data object Installed : BootstrapPhase

    data class AwaitingUninstallConfirm(
        val packageName: String,
        val reason: String
    ) : BootstrapPhase

    data class Uninstalling(
        val packageName: String,
        val willReinstall: Boolean = true
    ) : BootstrapPhase

    data class Failed(
        val error: PhaseError,
        val failedDuring: FailedPhase,
        val canRetry: Boolean = true
    ) : BootstrapPhase

    data object Cancelled : BootstrapPhase
}

enum class FailedPhase {
    BOOTSTRAP,
    INSTALL,
    UNINSTALL
}

sealed class PhaseError(
    override val title: String,
    override val message: String,
    override val cause: Throwable? = null
) : PhantomError {

    data class Bootstrap(val error: BootstrapError) : PhaseError(
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

val BootstrapPhase.canStartBootstrap: Boolean
    get() = this is BootstrapPhase.Ready ||
            this is BootstrapPhase.Cancelled ||
            (this is BootstrapPhase.Failed && failedDuring == FailedPhase.BOOTSTRAP)

val BootstrapPhase.canInstall: Boolean
    get() = this is BootstrapPhase.Bootstrapped ||
            (this is BootstrapPhase.Failed && failedDuring == FailedPhase.INSTALL)
