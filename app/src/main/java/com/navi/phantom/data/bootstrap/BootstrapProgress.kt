package com.navi.phantom.data.bootstrap

import com.navi.phantom.domain.errors.BootstrapError
import com.navi.phantom.features.bootstrap.logic.BootstrapStep

sealed interface BootstrapProgress {
    data class Step(val step: BootstrapStep, val message: String) : BootstrapProgress
    data class Completed(val outputPath: String) : BootstrapProgress
    data class Failed(val error: BootstrapError) : BootstrapProgress
    data object Cancelled : BootstrapProgress
}
