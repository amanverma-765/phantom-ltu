package com.navi.phantom.features.map.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.usecase.PlaceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapPickerViewModel(
    private val placeUseCase: PlaceUseCase
) : ViewModel() {
    private val log = Logger.withTag("MapPickerViewModel")

    private val _uiState = MutableStateFlow(MapPickerUiState())
    val uiState: StateFlow<MapPickerUiState> = _uiState.asStateFlow()

    fun onEvent(event: MapPickerUiEvent) {
        when (event) {
            is MapPickerUiEvent.SelectLocation -> selectLocation(event.latitude, event.longitude)
            is MapPickerUiEvent.UpdatePlaceName -> updatePlaceName(event.name)
            is MapPickerUiEvent.SavePlace -> savePlace()
            is MapPickerUiEvent.ResetSaveSuccess -> resetSaveSuccess()
        }
    }

    private fun selectLocation(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(selectedLatitude = latitude, selectedLongitude = longitude)
        }
    }

    private fun updatePlaceName(name: String) {
        _uiState.update { it.copy(placeName = name) }
    }

    private fun savePlace() {
        val state = _uiState.value
        if (!state.canSave) return

        val latitude = state.selectedLatitude ?: return
        val longitude = state.selectedLongitude ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }

            placeUseCase.savePlace(
                name = state.placeName,
                latitude = latitude,
                longitude = longitude
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { error ->
                log.e(error) { "Failed to save place" }
                _uiState.update {
                    it.copy(isSaving = false, saveError = "Failed to save place")
                }
            }
        }
    }

    private fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
