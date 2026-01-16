package com.navi.phantom.features.patcher.logic

import com.navi.phantom.domain.model.PatchingStep
import com.navi.phantom.domain.model.DeviceAppDetails

data class PatcherUiState(
    val app: DeviceAppDetails? = null,
    val isLoadingApp: Boolean = false,
    val phase: PatcherPhase = PatcherPhase.Ready,
    val patchedApkPath: String? = null
) {
    val hasCopyableError: Boolean
        get() = phase is PatcherPhase.Failed

    val error: PhaseError?
        get() = (phase as? PatcherPhase.Failed)?.error

    val statusMessage: String
        get() = when (val p = phase) {
            is PatcherPhase.Ready -> "Ready to patch"
            is PatcherPhase.Patching -> "${p.step.title}..."
            is PatcherPhase.Patched -> "Patched!"
            is PatcherPhase.Installing -> {
                val percent = p.progressPercent.toInt()
                "Installing... $percent%"
            }
            is PatcherPhase.Installed -> "Installed!"
            is PatcherPhase.AwaitingUninstallConfirm -> "Reinstall required (${p.reason})"
            is PatcherPhase.Uninstalling -> "Uninstalling..."
            is PatcherPhase.Failed -> p.error.message
            is PatcherPhase.Cancelled -> "Cancelled"
        }

    val currentStep: PatchingStep?
        get() = when (val p = phase) {
            is PatcherPhase.Patching -> p.step
            is PatcherPhase.Patched -> PatchingStep.COMPLETE
            is PatcherPhase.Installed -> PatchingStep.COMPLETE
            else -> null
        }

    val installationProgress: Int
        get() = (phase as? PatcherPhase.Installing)?.progress ?: 0

    val installationProgressMax: Int
        get() = (phase as? PatcherPhase.Installing)?.max ?: 100

    val isPatching: Boolean
        get() = phase is PatcherPhase.Patching

    val isPatched: Boolean
        get() = phase is PatcherPhase.Patched ||
                phase is PatcherPhase.Installing ||
                phase is PatcherPhase.Installed ||
                phase is PatcherPhase.AwaitingUninstallConfirm ||
                phase is PatcherPhase.Uninstalling ||
                (phase is PatcherPhase.Failed && phase.failedDuring != FailedPhase.PATCHING)

    val isInstalling: Boolean
        get() = phase is PatcherPhase.Installing

    val isInstalled: Boolean
        get() = phase is PatcherPhase.Installed

    val isUninstalling: Boolean
        get() = phase is PatcherPhase.Uninstalling

    val hasFailed: Boolean
        get() = phase is PatcherPhase.Failed

    val showUninstallDialog: Boolean
        get() = phase is PatcherPhase.AwaitingUninstallConfirm

    val conflictingPackageName: String?
        get() = when (val p = phase) {
            is PatcherPhase.AwaitingUninstallConfirm -> p.packageName
            is PatcherPhase.Uninstalling -> p.packageName
            else -> null
        }

    val canStartPatching: Boolean
        get() = app != null && !isLoadingApp && phase.canStartPatching

    val canInstall: Boolean
        get() = patchedApkPath != null && phase.canInstall
}
