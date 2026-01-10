package com.navi.phantom.features.apps.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.navi.phantom.domain.models.DetailedAppInfo

@Composable
fun HeroSection(
    app: DetailedAppInfo,
    isPatching: Boolean,
    isPatchComplete: Boolean,
    hasPatchFailed: Boolean,
    accentColor: Color,
    statusColors: PatchingStatusColors,
    modifier: Modifier = Modifier
) {
    val successScale by animateFloatAsState(
        targetValue = if (isPatchComplete) 1.03f else 1f,
        animationSpec = tween(300),
        label = "successScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(200.dp)
            .scale(successScale)
    ) {
        // Animated rings during patching
        if (isPatching) {
            AnimatedRings(
                accentColor = accentColor,
                modifier = Modifier.size(200.dp)
            )
        }

        // Complete state ring
        if (isPatchComplete) {
            Canvas(modifier = Modifier.size(180.dp)) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.2f),
                    radius = size.width / 2 - 3.dp.toPx(),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Ready state - subtle ring
        if (!isPatching && !isPatchComplete && !hasPatchFailed) {
            Canvas(modifier = Modifier.size(160.dp)) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.08f),
                    radius = size.width / 2,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // App Icon
        AppIcon(
            icon = app.icon,
            appName = app.appName,
            size = 120.dp,
            cornerRadius = 28.dp
        )

        // Success badge
        if (isPatchComplete) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Surface(
                    color = statusColors.successContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Complete",
                        tint = statusColors.success,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }

        // Failure badge
        if (hasPatchFailed) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Surface(
                    color = statusColors.errorContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Failed",
                        tint = statusColors.error,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}
