package com.navi.phantom.features.apps.logic

import com.navi.phantom.domain.model.DeviceApp
import com.navi.phantom.domain.model.PatchedApp

data class AppUiState(
    val isLoadingApps: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val allDeviceApps: List<DeviceApp> = emptyList(),
    val patchedApps: List<PatchedApp> = emptyList(),
    // Pre-computed filtered lists (excludes patched from device apps)
    val filteredPatchedApps: List<PatchedApp> = emptyList(),
    val filteredDeviceApps: List<DeviceApp> = emptyList()
)