package com.navi.phantom.data.bootstrap

data class BootstrapOptions(
    val debuggable: Boolean = false,
    val sigbypassLevel: Int = 1,
    val overrideVersionCode: Boolean = false
)
