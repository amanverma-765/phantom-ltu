package com.navi.phantom.shared

data class PatchConfig(
    val useManager: Boolean,
    val debuggable: Boolean,
    val overrideVersionCode: Boolean,
    val sigBypassLevel: Int,
    val originalSignature: String,
    val appComponentFactory: String?,
    val lspConfig: LSPConfig = LSPConfig.instance
)