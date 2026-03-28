package com.navi.phantom.features.map.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun CrosshairOverlay(
    isMapMoving: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = Color.Black.copy(alpha = 0.2f)
    val dotColor = Color.Red

    // Animate dot scale: grows when dragging, settles with bounce
    val dotScale by animateFloatAsState(
        targetValue = if (isMapMoving) 1.5f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dotScale"
    )

    // Animate dashed circle: expands when dragging, shrinks on settle
    val circleScale by animateFloatAsState(
        targetValue = if (isMapMoving) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "circleScale"
    )

    // Animate circle opacity: more visible when dragging
    val circleAlpha by animateFloatAsState(
        targetValue = if (isMapMoving) 0.8f else 0.45f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "circleAlpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val center = Offset(centerX, centerY)

        // Horizontal line — full width
        drawLine(
            color = lineColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx()
        )

        // Vertical line — full height
        drawLine(
            color = lineColor,
            start = Offset(centerX, 0f),
            end = Offset(centerX, size.height),
            strokeWidth = 1.dp.toPx()
        )

        // Dashed circle around the dot
        val circleRadius = 18.dp.toPx() * circleScale
        val dashLength = 8.dp.toPx()
        val gapLength = 6.dp.toPx()
        drawCircle(
            color = Color.Black.copy(alpha = circleAlpha * 0.5f),
            radius = circleRadius,
            center = center,
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(dashLength, gapLength),
                    0f
                )
            )
        )

        // Red center dot — animated scale
        val dotRadiusPx = 5.dp.toPx() * dotScale
        drawCircle(
            color = dotColor,
            radius = dotRadiusPx,
            center = center
        )
    }
}
