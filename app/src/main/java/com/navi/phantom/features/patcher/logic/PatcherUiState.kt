package com.navi.phantom.features.bootstrap.logic

import com.navi.phantom.domain.model.BootstrapStep
import com.navi.phantom.domain.model.DetailedAppInfo

data class BootstrapUiState(
    val app: DetailedAppInfo? = null,
    val isLoadingApp: Boolean = false,
    val phase: BootstrapPhase = BootstrapPhase.Ready,
    val bootstrappedApkPath: String? = null
) {
    val hasCopyableError: Boolean
        get() = phase is BootstrapPhase.Failed

    val error: PhaseError?
        get() = (phase as? BootstrapPhase.Failed)?.error

    val statusMessage: String
        get() = when (val p = phase) {
            is BootstrapPhase.Ready -> "Ready to bootstrap"
            is BootstrapPhase.Bootstrapping -> "${p.step.title}..."
            is BootstrapPhase.Bootstrapped -> "Bootstrapped!"
            is BootstrapPhase.Installing -> {
                val percent = p.progressPercent.toInt()
                "Installing... $percent%"
            }
            is BootstrapPhase.Installed -> "Installed!"
            is BootstrapPhase.AwaitingUninstallConfirm -> "Reinstall required (${p.reason})"
            is BootstrapPhase.Uninstalling -> "Uninstalling..."
            is BootstrapPhase.Failed -> p.error.message
            is BootstrapPhase.Cancelled -> "Cancelled"
        }

    val currentStep: BootstrapStep?
        get() = when (val p = phase) {
            is BootstrapPhase.Bootstrapping -> p.step
            is BootstrapPhase.Bootstrapped -> BootstrapStep.COMPLETE
            is BootstrapPhase.Installed -> BootstrapStep.COMPLETE
            else -> null
        }

    val installationProgress: Int
        get() = (phase as? BootstrapPhase.Installing)?.progress ?: 0

    val installationProgressMax: Int
        get() = (phase as? BootstrapPhase.Installing)?.max ?: 100

    val isBootstrapping: Boolean
        get() = phase is BootstrapPhase.Bootstrapping
    
    val isBootstrapped: Boolean
        get() = phase is BootstrapPhase.Bootstrapped ||
                phase is BootstrapPhase.Installing ||
                phase is BootstrapPhase.Installed ||
                phase is BootstrapPhase.AwaitingUninstallConfirm ||
                phase is BootstrapPhase.Uninstalling ||
                (phase is BootstrapPhase.Failed && phase.failedDuring != FailedPhase.BOOTSTRAP)
    
    val isInstalling: Boolean
        get() = phase is BootstrapPhase.Installing
    
    val isInstalled: Boolean
        get() = phase is BootstrapPhase.Installed
    
    val isUninstalling: Boolean
        get() = phase is BootstrapPhase.Uninstalling
    
    val hasFailed: Boolean
        get() = phase is BootstrapPhase.Failed
    
    val showUninstallDialog: Boolean
        get() = phase is BootstrapPhase.AwaitingUninstallConfirm
    
    val conflictingPackageName: String?
        get() = when (val p = phase) {
            is BootstrapPhase.AwaitingUninstallConfirm -> p.packageName
            is BootstrapPhase.Uninstalling -> p.packageName
            else -> null
        }

    val canStartBootstrap: Boolean
        get() = app != null && !isLoadingApp && phase.canStartBootstrap

    val canInstall: Boolean
        get() = bootstrappedApkPath != null && phase.canInstall
}
