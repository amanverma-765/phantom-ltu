package com.navi.phantom.features.places.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.model.Place
import com.navi.phantom.domain.usecase.PlaceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlacesViewModel(
    private val placeUseCase: PlaceUseCase
) : ViewModel() {
    private val log = Logger.withTag("PlacesViewModel")

    private val _errorState = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<PlacesUiState> = placeUseCase.getAllPlaces()
        .onStart { _isLoading.value = true }
        .catch { error ->
            log.e(error) { "Failed to load places" }
            _errorState.value = "Failed to load places"
            _isLoading.value = false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
        .let { placesFlow ->
            combine(placesFlow, _errorState, _isLoading) { places, error, loading ->
                PlacesUiState(
                    isLoading = if (places.isNotEmpty() || error != null) false else loading,
                    places = places,
                    errorMessage = error
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PlacesUiState()
            )
        }

    fun onEvent(event: PlacesUiEvent) {
        when (event) {
            is PlacesUiEvent.LoadPlaces -> clearError()
            is PlacesUiEvent.DeletePlace -> deletePlace(event.place)
        }
    }

    private fun clearError() {
        _errorState.value = null
    }

    private fun deletePlace(place: Place) {
        viewModelScope.launch {
            placeUseCase.deletePlace(place)
                .onFailure { error ->
                    log.e(error) { "Failed to delete place" }
                    _errorState.value = "Failed to delete place"
                }
        }
    }
}
