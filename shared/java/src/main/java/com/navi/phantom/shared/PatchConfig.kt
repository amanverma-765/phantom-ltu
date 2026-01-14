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
    @Transient
    val lspConfig: LSPConfig = LSPConfig.instance
)