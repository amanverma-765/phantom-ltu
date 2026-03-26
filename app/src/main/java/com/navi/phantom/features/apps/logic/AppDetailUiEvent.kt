package com.navi.phantom.features.apps.logic

import com.navi.phantom.domain.model.Place

sealed interface AppDetailUiEvent {
    data class Load(val packageName: String) : AppDetailUiEvent
    data class AssignPlace(val packageName: String, val place: Place) : AppDetailUiEvent
    data class ClearLocation(val packageName: String) : AppDetailUiEvent
}
