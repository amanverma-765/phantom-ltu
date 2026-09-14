package com.navi.phantom.features.apps.logic

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.error.AppError
import com.navi.phantom.domain.model.DeviceApp
import com.navi.phantom.domain.model.Place
import com.navi.phantom.domain.usecase.ActiveLocationUseCase
import com.navi.phantom.domain.usecase.DeviceAppUseCase
import com.navi.phantom.domain.usecase.PlaceUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val application: Application,
    private val deviceAppUseCase: DeviceAppUseCase,
    private val activeLocationUseCase: ActiveLocationUseCase,
    private val placeUseCase: PlaceUseCase
) : ViewModel() {
    private val log = Logger.withTag("AppViewModel")

    private val prefs by lazy {
        application.getSharedPreferences("phantom_app_locations", Context.MODE_PRIVATE)
    }

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
            is AppUiEvent.ToggleAppLocation -> toggleAppLocation(event.packageName, event.enable)
            is AppUiEvent.ClearUserMessage -> _uiState.update { it.copy(userMessage = null) }
        }
    }

    private fun toggleAppLocation(packageName: String, enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                // Toggled ON: select the last selected spoofed location or the last created place automatically
                val lastPlaceId = prefs.getLong("last_place_$packageName", -1L)
                var placeToAssign: Place? = null
                if (lastPlaceId != -1L) {
                    placeToAssign = placeUseCase.getPlaceById(lastPlaceId)
                }
                if (placeToAssign == null) {
                    // Fall back to the last created place (getAllPlaces is ordered by createdAt DESC)
                    val places = placeUseCase.getAllPlaces().firstOrNull() ?: emptyList()
                    placeToAssign = places.firstOrNull()
                }

                if (placeToAssign != null) {
                    prefs.edit().putLong("last_place_$packageName", placeToAssign.id).apply()
                    activeLocationUseCase.assignPlace(packageName, placeToAssign)
                        .onSuccess {
                            log.i { "Toggled ON: assigned ${placeToAssign.name} to $packageName" }
                        }
                        .onFailure {
                            log.e(it) { "Failed to assign place on toggle" }
                            _uiState.update { state -> state.copy(userMessage = "Failed to activate location") }
                        }
                } else {
                    _uiState.update { state ->
                        state.copy(userMessage = "No places saved yet. Please create a place first.")
                    }
                }
            } else {
                // Toggled OFF: remember current location placeId, then clear location to use real GPS
                val current = _uiState.value.activeLocations[packageName]
                if (current?.placeId != null) {
                    prefs.edit().putLong("last_place_$packageName", current.placeId).apply()
                }
                activeLocationUseCase.clearLocation(packageName)
                    .onSuccess {
                        log.i { "Toggled OFF: cleared location for $packageName (using real GPS)" }
                    }
                    .onFailure {
                        log.e(it) { "Failed to clear location on toggle" }
                    }
            }
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