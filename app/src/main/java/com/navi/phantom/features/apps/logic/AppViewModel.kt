package com.navi.phantom.features.apps.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.error.AppError
import com.navi.phantom.domain.model.DeviceApp
import com.navi.phantom.domain.usecase.ActiveLocationUseCase
import com.navi.phantom.domain.usecase.DeviceAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val deviceAppUseCase: DeviceAppUseCase,
    private val activeLocationUseCase: ActiveLocationUseCase
) : ViewModel() {
    private val log = Logger.withTag("AppViewModel")

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            activeLocationUseCase.getAllActiveLocations().collect { locations ->
                _uiState.update { state ->
                    state.copy(activeLocations = locations.associateBy { it.packageName })
                }
            }
        }
    }

    fun onEvent(event: AppUiEvent) {
        when (event) {
            is AppUiEvent.GetAllDeviceApps -> getAllDeviceApps()
            is AppUiEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
        }
    }

    private fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            delay(200)
            val state = _uiState.value
            val filtered = filterApps(state.allDeviceApps, query)
            val (patched, unpatched) = filtered.partition { it.isPatched }
            _uiState.update {
                it.copy(filteredPatchedApps = patched, filteredUnpatchedApps = unpatched)
            }
        }
    }

    private fun getAllDeviceApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true, errorMessage = null) }
            deviceAppUseCase.getAllInstalledApps()
                .onSuccess { apps ->
                    _uiState.update { state ->
                        val filtered = filterApps(apps, state.searchQuery)
                        val (patched, unpatched) = filtered.partition { it.isPatched }
                        state.copy(
                            isLoadingApps = false,
                            errorMessage = null,
                            allDeviceApps = apps,
                            filteredPatchedApps = patched,
                            filteredUnpatchedApps = unpatched
                        )
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

    private fun filterApps(apps: List<DeviceApp>, query: String): List<DeviceApp> =
        if (query.isBlank()) apps
        else apps.filter {
            it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
}