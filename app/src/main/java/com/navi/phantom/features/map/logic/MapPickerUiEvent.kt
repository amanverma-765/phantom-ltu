package com.navi.phantom.features.map.logic

sealed interface MapPickerUiEvent {
    data class SelectLocation(val latitude: Double, val longitude: Double) : MapPickerUiEvent
    data class UpdatePlaceName(val name: String) : MapPickerUiEvent
    data object SavePlace : MapPickerUiEvent
    data object ResetSaveSuccess : MapPickerUiEvent
}
