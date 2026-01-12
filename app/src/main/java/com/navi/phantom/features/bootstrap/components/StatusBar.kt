package com.navi.phantom.features.bootstrap.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    modifier: Modifier = Modifier
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

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isBootstrapped -> statusColors.successContainer
            hasFailed -> statusColors.errorContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(400),
        label = "statusBg"
    )

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Status indicator dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isBootstrapped || hasFailed) FontWeight.Medium else FontWeight.Normal,
                color = when {
                    isBootstrapped -> statusColors.success
                    hasFailed -> statusColors.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Blinking cursor during bootstrapping
            if (isBootstrapping) {
                Text(
                    text = "_",
                    fontFamily = FontFamily.Monospace,
                    color = accentColor,
                    modifier = Modifier.alpha(cursorAlpha)
                )
            }
        }
    }
}
