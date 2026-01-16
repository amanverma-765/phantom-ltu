package com.navi.phantom.domain.model

import com.navi.phantom.domain.error.BootstrapError

sealed interface BootstrapProgress {
    data class Step(val step: BootstrapStep, val message: String) : BootstrapProgress
    data class Completed(val outputPath: String) : BootstrapProgress
    data class Failed(val error: BootstrapError) : BootstrapProgress
    data object Cancelled : BootstrapProgress
}
