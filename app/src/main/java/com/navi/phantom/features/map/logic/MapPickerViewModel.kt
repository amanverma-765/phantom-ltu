package com.navi.phantom.features.map.logic

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.navi.phantom.domain.model.Place
import com.navi.phantom.domain.usecase.PlaceUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MapPickerViewModel(
    private val application: Application,
    private val placeUseCase: PlaceUseCase
) : ViewModel() {
    private val log = Logger.withTag("MapPickerViewModel")

    private val _uiState = MutableStateFlow(MapPickerUiState())
    val uiState: StateFlow<MapPickerUiState> = _uiState.asStateFlow()

    private var geocodeJob: Job? = null

    fun onEvent(event: MapPickerUiEvent) {
        when (event) {
            is MapPickerUiEvent.LoadPlace -> loadPlace(event.placeId)
            is MapPickerUiEvent.UpdateCameraPosition -> updateCameraPosition(event.latitude, event.longitude)
            is MapPickerUiEvent.SelectLocation -> selectLocation(event.latitude, event.longitude)
            is MapPickerUiEvent.UpdatePlaceName -> updatePlaceName(event.name)
            is MapPickerUiEvent.SavePlace -> savePlace()
            is MapPickerUiEvent.ResetSaveSuccess -> resetSaveSuccess()
        }
    }

    fun setAccuracyFromGps(accuracy: Float?) {
        _uiState.update { it.copy(accuracy = accuracy) }
    }

    private fun loadPlace(placeId: Long) {
        viewModelScope.launch {
            val place = placeUseCase.getPlaceById(placeId) ?: return@launch
            _uiState.update {
                it.copy(
                    editingPlaceId = place.id,
                    currentLatitude = place.latitude,
                    currentLongitude = place.longitude,
                    selectedLatitude = place.latitude,
                    selectedLongitude = place.longitude,
                    placeName = place.name,
                    address = place.address,
                    accuracy = place.accuracy
                )
            }
        }
    }

    private fun updateCameraPosition(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(currentLatitude = latitude, currentLongitude = longitude)
        }
        reverseGeocode(latitude, longitude)
    }

    private fun reverseGeocode(latitude: Double, longitude: Double) {
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAddress = true) }
            delay(300) // debounce
            try {
                val address = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    val results = Geocoder(application, Locale.getDefault())
                        .getFromLocation(latitude, longitude, 1)
                    results?.firstOrNull()?.getAddressLine(0)
                }
                _uiState.update { it.copy(address = address, isLoadingAddress = false) }
            } catch (e: Exception) {
                log.d(e) { "Geocoding failed" }
                _uiState.update { it.copy(address = null, isLoadingAddress = false) }
            }
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

            val result = if (state.isEditing) {
                placeUseCase.updatePlace(
                    Place(
                        id = state.editingPlaceId!!,
                        name = state.placeName,
                        latitude = latitude,
                        longitude = longitude,
                        address = state.address,
                        accuracy = state.accuracy
                    )
                ).map { state.editingPlaceId }
            } else {
                placeUseCase.savePlace(
                    name = state.placeName,
                    latitude = latitude,
                    longitude = longitude,
                    address = state.address,
                    accuracy = state.accuracy
                )
            }

            result.onSuccess {
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
