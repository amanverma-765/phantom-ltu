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

// Material 3 Easing Curves
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
private val StandardDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
private val StandardAccelerate = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

// Durations
private const val ENTER_DURATION = 400
private const val EXIT_DURATION = 200
private const val FADE_IN_DURATION = 250
private const val FADE_OUT_DURATION = 100

object NavAnimations {

    val forwardTransition
        get() = forwardEnter() togetherWith forwardExit()

    val backwardTransition
        get() = backwardEnter() togetherWith backwardExit()

    val predictiveBackTransition
        get() = predictiveEnter() togetherWith predictiveExit()

    // Forward Navigation
    private fun forwardEnter(): EnterTransition =
        slideInHorizontally(
            animationSpec = tween(ENTER_DURATION, easing = EmphasizedDecelerate),
            initialOffsetX = { (it * 0.15f).toInt() }
        ) + scaleIn(
            animationSpec = tween(ENTER_DURATION, easing = EmphasizedDecelerate),
            initialScale = 0.95f
        ) + fadeIn(
            animationSpec = tween(FADE_IN_DURATION, delayMillis = 60, easing = StandardDecelerate)
        )

    private fun forwardExit(): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(EXIT_DURATION, easing = EmphasizedAccelerate),
            targetOffsetX = { -(it * 0.05f).toInt() }
        ) + scaleOut(
            animationSpec = tween(EXIT_DURATION, easing = EmphasizedAccelerate),
            targetScale = 0.95f
        ) + fadeOut(
            animationSpec = tween(FADE_OUT_DURATION, easing = StandardAccelerate)
        )

    // Backward Navigation
    private fun backwardEnter(): EnterTransition =
        slideInHorizontally(
            animationSpec = tween(ENTER_DURATION, easing = EmphasizedDecelerate),
            initialOffsetX = { -(it * 0.05f).toInt() }
        ) + scaleIn(
            animationSpec = tween(ENTER_DURATION, easing = EmphasizedDecelerate),
            initialScale = 0.95f
        ) + fadeIn(
            animationSpec = tween(FADE_IN_DURATION, easing = StandardDecelerate)
        )

    private fun backwardExit(): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(EXIT_DURATION, easing = EmphasizedAccelerate),
            targetOffsetX = { (it * 0.15f).toInt() }
        ) + scaleOut(
            animationSpec = tween(EXIT_DURATION, easing = EmphasizedAccelerate),
            targetScale = 0.95f
        ) + fadeOut(
            animationSpec = tween(FADE_OUT_DURATION, easing = StandardAccelerate)
        )

    // Predictive Back (Gesture)
    private fun predictiveEnter(): EnterTransition =
        scaleIn(
            animationSpec = tween(350, easing = EmphasizedDecelerate),
            initialScale = 0.9f
        ) + fadeIn(
            animationSpec = tween(200, easing = StandardDecelerate)
        )

    private fun predictiveExit(): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(350, easing = EmphasizedAccelerate),
            targetOffsetX = { (it * 0.25f).toInt() }
        ) + scaleOut(
            animationSpec = tween(350, easing = EmphasizedAccelerate),
            targetScale = 0.85f
        ) + fadeOut(
            animationSpec = tween(150, easing = StandardAccelerate)
        )
}