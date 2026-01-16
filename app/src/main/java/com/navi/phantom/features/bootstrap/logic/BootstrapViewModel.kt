package com.navi.phantom.features.bootstrap.logic

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.navi.phantom.data.bootstrap.ApkInstaller
import com.navi.phantom.data.bootstrap.BootstrapEngine
import com.navi.phantom.data.bootstrap.BootstrapOptions
import com.navi.phantom.data.bootstrap.BootstrapProgress
import com.navi.phantom.domain.errors.AppError
import com.navi.phantom.domain.repository.InstalledAppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BootstrapViewModel(
    private val application: Application,
    private val repository: InstalledAppRepository,
    private val bootstrapEngine: BootstrapEngine,
    private val apkInstaller: ApkInstaller
) : ViewModel() {
    private val log = Logger.withTag("BootstrapViewModel")

    private val _uiState = MutableStateFlow(BootstrapUiState())
    val uiState: StateFlow<BootstrapUiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private var bootstrapJob: Job? = null

    fun onEvent(event: BootstrapUiEvent) {
        when (event) {
            is BootstrapUiEvent.LoadApp -> loadApp(event.packageName)
            is BootstrapUiEvent.StartBootstrap -> startBootstrap()
            is BootstrapUiEvent.InstallBootstrappedApp -> installBootstrappedApp()
            is BootstrapUiEvent.CancelBootstrap -> cancelBootstrap()
            is BootstrapUiEvent.CopyErrorLog -> copyErrorLog()
            is BootstrapUiEvent.DismissError -> dismissError()
        }
    }

    private fun loadApp(packageName: String) {
        _uiState.update {
            it.copy(
                isLoadingAppDetails = true,
                errorMessage = null,
                errorDetails = null
            )
        }

        viewModelScope.launch {
            repository.getAppDetails(packageName)
                .onSuccess { appInfo ->
                    _uiState.update {
                        it.copy(
                            app = appInfo,
                            isLoadingAppDetails = false,
                            isBootstrapping = false,
                            isBootstrapped = false,
                            hasBootstrapAttempted = false,
                            currentStep = null,
                            statusMessage = "Ready to bootstrap",
                            bootstrappedApkPath = null,
                            errorMessage = null,
                            errorDetails = null
                        )
                    }
                }
                .onFailure { e ->
                    log.e(e) { "Failed to load app details" }
                    _uiState.update {
                        it.copy(
                            isLoadingAppDetails = false,
                            errorMessage = AppError.AppDetailsLoadFailed.message
                        )
                    }
                }
        }
    }

    private fun startBootstrap() {
        val app = _uiState.value.app ?: return

        _uiState.update {
            it.copy(
                isBootstrapping = true,
                isBootstrapped = false,
                hasBootstrapAttempted = true,
                currentStep = null,
                statusMessage = "Initializing...",
                errorMessage = null,
                errorDetails = null
            )
        }

        bootstrapJob = viewModelScope.launch {
            val splitApkPaths = getSplitApkPaths(app.packageName)
            log.d { "Starting bootstrap for ${app.packageName} with ${splitApkPaths.size} split APKs" }

            bootstrapEngine.bootstrap(
                packageName = app.packageName,
                versionCode = app.versionCode,
                apkPath = app.apkPath,
                splitApkPaths = splitApkPaths,
                options = BootstrapOptions(sigbypassLevel = 1)
            ).collect { progress ->
                handleProgress(progress)
            }
        }
    }

    private fun handleProgress(progress: BootstrapProgress) {
        when (progress) {
            is BootstrapProgress.Step -> {
                _uiState.update {
                    it.copy(
                        currentStep = progress.step,
                        statusMessage = "${progress.step.title}..."
                    )
                }
            }
            is BootstrapProgress.Completed -> {
                log.i { "Bootstrap completed: ${progress.outputPath}" }
                _uiState.update {
                    it.copy(
                        isBootstrapping = false,
                        isBootstrapped = true,
                        currentStep = BootstrapStep.COMPLETE,
                        statusMessage = "Bootstrapped!",
                        bootstrappedApkPath = progress.outputPath
                    )
                }
            }
            is BootstrapProgress.Failed -> {
                val error = progress.error
                log.e { "Bootstrap failed: ${error.title} - ${error.message}" }
                error.cause?.let { log.e(it) { "Cause" } }

                _uiState.update {
                    it.copy(
                        isBootstrapping = false,
                        statusMessage = "Failed",
                        errorMessage = error.message,
                        errorDetails = error
                    )
                }
            }
            is BootstrapProgress.Cancelled -> {
                log.i { "Bootstrap cancelled" }
                _uiState.update {
                    it.copy(
                        isBootstrapping = false,
                        statusMessage = "Cancelled"
                    )
                }
            }
        }
    }

    private fun installBootstrappedApp() {
        val bootstrappedPath = _uiState.value.bootstrappedApkPath ?: return
        log.d { "Installing bootstrapped APK from: $bootstrappedPath" }

        viewModelScope.launch {
            apkInstaller.install(bootstrappedPath)
                .onFailure { e ->
                    log.e(e) { "Installation failed" }
                    _uiState.update {
                        it.copy(errorMessage = "Installation failed: ${e.message}")
                    }
                }
        }
    }

    private fun cancelBootstrap() {
        log.d { "Cancelling bootstrap" }
        bootstrapJob?.cancel()
        bootstrapEngine.cancel()
        _uiState.update {
            it.copy(
                isBootstrapping = false,
                statusMessage = "Cancelled",
                errorMessage = null,
                errorDetails = null
            )
        }
    }

    private fun copyErrorLog() {
        val state = _uiState.value
        val errorLog = state.errorDetails?.fullErrorLog ?: state.errorMessage ?: return

        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Error Log", errorLog)
        clipboard.setPrimaryClip(clip)

        viewModelScope.launch {
            _toastEvent.emit("Error copied to clipboard")
        }
    }

    private fun dismissError() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                errorDetails = null
            )
        }
    }

    private suspend fun getSplitApkPaths(packageName: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val appInfo = application.packageManager.getApplicationInfo(packageName, 0)
                appInfo.splitSourceDirs?.toList() ?: emptyList()
            } catch (e: Exception) {
                log.w(e) { "Could not get split APK paths" }
                emptyList()
            }
        }
    }
}
