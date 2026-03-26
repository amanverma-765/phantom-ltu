package com.navi.phantom.features.apps.logic

import com.navi.phantom.domain.model.ActiveLocation
import com.navi.phantom.domain.model.DeviceApp
import com.navi.phantom.domain.model.Place

data class AppDetailUiState(
    val app: DeviceApp? = null,
    val activeLocation: ActiveLocation? = null,
    val places: List<Place> = emptyList(),
    val isLoading: Boolean = true
)
