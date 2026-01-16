package com.navi.phantom.domain.model

data class PatchingOptions(
    val debuggable: Boolean = false,
    val sigbypassLevel: Int = 2,
    val overrideVersionCode: Boolean = false,
    val injectDex: Boolean = false
)
