package com.navi.phantom.data.bootstrap

data class BootstrapOptions(
    val debuggable: Boolean = false,
    val sigbypassLevel: Int = 2,
    val overrideVersionCode: Boolean = true,
    val injectDex: Boolean = false
)