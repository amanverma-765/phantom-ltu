package com.navi.phantom.features.apps.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.model.ActiveLocation
import com.navi.phantom.domain.model.DeviceApp
import com.navi.phantom.domain.model.Place
import com.navi.phantom.domain.usecase.ActiveLocationUseCase
import com.navi.phantom.domain.usecase.DeviceAppUseCase
import com.navi.phantom.domain.usecase.PlaceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppDetailUiState(
    val app: DeviceApp? = null,
    val activeLocation: ActiveLocation? = null,
    val places: List<Place> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface AppDetailUiEvent {
    data class Load(val packageName: String) : AppDetailUiEvent
    data class AssignPlace(val packageName: String, val place: Place) : AppDetailUiEvent
    data class ClearLocation(val packageName: String) : AppDetailUiEvent
}

class AppDetailViewModel(
    private val deviceAppUseCase: DeviceAppUseCase,
    private val activeLocationUseCase: ActiveLocationUseCase,
    private val placeUseCase: PlaceUseCase
) : ViewModel() {

    private val log = Logger.withTag("AppDetailViewModel")

    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    fun onEvent(event: AppDetailUiEvent) {
        when (event) {
            is AppDetailUiEvent.Load -> load(event.packageName)
            is AppDetailUiEvent.AssignPlace -> assignPlace(event.packageName, event.place)
            is AppDetailUiEvent.ClearLocation -> clearLocation(event.packageName)
        }
    }

    private fun load(packageName: String) {
        viewModelScope.launch {
            deviceAppUseCase.getAppByPackageName(packageName)
                .onSuccess { app ->
                    _uiState.update { it.copy(app = app) }
                }
                .onFailure { error ->
                    log.e(error) { "Failed to load app: $packageName" }
                }
        }

        viewModelScope.launch {
            combine(
                placeUseCase.getAllPlaces(),
                activeLocationUseCase.getActiveLocation(packageName)
            ) { places, activeLocation ->
                Pair(places, activeLocation)
            }.collect { (places, location) ->
                _uiState.update {
                    it.copy(
                        places = places,
                        activeLocation = location,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun assignPlace(packageName: String, place: Place) {
        viewModelScope.launch {
            activeLocationUseCase.assignPlace(packageName, place)
                .onSuccess { log.i { "Assigned ${place.name} to $packageName" } }
                .onFailure { log.e(it) { "Failed to assign place" } }
        }
    }

    private fun clearLocation(packageName: String) {
        viewModelScope.launch {
            activeLocationUseCase.clearLocation(packageName)
                .onSuccess { log.i { "Cleared location for $packageName" } }
                .onFailure { log.e(it) { "Failed to clear location" } }
        }
    }
}
