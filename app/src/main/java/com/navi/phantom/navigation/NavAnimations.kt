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

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

private const val DURATION_MEDIUM3 = 350
private const val DURATION_SHORT4 = 200

object NavAnimations {

    val forwardTransition
        get() = forwardEnter() togetherWith forwardExit()

    val backwardTransition
        get() = backwardEnter() togetherWith backwardExit()

    val predictiveBackTransition
        get() = predictiveEnter() togetherWith predictiveExit()

    val tabTransition
        get() = tabEnter() togetherWith tabExit()

    private fun forwardEnter(): EnterTransition =
        slideInHorizontally(
            animationSpec = tween(DURATION_MEDIUM3, easing = EmphasizedDecelerate),
            initialOffsetX = { it }
        ) + fadeIn(
            animationSpec = tween(DURATION_SHORT4, easing = EmphasizedDecelerate)
        )

    private fun forwardExit(): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(DURATION_MEDIUM3, easing = EmphasizedAccelerate),
            targetOffsetX = { -it / 4 }
        ) + fadeOut(
            animationSpec = tween(DURATION_SHORT4, easing = EmphasizedAccelerate)
        )

    private fun backwardEnter(): EnterTransition =
        slideInHorizontally(
            animationSpec = tween(DURATION_MEDIUM3, easing = EmphasizedDecelerate),
            initialOffsetX = { -it / 4 }
        ) + fadeIn(
            animationSpec = tween(DURATION_SHORT4, easing = EmphasizedDecelerate)
        )

    private fun backwardExit(): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(DURATION_MEDIUM3, easing = EmphasizedAccelerate),
            targetOffsetX = { it }
        ) + fadeOut(
            animationSpec = tween(DURATION_SHORT4, easing = EmphasizedAccelerate)
        )

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
            targetOffsetX = { it }
        ) + scaleOut(
            animationSpec = tween(DURATION_MEDIUM3, easing = EmphasizedAccelerate),
            targetScale = 0.9f
        ) + fadeOut(
            animationSpec = tween(DURATION_SHORT4, easing = EmphasizedAccelerate)
        )

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