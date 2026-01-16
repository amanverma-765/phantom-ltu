package com.navi.phantom.features.bootstrap.logic

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.usecase.BootstrapUseCase
import com.navi.phantom.domain.error.AppError
import com.navi.phantom.domain.error.InstallationError
import com.navi.phantom.domain.model.BootstrapOptions
import com.navi.phantom.domain.model.BootstrapProgress
import com.navi.phantom.domain.model.BootstrapStep
import com.navi.phantom.domain.model.InstallationState
import com.navi.phantom.domain.model.UninstallState
import com.navi.phantom.domain.usecase.InstalledAppUseCase
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

class BootstrapViewModel(
    private val application: Application,
    private val installedAppUseCase: InstalledAppUseCase,
    private val bootstrapUseCase: BootstrapUseCase,
    private val installationUseCase: InstallationUseCase
) : ViewModel() {
    
    private val log = Logger.withTag("BootstrapViewModel")

    private val _uiState = MutableStateFlow(BootstrapUiState())
    val uiState: StateFlow<BootstrapUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private var currentJob: Job? = null

    fun onEvent(event: BootstrapUiEvent) {
        when (event) {
            is BootstrapUiEvent.LoadApp -> loadApp(event.packageName)
            is BootstrapUiEvent.StartBootstrap -> startBootstrap()
            is BootstrapUiEvent.Install -> install()
            is BootstrapUiEvent.Cancel -> cancel()
            is BootstrapUiEvent.Retry -> retry()
            is BootstrapUiEvent.CopyError -> copyError()
            is BootstrapUiEvent.ConfirmUninstall -> confirmUninstall()
            is BootstrapUiEvent.DismissUninstallDialog -> dismissUninstallDialog()
            is BootstrapUiEvent.LaunchApp -> launchApp()
        }
    }

    private fun loadApp(packageName: String) {
        currentJob?.cancel()
        currentJob = null

        _uiState.update {
            BootstrapUiState(isLoadingApp = true)
        }

        viewModelScope.launch {
            installedAppUseCase.getAppDetails(packageName)
                .onSuccess { appInfo ->
                    _uiState.update {
                        it.copy(
                            app = appInfo,
                            isLoadingApp = false,
                            phase = BootstrapPhase.Ready
                        )
                    }
                }
                .onFailure { e ->
                    log.e(e) { "Failed to load app details" }
                    _uiState.update {
                        it.copy(
                            isLoadingApp = false,
                            phase = BootstrapPhase.Failed(
                                error = PhaseError.Generic(AppError.AppDetailsLoadFailed.message),
                                failedDuring = FailedPhase.BOOTSTRAP,
                                canRetry = false
                            )
                        )
                    }
                }
        }
    }

    private fun startBootstrap() {
        val app = _uiState.value.app ?: return
        
        currentJob?.cancel()
        setPhase(BootstrapPhase.Bootstrapping(BootstrapStep.PARSE_APK, "Initializing..."))

        currentJob = viewModelScope.launch {
            val splitApkPaths = bootstrapUseCase.getSplitApkPaths(app.packageName)
            log.d { "Starting bootstrap for ${app.packageName} with ${splitApkPaths.size} split APKs" }

            bootstrapUseCase.bootstrap(
                packageName = app.packageName,
                versionCode = app.versionCode,
                apkPath = app.apkPath,
                splitApkPaths = splitApkPaths,
                options = BootstrapOptions(sigbypassLevel = 1)
            ).collect { progress ->
                when (progress) {
                    is BootstrapProgress.Step -> {
                        setPhase(BootstrapPhase.Bootstrapping(progress.step, progress.message))
                    }
                    is BootstrapProgress.Completed -> {
                        log.i { "Bootstrap completed: ${progress.outputPath}" }
                        _uiState.update {
                            it.copy(
                                phase = BootstrapPhase.Bootstrapped(progress.outputPath),
                                bootstrappedApkPath = progress.outputPath
                            )
                        }
                    }
                    is BootstrapProgress.Failed -> {
                        log.e { "Bootstrap failed: ${progress.error.title}" }
                        setPhase(BootstrapPhase.Failed(
                            error = PhaseError.Bootstrap(progress.error),
                            failedDuring = FailedPhase.BOOTSTRAP
                        ))
                    }
                    is BootstrapProgress.Cancelled -> {
                        log.i { "Bootstrap cancelled" }
                        setPhase(BootstrapPhase.Cancelled)
                    }
                }
            }
        }
    }

    private fun install() {
        val bootstrappedPath = _uiState.value.bootstrappedApkPath ?: return
        val appName = _uiState.value.app?.appName ?: "Bootstrapped App"

        log.d { "Starting installation for: $bootstrappedPath" }
        currentJob?.cancel()
        setPhase(BootstrapPhase.Installing(0, 100, "Checking installation..."))

        currentJob = viewModelScope.launch {
            // Run preflight check
            when (val result = installationUseCase.runPreflightCheck(bootstrappedPath)) {
                is InstallationUseCase.PreflightResult.CanInstall -> {
                    performInstall(bootstrappedPath, appName)
                }
                is InstallationUseCase.PreflightResult.RequiresUninstall -> {
                    log.d { "Preflight requires uninstall: ${result.reason}" }
                    setPhase(BootstrapPhase.AwaitingUninstallConfirm(
                        packageName = result.packageName,
                        reason = result.reason
                    ))
                }
            }
        }
    }

    private suspend fun performInstall(path: String, appName: String) {
        setPhase(BootstrapPhase.Installing(0, 100, "Preparing installation..."))

        installationUseCase.install(path, appName).collect { state ->
            when (state) {
                is InstallationState.Idle -> { /* No-op */ }
                is InstallationState.Preparing -> {
                    setPhase(BootstrapPhase.Installing(0, 100, "Preparing..."))
                }
                is InstallationState.Installing -> {
                    setPhase(BootstrapPhase.Installing(state.progress, state.max))
                }
                is InstallationState.AwaitingConfirmation -> {
                    setPhase(BootstrapPhase.Installing(50, 100, "Waiting for confirmation..."))
                }
                is InstallationState.Succeeded -> {
                    log.i { "Installation succeeded" }
                    setPhase(BootstrapPhase.Installed)
                    _toastEvent.emit("Installation complete!")
                }
                is InstallationState.Failed -> {
                    log.e { "Installation failed: ${state.error.message}" }
                    handleInstallationError(state.error)
                }
                is InstallationState.Cancelled -> {
                    log.i { "Installation cancelled" }
                    setPhase(BootstrapPhase.Cancelled)
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
                setPhase(BootstrapPhase.AwaitingUninstallConfirm(
                    packageName = packageName,
                    reason = error.title
                ))
                return
            }
        }
        
        setPhase(BootstrapPhase.Failed(
            error = PhaseError.Installation(error),
            failedDuring = FailedPhase.INSTALL
        ))
    }

    private fun confirmUninstall() {
        val packageName = when (val phase = _uiState.value.phase) {
            is BootstrapPhase.AwaitingUninstallConfirm -> phase.packageName
            else -> _uiState.value.app?.packageName
        } ?: return

        log.d { "Starting uninstall for: $packageName" }
        currentJob?.cancel()
        setPhase(BootstrapPhase.Uninstalling(packageName, willReinstall = true))

        currentJob = viewModelScope.launch {
            installationUseCase.uninstall(packageName).collect { state ->
                when (state) {
                    is UninstallState.Idle -> { /* No-op */ }
                    is UninstallState.Preparing -> { /* Already showing Uninstalling */ }
                    is UninstallState.Succeeded -> {
                        log.i { "Uninstall succeeded, will reinstall" }
                        performReinstallAfterUninstall(packageName)
                    }
                    is UninstallState.Failed -> {
                        log.e { "Uninstall failed: ${state.message}" }
                        setPhase(BootstrapPhase.Failed(
                            error = PhaseError.Uninstall(state.cause ?: Exception(state.message)),
                            failedDuring = FailedPhase.UNINSTALL
                        ))
                        _toastEvent.emit("Uninstall failed: ${state.message}")
                    }
                    is UninstallState.Cancelled -> {
                        log.i { "Uninstall cancelled" }
                        // Go back to bootstrapped state since we still have the APK
                        val apkPath = _uiState.value.bootstrappedApkPath
                        if (apkPath != null) {
                            setPhase(BootstrapPhase.Bootstrapped(apkPath))
                        } else {
                            setPhase(BootstrapPhase.Cancelled)
                        }
                    }
                }
            }
        }
    }

    private suspend fun performReinstallAfterUninstall(uninstalledPackage: String) {
        val bootstrappedPath = _uiState.value.bootstrappedApkPath ?: return
        val appName = _uiState.value.app?.appName ?: "Bootstrapped App"

        // Wait for package to be fully uninstalled
        setPhase(BootstrapPhase.Installing(0, 100, "Waiting for system..."))
        
        val uninstalled = installationUseCase.waitForUninstall(uninstalledPackage)
        if (!uninstalled) {
            log.w { "Package may not be fully uninstalled" }
        }

        // Cleanup orphaned sessions
        setPhase(BootstrapPhase.Installing(0, 100, "Cleaning up..."))
        installationUseCase.cleanupSessions()
        delay(300) // Brief delay for system to settle

        // Perform installation
        performInstall(bootstrappedPath, appName)
    }

    private fun dismissUninstallDialog() {
        // Go back to bootstrapped state
        val apkPath = _uiState.value.bootstrappedApkPath
        if (apkPath != null) {
            setPhase(BootstrapPhase.Bootstrapped(apkPath))
        } else {
            setPhase(BootstrapPhase.Ready)
        }
    }

    private fun cancel() {
        log.d { "Cancelling current operation" }
        currentJob?.cancel()
        currentJob = null
        
        // Also cancel the bootstrap use case in case it's running
        bootstrapUseCase.cancel()

        // Determine what state to go back to
        val apkPath = _uiState.value.bootstrappedApkPath
        if (apkPath != null && _uiState.value.phase !is BootstrapPhase.Bootstrapping) {
            // We have a bootstrapped APK, go back to that state
            setPhase(BootstrapPhase.Bootstrapped(apkPath))
        } else {
            setPhase(BootstrapPhase.Cancelled)
        }
    }

    private fun retry() {
        val phase = _uiState.value.phase
        if (phase is BootstrapPhase.Failed) {
            when (phase.failedDuring) {
                FailedPhase.BOOTSTRAP -> startBootstrap()
                FailedPhase.INSTALL -> install()
                FailedPhase.UNINSTALL -> {
                    // Try to install directly since uninstall may have succeeded
                    install()
                }
            }
        } else if (phase is BootstrapPhase.Cancelled) {
            // Retry from beginning or install if we have an APK
            if (_uiState.value.bootstrappedApkPath != null) {
                install()
            } else {
                startBootstrap()
            }
        }
    }

    private fun setPhase(phase: BootstrapPhase) {
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