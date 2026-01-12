package com.navi.phantom.features.bootstrap.components

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
import com.navi.phantom.features.apps.components.AppIcon

@Composable
fun HeroSection(
    app: DetailedAppInfo,
    isBootstrapping: Boolean,
    isBootstrapped: Boolean,
    hasFailed: Boolean,
    accentColor: Color,
    statusColors: BootstrapStatusColors,
    modifier: Modifier = Modifier
) {
    val successScale by animateFloatAsState(
        targetValue = if (isBootstrapped) 1.03f else 1f,
        animationSpec = tween(300),
        label = "successScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(200.dp)
            .scale(successScale)
    ) {
        // Animated rings during bootstrapping
        if (isBootstrapping) {
            AnimatedRings(
                accentColor = accentColor,
                modifier = Modifier.size(200.dp)
            )
        }

        // Complete state ring
        if (isBootstrapped) {
            Canvas(modifier = Modifier.size(180.dp)) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.2f),
                    radius = size.width / 2 - 3.dp.toPx(),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Ready state - subtle ring
        if (!isBootstrapping && !isBootstrapped && !hasFailed) {
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
        if (isBootstrapped) {
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
        if (hasFailed) {
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
