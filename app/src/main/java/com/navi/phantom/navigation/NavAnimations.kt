package com.navi.phantom.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

// Material 3 Official Easing Curves
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

// Material 3 Duration Tokens
private const val DURATION_MEDIUM3 = 350  // Primary navigation duration
private const val DURATION_SHORT4 = 200   // Fade duration

object NavAnimations {

    val forwardTransition
        get() = forwardEnter() togetherWith forwardExit()

    val backwardTransition
        get() = backwardEnter() togetherWith backwardExit()

    val predictiveBackTransition
        get() = predictiveEnter() togetherWith predictiveExit()

    // Tab switching - scale only (no slide)
    val tabTransition
        get() = tabEnter() togetherWith tabExit()

    // Forward Navigation (push) - new screen slides in from right edge
    private fun forwardEnter(): EnterTransition =
        slideInHorizontally(
            animationSpec = tween(DURATION_MEDIUM3, easing = EmphasizedDecelerate),
            initialOffsetX = { it }  // Full width slide from right
        ) + fadeIn(
            animationSpec = tween(DURATION_SHORT4, easing = EmphasizedDecelerate)
        )

    private fun forwardExit(): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(DURATION_MEDIUM3, easing = EmphasizedAccelerate),
            targetOffsetX = { -it / 4 }  // Parallax: slides left 25%
        ) + fadeOut(
            animationSpec = tween(DURATION_SHORT4, easing = EmphasizedAccelerate)
        )

    // Backward Navigation (pop) - previous screen slides back from left
    private fun backwardEnter(): EnterTransition =
        slideInHorizontally(
            animationSpec = tween(DURATION_MEDIUM3, easing = EmphasizedDecelerate),
            initialOffsetX = { -it / 4 }  // Parallax: slides in from 25% left
        ) + fadeIn(
            animationSpec = tween(DURATION_SHORT4, easing = EmphasizedDecelerate)
        )

    private fun backwardExit(): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(DURATION_MEDIUM3, easing = EmphasizedAccelerate),
            targetOffsetX = { it }  // Full width slide to right
        ) + fadeOut(
            animationSpec = tween(DURATION_SHORT4, easing = EmphasizedAccelerate)
        )

    // Predictive Back (Android 14+ gesture) - scale + slide for tactile feedback
    private fun predictiveEnter(): EnterTransition =
        scaleIn(
            animationSpec = tween(DURATION_MEDIUM3, easing = EmphasizedDecelerate),
            initialScale = 0.9f
        ) + fadeIn(
            animationSpec = tween(DURATION_SHORT4, easing = EmphasizedDecelerate)
        )

    private fun predictiveExit(): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(DURATION_MEDIUM3, easing = EmphasizedAccelerate),
            targetOffsetX = { it }  // Full width slide to right
        ) + scaleOut(
            animationSpec = tween(DURATION_MEDIUM3, easing = EmphasizedAccelerate),
            targetScale = 0.9f
        ) + fadeOut(
            animationSpec = tween(DURATION_SHORT4, easing = EmphasizedAccelerate)
        )

    // Tab switching - subtle scale + crossfade (slower for smoothness)
    private fun tabEnter(): EnterTransition =
        scaleIn(
            animationSpec = tween(400, easing = EmphasizedDecelerate),
            initialScale = 0.96f
        ) + fadeIn(
            animationSpec = tween(300, easing = EmphasizedDecelerate)
        )

    private fun tabExit(): ExitTransition =
        scaleOut(
            animationSpec = tween(400, easing = EmphasizedAccelerate),
            targetScale = 0.96f
        ) + fadeOut(
            animationSpec = tween(300, easing = EmphasizedAccelerate)
        )
}