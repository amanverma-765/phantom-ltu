package com.navi.phantom.features.bootstrap.logic

import com.navi.phantom.domain.errors.BootstrapError
import com.navi.phantom.domain.models.DetailedAppInfo

data class BootstrapUiState(
    val app: DetailedAppInfo? = null,
    val isLoadingAppDetails: Boolean = false,
    val isBootstrapping: Boolean = false,
    val isBootstrapped: Boolean = false,
    val hasBootstrapAttempted: Boolean = false,
    val statusMessage: String = "Ready to bootstrap",
    val currentStep: BootstrapStep? = null,
    val bootstrappedApkPath: String? = null,
    val errorMessage: String? = null,
    val errorDetails: BootstrapError? = null
) {
    val hasCopyableError: Boolean
        get() = errorDetails != null
}
