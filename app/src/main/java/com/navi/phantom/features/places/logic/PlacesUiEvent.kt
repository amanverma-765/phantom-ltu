package com.navi.phantom.features.places.logic

import com.navi.phantom.domain.model.Place

sealed interface PlacesUiEvent {
    data object LoadPlaces : PlacesUiEvent
    data class DeletePlace(val place: Place) : PlacesUiEvent
}
