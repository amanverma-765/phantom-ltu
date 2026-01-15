package com.navi.phantom.features.bootstrap.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navi.phantom.domain.errors.AppError
import com.navi.phantom.domain.repository.InstalledAppRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import co.touchlab.kermit.Logger

class BootstrapViewModel(
    private val repository: InstalledAppRepository
) : ViewModel() {
    private val log = Logger.withTag("BootstrapViewModel")

    private val _uiState = MutableStateFlow(BootstrapUiState())
    val uiState: StateFlow<BootstrapUiState> = _uiState.asStateFlow()

    fun onEvent(event: BootstrapUiEvent) {
        when (event) {
            is BootstrapUiEvent.LoadApp -> loadApp(event.packageName)
            is BootstrapUiEvent.StartBootstrap -> startBootstrap()
            is BootstrapUiEvent.InstallBootstrappedApp -> installBootstrappedApp()
            is BootstrapUiEvent.CancelBootstrap -> cancelBootstrap()
        }
    }

    private fun loadApp(packageName: String) {
        _uiState.update {
            it.copy(
                isLoadingAppDetails = true,
                errorMessage = null
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
                            statusMessage = "Ready to bootstrap",
                            bootstrappedApkPath = null,
                            errorMessage = null
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
                statusMessage = "Initializing...",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                // TODO: Replace with actual bootstrap logic
                simulateBootstrap()
            } catch (e: Exception) {
                log.e(e) { "Bootstrap failed" }
                _uiState.update {
                    it.copy(
                        isBootstrapping = false,
                        errorMessage = AppError.BootstrapFailed.message
                    )
                }
            }
        }
    }

    private suspend fun simulateBootstrap() {
        val steps = listOf(
            "Reading APK structure...",
            "Extracting classes.dex...",
            "Injecting location hooks...",
            "Modifying manifest...",
            "Rebuilding APK...",
            "Signing APK...",
            "Bootstrapped!"
        )

        steps.forEachIndexed { index, message ->
            delay(800)
            _uiState.update { it.copy(statusMessage = message) }

            if (index == steps.lastIndex) {
                _uiState.update {
                    it.copy(
                        isBootstrapping = false,
                        isBootstrapped = true,
                        bootstrappedApkPath = "/path/to/bootstrapped.apk" // TODO: actual path
                    )
                }
            }
        }
    }

    private fun installBootstrappedApp() {
        val bootstrappedPath = _uiState.value.bootstrappedApkPath ?: return
        log.d { "Installing bootstrapped APK from: $bootstrappedPath" }
        // TODO: Implement APK installation via PackageInstaller
    }

    private fun cancelBootstrap() {
        // TODO: Cancel ongoing bootstrap operation
        _uiState.update {
            it.copy(
                isBootstrapping = false,
                statusMessage = "Cancelled",
                errorMessage = null
            )
        }
    }
}
