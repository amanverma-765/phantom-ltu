package com.navi.phantom.features.places.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.model.ActiveLocation
import com.navi.phantom.domain.model.Place
import com.navi.phantom.domain.usecase.ActiveLocationUseCase
import com.navi.phantom.domain.usecase.PlaceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaceSelectionUiState(
    val places: List<Place> = emptyList(),
    val currentActiveLocation: ActiveLocation? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

sealed interface PlaceSelectionUiEvent {
    data class LoadPlaces(val packageName: String) : PlaceSelectionUiEvent
    data class AssignPlace(val packageName: String, val place: Place) : PlaceSelectionUiEvent
    data class ClearLocation(val packageName: String) : PlaceSelectionUiEvent
}

class PlaceSelectionViewModel(
    private val placeUseCase: PlaceUseCase,
    private val activeLocationUseCase: ActiveLocationUseCase
) : ViewModel() {

    private val log = Logger.withTag("PlaceSelectionViewModel")

    private val _uiState = MutableStateFlow(PlaceSelectionUiState())
    val uiState: StateFlow<PlaceSelectionUiState> = _uiState.asStateFlow()

    fun onEvent(event: PlaceSelectionUiEvent) {
        when (event) {
            is PlaceSelectionUiEvent.LoadPlaces -> loadPlaces(event.packageName)
            is PlaceSelectionUiEvent.AssignPlace -> assignPlace(event.packageName, event.place)
            is PlaceSelectionUiEvent.ClearLocation -> clearLocation(event.packageName)
        }
    }

    private fun loadPlaces(packageName: String) {
        viewModelScope.launch {
            combine(
                placeUseCase.getAllPlaces(),
                activeLocationUseCase.getActiveLocation(packageName)
            ) { places, activeLocation ->
                PlaceSelectionUiState(
                    places = places,
                    currentActiveLocation = activeLocation,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun assignPlace(packageName: String, place: Place) {
        viewModelScope.launch {
            activeLocationUseCase.assignPlace(packageName, place)
                .onSuccess {
                    log.i { "Assigned ${place.name} to $packageName" }
                    _uiState.update { it.copy(isSaved = true) }
                }
                .onFailure { error ->
                    log.e(error) { "Failed to assign place" }
                    _uiState.update { it.copy(errorMessage = "Failed to assign location") }
                }
        }
    }

    private fun clearLocation(packageName: String) {
        viewModelScope.launch {
            activeLocationUseCase.clearLocation(packageName)
                .onSuccess {
                    log.i { "Cleared location for $packageName" }
                    _uiState.update { it.copy(isSaved = true) }
                }
                .onFailure { error ->
                    log.e(error) { "Failed to clear location" }
                    _uiState.update { it.copy(errorMessage = "Failed to clear location") }
                }
        }
    }
}
