package com.navi.phantom.features.map.logic

data class MapPickerUiState(
    val editingPlaceId: Long? = null,
    val selectedLatitude: Double? = null,
    val selectedLongitude: Double? = null,
    val currentLatitude: Double = 28.6139,
    val currentLongitude: Double = 77.2090,
    val placeName: String = "",
    val address: String? = null,
    val accuracy: Float? = null,
    val isLoadingAddress: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false
) {
    val isEditing: Boolean get() = editingPlaceId != null
    val hasSelection: Boolean get() = selectedLatitude != null && selectedLongitude != null
    val canSave: Boolean get() = hasSelection && placeName.isNotBlank() && !isSaving
}
