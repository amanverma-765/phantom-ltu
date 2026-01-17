package com.navi.phantom.features.patcher.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.navi.phantom.domain.model.PatchingStep

private val timelineSteps = PatchingStep.entries

@Composable
fun PatcherTimeline(
    currentStep: PatchingStep?,
    isPatching: Boolean,
    isPatched: Boolean,
    hasFailed: Boolean,
    statusColors: PatcherStatusColors,
    modifier: Modifier = Modifier
) {
    val currentStepIndex = remember(currentStep) {
        if (currentStep != null) timelineSteps.indexOf(currentStep) else -1
    }

    val completedSteps = when {
        isPatched -> timelineSteps.size
        currentStepIndex >= 0 -> currentStepIndex
        else -> 0
    }

    val progress by animateFloatAsState(
        targetValue = if (isPatched) 1f else completedSteps.toFloat() / timelineSteps.size,
        animationSpec = tween(400),
        label = "progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "timeline")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (isPatched) "Complete" else "$completedSteps of ${timelineSteps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isPatched) statusColors.success else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                color = if (isPatched) statusColors.success
                        else if (hasFailed) statusColors.error
                        else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            timelineSteps.forEachIndexed { index, step ->
                val isCompleted = isPatched || index < currentStepIndex
                val isActive = index == currentStepIndex && isPatching && !hasFailed
                val isPending = !isCompleted && !isActive

                TimelineStep(
                    label = step.title,
                    isCompleted = isCompleted,
                    isActive = isActive,
                    isPending = isPending,
                    hasFailed = hasFailed && index == currentStepIndex,
                    pulseAlpha = pulseAlpha,
                    statusColors = statusColors,
                    showConnector = index < timelineSteps.lastIndex
                )
            }
        }
    }
}

@Composable
private fun TimelineStep(
    label: String,
    isCompleted: Boolean,
    isActive: Boolean,
    isPending: Boolean,
    hasFailed: Boolean,
    pulseAlpha: Float,
    statusColors: PatcherStatusColors,
    showConnector: Boolean
) {
    val dotColor by animateColorAsState(
        targetValue = when {
            isCompleted -> statusColors.success
            isActive -> MaterialTheme.colorScheme.primary
            hasFailed -> statusColors.error
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(300),
        label = "dotColor"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.onSurface
            isActive -> MaterialTheme.colorScheme.onSurface
            hasFailed -> statusColors.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        },
        animationSpec = tween(300),
        label = "textColor"
    )

    val connectorColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.width(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .alpha(if (isActive) pulseAlpha else 1f)
                ) {
                    if (isCompleted) {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            drawCircle(
                                color = dotColor.copy(alpha = 0.15f),
                                radius = size.minDimension / 2
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = dotColor,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            if (isActive) {
                                drawCircle(
                                    color = dotColor.copy(alpha = 0.2f),
                                    radius = size.minDimension / 2
                                )
                            }
                            drawCircle(
                                color = if (isPending) Color.Transparent else dotColor,
                                radius = if (isActive) 6.dp.toPx() else 4.dp.toPx()
                            )
                            if (isPending) {
                                drawCircle(
                                    color = dotColor,
                                    radius = 4.dp.toPx(),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 1.5.dp.toPx()
                                    )
                                )
                            }
                        }
                    }
                }

                if (showConnector) {
                    Canvas(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                    ) {
                        drawLine(
                            color = connectorColor,
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isActive || isCompleted) FontWeight.Medium else FontWeight.Normal,
            color = textColor,
            modifier = Modifier.padding(top = 2.dp, bottom = if (showConnector) 12.dp else 0.dp)
        )
    }
}
