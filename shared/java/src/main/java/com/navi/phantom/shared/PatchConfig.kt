package com.navi.phantom.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class PatchConfig(
    val debuggable: Boolean,
    val overrideVersionCode: Boolean,
    val sigBypassLevel: Int,
    val originalSignature: String?,
    val appComponentFactory: String?,
    val managerApkPath: String? = null,
    val addedPermissions: List<String>? = null,
    val versionCodeOverride: Int? = null,
    val managerPackageName: String? = null,
    @Transient
    val lspConfig: LSPConfig = LSPConfig.instance
)