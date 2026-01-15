package com.navi.phantom.features.apps.logic

import com.navi.phantom.domain.models.InstalledApp

data class AppUiState(
    val isLoadingApps: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val allInstalledApps: List<InstalledApp> = emptyList(),
    val filteredApps: List<InstalledApp> = emptyList()
)