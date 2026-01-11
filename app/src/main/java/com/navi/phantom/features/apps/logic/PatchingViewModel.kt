package com.navi.phantom.features.apps.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navi.phantom.domain.repository.InstalledAppRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import co.touchlab.kermit.Logger

class PatchingViewModel(
    private val repository: InstalledAppRepository
) : ViewModel() {
    private val log = Logger.withTag("PatchingViewModel")

    private val _uiState = MutableStateFlow(PatchingUiState())
    val uiState: StateFlow<PatchingUiState> = _uiState.asStateFlow()

    fun onEvent(event: PatchingUiEvent) {
        when (event) {
            is PatchingUiEvent.LoadApp -> loadApp(event.packageName)
            is PatchingUiEvent.StartPatching -> startPatching()
            is PatchingUiEvent.InstallPatchedApp -> installPatchedApp()
            is PatchingUiEvent.CancelPatching -> cancelPatching()
        }
    }

    private fun loadApp(packageName: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            repository.getAppDetails(packageName)
                .onSuccess { appInfo ->
                    _uiState.update {
                        it.copy(
                            app = appInfo,
                            isLoading = false,
                            isPatching = false,
                            isPatchComplete = false,
                            hasPatchingAttempted = false,
                            statusMessage = "Ready to patch",
                            patchedApkPath = null,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { e ->
                    log.e(e) { "Failed to load app details" }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load app: ${e.message}"
                        )
                    }
                }
        }
    }

    private fun startPatching() {
        val app = _uiState.value.app ?: return

        _uiState.update {
            it.copy(
                isPatching = true,
                isPatchComplete = false,
                hasPatchingAttempted = true,
                statusMessage = "Initializing patcher...",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                // TODO: Replace with actual patching logic
                simulatePatching()
            } catch (e: Exception) {
                log.e(e) { "Patching failed" }
                _uiState.update {
                    it.copy(
                        isPatching = false,
                        errorMessage = "Patching failed: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun simulatePatching() {
        val steps = listOf(
            "Reading APK structure...",
            "Extracting classes.dex...",
            "Injecting location hooks...",
            "Modifying manifest...",
            "Rebuilding APK...",
            "Signing APK...",
            "Patch complete!"
        )

        steps.forEachIndexed { index, message ->
            delay(800)
            _uiState.update { it.copy(statusMessage = message) }

            if (index == steps.lastIndex) {
                _uiState.update {
                    it.copy(
                        isPatching = false,
                        isPatchComplete = true,
                        patchedApkPath = "/path/to/patched.apk" // TODO: actual path
                    )
                }
            }
        }
    }

    private fun installPatchedApp() {
        val patchedPath = _uiState.value.patchedApkPath ?: return
        log.d { "Installing patched APK from: $patchedPath" }
        // TODO: Implement APK installation via PackageInstaller
    }

    private fun cancelPatching() {
        // TODO: Cancel ongoing patching operation
        _uiState.update {
            it.copy(
                isPatching = false,
                statusMessage = "Patching cancelled"
            )
        }
    }
}
