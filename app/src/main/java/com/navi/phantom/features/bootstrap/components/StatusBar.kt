package com.navi.phantom.features.bootstrap.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun StatusBar(
    message: String,
    isBootstrapping: Boolean,
    isBootstrapped: Boolean,
    hasFailed: Boolean,
    accentColor: Color,
    statusColors: BootstrapStatusColors,
    modifier: Modifier = Modifier,
    showCopyButton: Boolean = false,
    onCopyError: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(530),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )

    val dotPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    // Material 3 color hierarchy:
    // Header uses surfaceContainerHigh, status bar uses surfaceContainer (one level below)
    // Error state takes precedence over success (e.g., bootstrap succeeded but installation failed)
    val backgroundColor by animateColorAsState(
        targetValue = when {
            hasFailed -> statusColors.errorContainer
            isBootstrapped -> statusColors.successContainer
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(400),
        label = "statusBg"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            hasFailed -> statusColors.error
            isBootstrapped -> statusColors.success
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(400),
        label = "textColor"
    )

    val dotColor by animateColorAsState(
        targetValue = when {
            hasFailed -> statusColors.error
            isBootstrapped -> statusColors.success
            isBootstrapping -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(400),
        label = "dotColor"
    )

    Surface(
        color = backgroundColor,
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
                // Status indicator dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .alpha(if (isBootstrapping) dotPulse else 1f)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isBootstrapping || isBootstrapped) FontWeight.Medium else FontWeight.Normal,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Blinking cursor during bootstrapping
                if (isBootstrapping) {
                    Text(
                        text = "_",
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        color = textColor,
                        modifier = Modifier.alpha(cursorAlpha)
                    )
                }

                // Copy error log button
                if (showCopyButton && hasFailed) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onCopyError) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy error log",
                            tint = statusColors.error
                        )
                    }
                }
            }

            // Progress indicator during bootstrapping
            AnimatedVisibility(
                visible = isBootstrapping,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}
