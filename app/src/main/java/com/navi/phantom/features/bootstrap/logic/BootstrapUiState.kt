package com.navi.phantom.features.bootstrap.logic

import com.navi.phantom.domain.models.DetailedAppInfo

data class BootstrapUiState(
    val app: DetailedAppInfo? = null,
    val isLoading: Boolean = false,
    val isBootstrapping: Boolean = false,
    val isBootstrapped: Boolean = false,
    val hasBootstrapAttempted: Boolean = false,
    val statusMessage: String = "Ready to bootstrap",
    val bootstrappedApkPath: String? = null,
    val errorMessage: String? = null
)
