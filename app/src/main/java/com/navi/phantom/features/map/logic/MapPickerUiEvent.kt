package com.navi.phantom.features.map.logic

sealed interface MapPickerUiEvent {
    data class LoadPlace(val placeId: Long) : MapPickerUiEvent
    data class UpdateCameraPosition(val latitude: Double, val longitude: Double) : MapPickerUiEvent
    data class SelectLocation(val latitude: Double, val longitude: Double) : MapPickerUiEvent
    data class UpdatePlaceName(val name: String) : MapPickerUiEvent
    data class SearchLocation(val query: String) : MapPickerUiEvent
    data class SelectSearchResult(val placeId: String) : MapPickerUiEvent
    data object ClearSearch : MapPickerUiEvent
    data object SavePlace : MapPickerUiEvent
    data object ResetSaveSuccess : MapPickerUiEvent
}
