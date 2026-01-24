package com.navi.phantom.features.map.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

@Composable
fun MapLoadingSkeleton(
    modifier: Modifier = Modifier
) {
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")

    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
    val shimmerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            surfaceColor,
            shimmerColor,
            surfaceColor
        ),
        start = Offset(shimmerOffset - 500f, shimmerOffset - 500f),
        end = Offset(shimmerOffset, shimmerOffset)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = shimmerBrush),
        contentAlignment = Alignment.Center
    ) {}
}
