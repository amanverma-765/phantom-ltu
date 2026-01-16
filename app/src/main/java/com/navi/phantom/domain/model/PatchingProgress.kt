package com.navi.phantom.domain.model

import com.navi.phantom.domain.error.PatchingError

sealed interface PatchingProgress {
    data class Step(val step: PatchingStep, val message: String) : PatchingProgress
    data class Completed(val outputPath: String) : PatchingProgress
    data class Failed(val error: PatchingError) : PatchingProgress
    data object Cancelled : PatchingProgress
}
