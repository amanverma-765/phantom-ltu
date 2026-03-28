package com.navi.phantom.features.map.logic

import com.navi.phantom.data.geocoding.SearchSuggestion

data class MapPickerUiState(
    val editingPlaceId: Long? = null,
    val selectedLatitude: Double? = null,
    val selectedLongitude: Double? = null,
    val currentLatitude: Double = DEFAULT_LATITUDE,
    val currentLongitude: Double = DEFAULT_LONGITUDE,
    val placeName: String = "",
    val address: String? = null,
    val accuracy: Float? = null,
    val isLoadingAddress: Boolean = false,
    val searchSuggestions: List<SearchSuggestion> = emptyList(),
    val isSearching: Boolean = false,
    val navigateToSearchResult: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false
) {
    val isEditing: Boolean get() = editingPlaceId != null
    val hasSelection: Boolean get() = selectedLatitude != null && selectedLongitude != null
    val canSave: Boolean get() = hasSelection && placeName.isNotBlank() && !isSaving

    companion object {
        const val DEFAULT_LATITUDE = 28.6127
        const val DEFAULT_LONGITUDE = 77.2373
    }
}
