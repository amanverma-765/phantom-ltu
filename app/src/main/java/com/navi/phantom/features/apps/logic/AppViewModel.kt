package com.navi.phantom.features.apps.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.error.AppError
import com.navi.phantom.domain.model.PatchedApp
import com.navi.phantom.domain.usecase.DeviceAppUseCase
import com.navi.phantom.domain.usecase.PatchedAppUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val deviceAppUseCase: DeviceAppUseCase,
    private val patchedAppUseCase: PatchedAppUseCase
) : ViewModel() {
    private val log = Logger.withTag("AppViewModel")

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        // Start observing patched apps from database
        observePatchedApps()
    }

    fun onEvent(event: AppUiEvent) {
        when (event) {
            is AppUiEvent.GetAllDeviceApps -> getAllDeviceApps()
            is AppUiEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is AppUiEvent.DeletePatchedApp -> deletePatchedApp(event.patchedApp)
        }
    }

    private fun observePatchedApps() {
        viewModelScope.launch {
            patchedAppUseCase.observeAll().collect { patchedApps ->
                log.d { "Patched apps updated: ${patchedApps.size} apps" }
                _uiState.update { state ->
                    state.copy(patchedApps = patchedApps).recomputeFilteredLists()
                }
            }
        }
    }

    private fun deletePatchedApp(patchedApp: PatchedApp) {
        viewModelScope.launch {
            try {
                patchedAppUseCase.deleteById(patchedApp.id)
                log.i { "Deleted patched app: ${patchedApp.packageName}" }
            } catch (e: Exception) {
                log.e(e) { "Failed to delete patched app" }
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(searchQuery = query).recomputeFilteredLists()
        }
    }

    private fun getAllDeviceApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true, errorMessage = null) }
            deviceAppUseCase.getAllInstalledApps()
                .onSuccess { apps ->
                    _uiState.update { state ->
                        state.copy(
                            isLoadingApps = false,
                            errorMessage = null,
                            allDeviceApps = apps
                        ).recomputeFilteredLists()
                    }
                }
                .onFailure { throwable ->
                    log.e(throwable) { "getAllInstalledApps failed" }
                    val message = when (throwable) {
                        is SecurityException -> AppError.PermissionDenied.message
                        else -> AppError.Unknown.message
                    }
                    _uiState.update {
                        it.copy(
                            isLoadingApps = false,
                            errorMessage = message
                        )
                    }
                }
        }
    }

    /**
     * Recomputes both filtered lists based on current state.
     * - filteredPatchedApps: patched apps filtered by search query
     * - filteredDeviceApps: device apps filtered by search query, excluding patched packages
     */
    private fun AppUiState.recomputeFilteredLists(): AppUiState {
        val patchedPackageNames = patchedApps.map { it.packageName }.toSet()

        val filteredPatched = if (searchQuery.isBlank()) {
            patchedApps
        } else {
            patchedApps.filter { app ->
                app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }

        val filteredDevice = deviceAppUseCase.filterApps(allDeviceApps, searchQuery)
            .filter { it.packageName !in patchedPackageNames }

        return copy(
            filteredPatchedApps = filteredPatched,
            filteredDeviceApps = filteredDevice
        )
    }
}