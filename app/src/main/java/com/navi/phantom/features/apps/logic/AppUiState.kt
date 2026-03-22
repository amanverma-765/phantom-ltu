package com.navi.phantom.features.apps.logic

import com.navi.phantom.domain.model.ActiveLocation
import com.navi.phantom.domain.model.DeviceApp

data class AppUiState(
    val isLoadingApps: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val allDeviceApps: List<DeviceApp> = emptyList(),
    val filteredPatchedApps: List<DeviceApp> = emptyList(),
    val filteredUnpatchedApps: List<DeviceApp> = emptyList(),
    val activeLocations: Map<String, ActiveLocation> = emptyMap()
)