package com.navi.phantom.features.apps.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedRings(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ringsAnim")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = modifier) {
        // Rotating rings
        Canvas(modifier = Modifier.size(200.dp)) {
            val strokeWidth = 3.dp.toPx()
            val center = Offset(size.width / 2, size.height / 2)

            // Outer segmented ring
            rotate(outerRotation, center) {
                for (i in 0 until 8) {
                    val startAngle = i * 45f + 5f
                    drawArc(
                        color = accentColor.copy(alpha = 0.5f),
                        startAngle = startAngle,
                        sweepAngle = 35f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth, strokeWidth),
                        size = Size(size.width - strokeWidth * 2, size.height - strokeWidth * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Inner dashed ring
            rotate(innerRotation, center) {
                val innerSize = size.width * 0.85f
                val innerOffset = (size.width - innerSize) / 2
                for (i in 0 until 12) {
                    val startAngle = i * 30f + 5f
                    drawArc(
                        color = accentColor.copy(alpha = 0.25f),
                        startAngle = startAngle,
                        sweepAngle = 18f,
                        useCenter = false,
                        topLeft = Offset(innerOffset, innerOffset),
                        size = Size(innerSize, innerSize),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Pulsing glow
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(pulse)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
                }
        )
    }
}
