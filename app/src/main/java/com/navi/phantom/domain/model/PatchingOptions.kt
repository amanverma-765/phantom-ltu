package com.navi.phantom.domain.model

data class PatchingOptions(
    val debuggable: Boolean = false,
    val sigbypassLevel: Int = 2,
    val overrideVersionCode: Boolean = false,
    val injectDex: Boolean = false,
    val managerApkPath: String? = null,
    val versionCodeOverride: Int? = null,
    val addedPermissions: List<String> = emptyList(),
    val managerPackageName: String? = null
)
