package com.navi.phantom.features.places.logic

import com.navi.phantom.domain.model.Place

data class PlacesUiState(
    val isLoading: Boolean = true,
    val places: List<Place> = emptyList(),
    val errorMessage: String? = null
)
