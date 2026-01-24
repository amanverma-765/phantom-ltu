package com.navi.phantom.features.map.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@Composable
fun MapControlCluster(
    isSatelliteMode: Boolean,
    isLoadingLocation: Boolean,
    onLayerToggle: () -> Unit,
    onMyLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Layer toggle - SmallFAB
        SmallFloatingActionButton(
            onClick = onLayerToggle,
            containerColor = if (isSatelliteMode)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (isSatelliteMode)
                MaterialTheme.colorScheme.onTertiaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 3.dp,
                pressedElevation = 6.dp
            )
        ) {
            AnimatedContent(
                targetState = isSatelliteMode,
                transitionSpec = {
                    (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut())
                },
                label = "layerIcon"
            ) { satellite ->
                Icon(
                    imageVector = if (satellite) Icons.Filled.Satellite else Icons.Filled.Layers,
                    contentDescription = if (satellite) "Street view" else "Satellite view"
                )
            }
        }

        // My location - Regular FAB
        LocationFab(
            onClick = onMyLocation,
            isLoading = isLoadingLocation
        )
    }
}

@Composable
private fun LocationFab(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    FloatingActionButton(
        onClick = { if (!isLoading) onClick() },
        modifier = modifier.then(
            if (isLoading) Modifier.scale(pulseScale) else Modifier
        ),
        containerColor = if (isLoading)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (isLoading)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp
        )
    ) {
        Icon(
            imageVector = Icons.Filled.MyLocation,
            contentDescription = "My location"
        )
    }
}
