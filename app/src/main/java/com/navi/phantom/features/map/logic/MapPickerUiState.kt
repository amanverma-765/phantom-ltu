package com.navi.phantom.features.map.logic

data class MapPickerUiState(
    val selectedLatitude: Double? = null,
    val selectedLongitude: Double? = null,
    val placeName: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false
) {
    val hasSelection: Boolean get() = selectedLatitude != null && selectedLongitude != null
    val canSave: Boolean get() = hasSelection && placeName.isNotBlank() && !isSaving
}
