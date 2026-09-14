package com.navi.phantom.features.apps.logic

import com.navi.phantom.domain.model.ActiveLocation
import com.navi.phantom.domain.model.DeviceApp

data class AppUiState(
    val isLoadingApps: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val allDeviceApps: List<DeviceApp> = emptyList(),
    val filteredPatchedApps: List<DeviceApp> = emptyList(),
    val filteredUnpatchedApps: List<DeviceApp> = emptyList(),
    val activeLocations: Map<String, ActiveLocation> = emptyMap(),
    val userMessage: String? = null
)