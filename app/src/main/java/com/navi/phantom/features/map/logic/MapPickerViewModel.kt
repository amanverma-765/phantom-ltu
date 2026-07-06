package com.navi.phantom.features.map.logic

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.navi.phantom.data.geocoding.CoordinateParser
import com.navi.phantom.data.geocoding.GeocodingService
import com.navi.phantom.data.geocoding.SearchSuggestion
import com.navi.phantom.data.routing.RoutingService
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
    private var searchJob: Job? = null
    private var routeJob: Job? = null
    private var mapController: MapController? = null

    fun onEvent(event: MapPickerUiEvent) {
        when (event) {
            is MapPickerUiEvent.LoadPlace -> loadPlace(event.placeId)
            is MapPickerUiEvent.UpdateCameraPosition -> updateCameraPosition(event.latitude, event.longitude)
            is MapPickerUiEvent.SelectLocation -> selectLocation(event.latitude, event.longitude)
            is MapPickerUiEvent.UpdatePlaceName -> updatePlaceName(event.name)
            is MapPickerUiEvent.SearchLocation -> searchLocation(event.query, event.field)
            is MapPickerUiEvent.SelectSearchResult -> selectSearchResult(event.placeId, event.field)
            is MapPickerUiEvent.ClearSearch -> clearSearch()
            is MapPickerUiEvent.ClearRoute -> clearRoute()
            is MapPickerUiEvent.SavePlace -> savePlace()
            is MapPickerUiEvent.ResetSaveSuccess -> resetSaveSuccess()
        }
    }

    fun setAccuracyFromGps(accuracy: Float?, latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(accuracy = accuracy, currentLatitude = latitude, currentLongitude = longitude)
        }
    }

    fun setMapController(controller: MapController) {
        mapController = controller
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
        _uiState.update { state ->
            // Clear accuracy if the user dragged away from the GPS position
            val clearAccuracy = state.accuracy != null &&
                (Math.abs(latitude - state.currentLatitude) > 0.0001 ||
                 Math.abs(longitude - state.currentLongitude) > 0.0001)
            state.copy(
                currentLatitude = latitude,
                currentLongitude = longitude,
                accuracy = if (clearAccuracy) null else state.accuracy
            )
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

    private fun searchLocation(query: String, field: RouteField) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                activeField = field,
                originQuery = if (field == RouteField.ORIGIN) query else it.originQuery,
                destQuery = if (field == RouteField.DEST) query else it.destQuery
            )
        }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchSuggestions = emptyList(), isSearching = false) }
            return
        }

        // Check for coordinates or Google Maps URL before hitting the API
        val parsed = CoordinateParser.parse(query)
        if (parsed != null && !parsed.needsResolve) {
            val suggestion = SearchSuggestion(
                placeId = "coord:${parsed.latitude},${parsed.longitude}",
                name = "%.5f, %.5f".format(parsed.latitude, parsed.longitude),
                description = "Go to coordinates"
            )
            _uiState.update { it.copy(searchSuggestions = listOf(suggestion), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchSuggestions = emptyList()) }

            // Short URL — resolve to coordinates inline, then show as suggestion
            if (parsed != null && parsed.needsResolve) {
                val geocoder = Geocoder(application, Locale.getDefault())
                val resolved = CoordinateParser.resolveShortUrl(parsed.originalUrl!!, geocoder)
                if (resolved != null) {
                    val suggestion = SearchSuggestion(
                        placeId = "coord:${resolved.latitude},${resolved.longitude}",
                        name = "%.5f, %.5f".format(resolved.latitude, resolved.longitude),
                        description = "From Google Maps link"
                    )
                    _uiState.update { it.copy(searchSuggestions = listOf(suggestion), isSearching = false) }
                } else {
                    _uiState.update {
                        it.copy(
                            searchSuggestions = listOf(
                                SearchSuggestion(
                                    placeId = "error",
                                    name = "Could not resolve link",
                                    description = "Try pasting the full Google Maps URL instead"
                                )
                            ),
                            isSearching = false
                        )
                    }
                }
                return@launch
            }

            delay(300) // debounce
            val state = _uiState.value
            val results = GeocodingService.searchLocation(
                query = query,
                lat = state.currentLatitude,
                lng = state.currentLongitude
            )
            _uiState.update { it.copy(searchSuggestions = results, isSearching = false) }
        }
    }

    private fun selectSearchResult(placeId: String, field: RouteField) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchSuggestions = emptyList()) }

            when {
                placeId.startsWith("coord:") -> {
                    val parts = placeId.removePrefix("coord:").split(",")
                    val lat = parts[0].toDouble()
                    val lng = parts[1].toDouble()
                    if (field == RouteField.ORIGIN) {
                        _uiState.update {
                            it.copy(
                                currentLatitude = lat,
                                currentLongitude = lng,
                                originLatLng = lat to lng,
                                isSearching = false,
                                navigateToSearchResult = true
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(destLatLng = lat to lng, isSearching = false)
                        }
                    }
                }

                placeId == "error" -> {
                    _uiState.update { it.copy(isSearching = false) }
                }

                else -> {
                    val details = GeocodingService.getPlaceDetails(placeId)
                    if (details != null) {
                        if (field == RouteField.ORIGIN) {
                            _uiState.update {
                                it.copy(
                                    currentLatitude = details.latitude,
                                    currentLongitude = details.longitude,
                                    address = details.address,
                                    originLatLng = details.latitude to details.longitude,
                                    isSearching = false,
                                    navigateToSearchResult = true
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    destLatLng = details.latitude to details.longitude,
                                    isSearching = false
                                )
                            }
                        }
                    } else {
                        _uiState.update { it.copy(isSearching = false) }
                    }
                }
            }

            maybeComputeRoute()
        }
    }

    private fun maybeComputeRoute() {
        val state = _uiState.value
        val origin = state.originLatLng
        val dest = state.destLatLng
        if (origin == null || dest == null) return

        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            val result = RoutingService.getRoute(origin.first, origin.second, dest.first, dest.second)
            if (result != null) {
                mapController?.drawRoute(result.points)
                _uiState.update {
                    it.copy(
                        routeDistanceText = result.distanceText,
                        routeDurationText = result.durationText
                    )
                }
            } else {
                log.d { "Route fetch failed for $origin -> $dest" }
            }
        }
    }

    private fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchSuggestions = emptyList(), isSearching = false, navigateToSearchResult = false) }
    }

    private fun clearRoute() {
        routeJob?.cancel()
        mapController?.clearRoute()
        _uiState.update {
            it.copy(
                originQuery = "",
                destQuery = "",
                originLatLng = null,
                destLatLng = null,
                routeDistanceText = null,
                routeDurationText = null
            )
        }
    }

    private fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
