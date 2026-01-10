package com.navi.phantom.features.apps.logic

import com.navi.phantom.domain.models.DetailedAppInfo

data class PatchingUiState(
    val app: DetailedAppInfo? = null,
    val isLoading: Boolean = false,
    val isPatching: Boolean = false,
    val isPatchComplete: Boolean = false,
    val hasPatchingAttempted: Boolean = false,
    val statusMessage: String = "Ready to patch",
    val patchedApkPath: String? = null,
    val errorMessage: String? = null
)
