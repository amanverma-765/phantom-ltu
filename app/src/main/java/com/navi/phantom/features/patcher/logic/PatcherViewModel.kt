package com.navi.phantom.features.patcher.logic

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.error.AppError
import com.navi.phantom.domain.error.InstallationError
import com.navi.phantom.domain.model.PatchingOptions
import com.navi.phantom.domain.model.PatchingProgress
import com.navi.phantom.domain.model.PatchingStep
import com.navi.phantom.domain.model.InstallationState
import com.navi.phantom.domain.model.UninstallState
import com.navi.phantom.domain.usecase.PatcherUseCase
import com.navi.phantom.domain.usecase.DeviceAppUseCase
import com.navi.phantom.domain.usecase.InstallationUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PatcherViewModel(
    private val application: Application,
    private val deviceAppUseCase: DeviceAppUseCase,
    private val patcherUseCase: PatcherUseCase,
    private val installationUseCase: InstallationUseCase
) : ViewModel() {

    private val log = Logger.withTag("PatcherViewModel")

    private val _uiState = MutableStateFlow(PatcherUiState())
    val uiState: StateFlow<PatcherUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private var currentJob: Job? = null

    fun onEvent(event: PatcherUiEvent) {
        when (event) {
            is PatcherUiEvent.LoadApp -> loadApp(event.packageName)
            is PatcherUiEvent.StartPatching -> startPatching()
            is PatcherUiEvent.Install -> install()
            is PatcherUiEvent.Cancel -> cancel()
            is PatcherUiEvent.Retry -> retry()
            is PatcherUiEvent.CopyError -> copyError()
            is PatcherUiEvent.ConfirmUninstall -> confirmUninstall()
            is PatcherUiEvent.DismissUninstallDialog -> dismissUninstallDialog()
            is PatcherUiEvent.LaunchApp -> launchApp()
        }
    }

    private fun loadApp(packageName: String) {
        currentJob?.cancel()
        currentJob = null

        _uiState.update {
            PatcherUiState(isLoadingApp = true)
        }

        viewModelScope.launch {
            deviceAppUseCase.getAppDetails(packageName)
                .onSuccess { appInfo ->
                    _uiState.update {
                        it.copy(
                            app = appInfo,
                            isLoadingApp = false,
                            phase = PatcherPhase.Ready
                        )
                    }
                }
                .onFailure { e ->
                    log.e(e) { "Failed to load app details" }
                    _uiState.update {
                        it.copy(
                            isLoadingApp = false,
                            phase = PatcherPhase.Failed(
                                error = PhaseError.Generic(AppError.AppDetailsLoadFailed.message),
                                failedDuring = FailedPhase.PATCHING,
                                canRetry = false
                            )
                        )
                    }
                }
        }
    }

    private fun startPatching() {
        val app = _uiState.value.app ?: return

        currentJob?.cancel()
        setPhase(PatcherPhase.Patching(PatchingStep.PARSE_APK, "Initializing..."))

        currentJob = viewModelScope.launch {
            val splitApkPaths = patcherUseCase.getSplitApkPaths(app.packageName)
            log.d { "Starting patching for ${app.packageName} with ${splitApkPaths.size} split APKs" }

            patcherUseCase.patch(
                packageName = app.packageName,
                versionCode = app.versionCode,
                apkPath = app.apkPath,
                splitApkPaths = splitApkPaths,
                options = PatchingOptions(sigbypassLevel = 1)
            ).collect { progress ->
                when (progress) {
                    is PatchingProgress.Step -> {
                        setPhase(PatcherPhase.Patching(progress.step, progress.message))
                    }
                    is PatchingProgress.Completed -> {
                        log.i { "Patching completed: ${progress.outputPath}" }
                        _uiState.update {
                            it.copy(
                                phase = PatcherPhase.Patched(progress.outputPath),
                                patchedApkPath = progress.outputPath
                            )
                        }
                    }
                    is PatchingProgress.Failed -> {
                        log.e { "Patching failed: ${progress.error.title}" }
                        setPhase(PatcherPhase.Failed(
                            error = PhaseError.Patching(progress.error),
                            failedDuring = FailedPhase.PATCHING
                        ))
                    }
                    is PatchingProgress.Cancelled -> {
                        log.i { "Patching cancelled" }
                        setPhase(PatcherPhase.Cancelled)
                    }
                }
            }
        }
    }

    private fun install() {
        val patchedPath = _uiState.value.patchedApkPath ?: return
        val appName = _uiState.value.app?.appName ?: "Patched App"

        log.d { "Starting installation for: $patchedPath" }
        currentJob?.cancel()
        setPhase(PatcherPhase.Installing(0, 100, "Checking installation..."))

        currentJob = viewModelScope.launch {
            // Run preflight check
            when (val result = installationUseCase.runPreflightCheck(patchedPath)) {
                is InstallationUseCase.PreflightResult.CanInstall -> {
                    performInstall(patchedPath, appName)
                }
                is InstallationUseCase.PreflightResult.RequiresUninstall -> {
                    log.d { "Preflight requires uninstall: ${result.reason}" }
                    setPhase(PatcherPhase.AwaitingUninstallConfirm(
                        packageName = result.packageName,
                        reason = result.reason
                    ))
                }
            }
        }
    }

    private suspend fun performInstall(path: String, appName: String) {
        setPhase(PatcherPhase.Installing(0, 100, "Preparing installation..."))

        installationUseCase.install(path, appName).collect { state ->
            when (state) {
                is InstallationState.Idle -> { }
                is InstallationState.Preparing -> {
                    setPhase(PatcherPhase.Installing(0, 100, "Preparing..."))
                }
                is InstallationState.Installing -> {
                    setPhase(PatcherPhase.Installing(state.progress, state.max))
                }
                is InstallationState.AwaitingConfirmation -> {
                    setPhase(PatcherPhase.Installing(50, 100, "Waiting for confirmation..."))
                }
                is InstallationState.Succeeded -> {
                    log.i { "Installation succeeded" }
                    setPhase(PatcherPhase.Installed)
                    _toastEvent.emit("Installation complete!")
                }
                is InstallationState.Failed -> {
                    log.e { "Installation failed: ${state.error.message}" }
                    handleInstallationError(state.error)
                }
                is InstallationState.Cancelled -> {
                    log.i { "Installation cancelled" }
                    setPhase(PatcherPhase.Cancelled)
                }
            }
        }
    }

    private fun handleInstallationError(error: InstallationError) {
        if (installationUseCase.requiresUninstall(error)) {
            val packageName = installationUseCase.getConflictingPackage(
                error,
                _uiState.value.app?.packageName
            )
            if (packageName != null) {
                setPhase(PatcherPhase.AwaitingUninstallConfirm(
                    packageName = packageName,
                    reason = error.title
                ))
                return
            }
        }

        setPhase(PatcherPhase.Failed(
            error = PhaseError.Installation(error),
            failedDuring = FailedPhase.INSTALL
        ))
    }

    private fun confirmUninstall() {
        val packageName = when (val phase = _uiState.value.phase) {
            is PatcherPhase.AwaitingUninstallConfirm -> phase.packageName
            else -> _uiState.value.app?.packageName
        } ?: return

        log.d { "Starting uninstall for: $packageName" }
        currentJob?.cancel()
        setPhase(PatcherPhase.Uninstalling(packageName))

        currentJob = viewModelScope.launch {
            installationUseCase.uninstall(packageName).collect { state ->
                when (state) {
                    is UninstallState.Idle -> { }
                    is UninstallState.Preparing -> { }
                    is UninstallState.Succeeded -> {
                        log.i { "Uninstall succeeded, will reinstall" }
                        performReinstallAfterUninstall(packageName)
                    }
                    is UninstallState.Failed -> {
                        log.e { "Uninstall failed: ${state.message}" }
                        setPhase(PatcherPhase.Failed(
                            error = PhaseError.Uninstall(state.cause ?: Exception(state.message)),
                            failedDuring = FailedPhase.UNINSTALL
                        ))
                        _toastEvent.emit("Uninstall failed: ${state.message}")
                    }
                    is UninstallState.Cancelled -> {
                        log.i { "Uninstall cancelled" }
                        // Go back to patched state since we still have the APK
                        val apkPath = _uiState.value.patchedApkPath
                        if (apkPath != null) {
                            setPhase(PatcherPhase.Patched(apkPath))
                        } else {
                            setPhase(PatcherPhase.Cancelled)
                        }
                    }
                }
            }
        }
    }

    private suspend fun performReinstallAfterUninstall(uninstalledPackage: String) {
        val patchedPath = _uiState.value.patchedApkPath ?: return
        val appName = _uiState.value.app?.appName ?: "Patched App"

        // Wait for package to be fully uninstalled
        setPhase(PatcherPhase.Installing(0, 100, "Waiting for system..."))

        val uninstalled = installationUseCase.waitForUninstall(uninstalledPackage)
        if (!uninstalled) {
            log.w { "Package may not be fully uninstalled" }
        }

        // Cleanup orphaned sessions
        setPhase(PatcherPhase.Installing(0, 100, "Cleaning up..."))
        installationUseCase.cleanupSessions()
        delay(300) // Brief delay for system to settle

        // Perform installation
        performInstall(patchedPath, appName)
    }

    private fun dismissUninstallDialog() {
        // Go back to patched state
        val apkPath = _uiState.value.patchedApkPath
        if (apkPath != null) {
            setPhase(PatcherPhase.Patched(apkPath))
        } else {
            setPhase(PatcherPhase.Ready)
        }
    }

    private fun cancel() {
        log.d { "Cancelling current operation" }
        currentJob?.cancel()
        currentJob = null

        // Also cancel the patcher use case in case it's running
        patcherUseCase.cancel()

        // Determine what state to go back to
        val apkPath = _uiState.value.patchedApkPath
        if (apkPath != null && _uiState.value.phase !is PatcherPhase.Patching) {
            // We have a patched APK, go back to that state
            setPhase(PatcherPhase.Patched(apkPath))
        } else {
            setPhase(PatcherPhase.Cancelled)
        }
    }

    private fun retry() {
        val phase = _uiState.value.phase
        if (phase is PatcherPhase.Failed) {
            when (phase.failedDuring) {
                FailedPhase.PATCHING -> startPatching()
                FailedPhase.INSTALL -> install()
                FailedPhase.UNINSTALL -> {
                    // Try to install directly since uninstall may have succeeded
                    install()
                }
            }
        } else if (phase is PatcherPhase.Cancelled) {
            // Retry from beginning or install if we have an APK
            if (_uiState.value.patchedApkPath != null) {
                install()
            } else {
                startPatching()
            }
        }
    }

    private fun setPhase(phase: PatcherPhase) {
        _uiState.update { it.copy(phase = phase) }
    }

    private fun copyError() {
        val errorLog = _uiState.value.error?.fullErrorLog ?: return

        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Error Log", errorLog)
        clipboard.setPrimaryClip(clip)

        viewModelScope.launch {
            _toastEvent.emit("Error copied to clipboard")
        }
    }

    private fun launchApp() {
        val packageName = _uiState.value.app?.packageName ?: return

        log.d { "Launching installed app: $packageName" }

        try {
            val launchIntent = application.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                application.startActivity(launchIntent)
            } else {
                log.w { "No launch intent found for $packageName" }
                viewModelScope.launch {
                    _toastEvent.emit("Cannot launch app")
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to launch app" }
            viewModelScope.launch {
                _toastEvent.emit("Failed to launch app")
            }
        }
    }
}