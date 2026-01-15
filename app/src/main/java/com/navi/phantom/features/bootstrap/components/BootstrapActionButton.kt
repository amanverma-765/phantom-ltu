package com.navi.phantom.features.bootstrap.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class ButtonState {
    READY, BOOTSTRAPPING, BOOTSTRAPPED
}

@Composable
fun BootstrapActionButton(
    isReady: Boolean,
    isBootstrapping: Boolean,
    isBootstrapped: Boolean,
    onStartBootstrap: () -> Unit,
    onInstallClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonState = when {
        isReady -> ButtonState.READY
        isBootstrapped -> ButtonState.BOOTSTRAPPED
        isBootstrapping -> ButtonState.BOOTSTRAPPING
        else -> ButtonState.READY
    }

    AnimatedContent(
        targetState = buttonState,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.95f)) togetherWith
                    (fadeOut() + scaleOut(targetScale = 0.95f))
        },
        label = "buttonTransition",
        modifier = modifier.fillMaxWidth()
    ) { state ->
        when (state) {
            ButtonState.READY -> {
                ActionButton(
                    onClick = onStartBootstrap,
                    icon = Icons.Outlined.PlayArrow,
                    text = "Bootstrap",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            ButtonState.BOOTSTRAPPED -> {
                ActionButton(
                    onClick = onInstallClick,
                    icon = Icons.Outlined.InstallMobile,
                    text = "Install",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                )
            }

            ButtonState.BOOTSTRAPPING -> {
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    ButtonContent(
                        icon = Icons.Default.Close,
                        text = "Cancel",
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    colors: ButtonColors,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = colors,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        ButtonContent(
            icon = icon,
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ButtonContent(
    icon: ImageVector,
    text: String,
    fontWeight: FontWeight
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = fontWeight
        )
    }
}
