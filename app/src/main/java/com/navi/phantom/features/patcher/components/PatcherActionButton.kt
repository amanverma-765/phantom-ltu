package com.navi.phantom.features.patcher.components

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
import androidx.compose.material.icons.filled.Refresh
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
import com.navi.phantom.features.patcher.logic.PatcherPhase
import com.navi.phantom.features.patcher.logic.FailedPhase

@Composable
fun PatcherActionButton(
    phase: PatcherPhase,
    canStartPatching: Boolean,
    canInstall: Boolean,
    onStartPatching: () -> Unit,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onLaunch: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    statusColors: PatcherStatusColors = rememberPatcherStatusColors()
) {
    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.95f)) togetherWith
                    (fadeOut() + scaleOut(targetScale = 0.95f))
        },
        label = "buttonTransition",
        modifier = modifier.fillMaxWidth()
    ) { currentPhase ->
        when (currentPhase) {
            is PatcherPhase.Ready -> {
                ActionButton(
                    onClick = onStartPatching,
                    icon = Icons.Outlined.PlayArrow,
                    text = "Patch",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            is PatcherPhase.Patched -> {
                ActionButton(
                    onClick = onInstall,
                    icon = Icons.Outlined.InstallMobile,
                    text = "Install",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                )
            }

            is PatcherPhase.Patching -> {
                CancelButton(onClick = onCancel, text = "Cancel")
            }

            is PatcherPhase.Installing -> {
                CancelButton(onClick = onCancel, text = "Cancel Installation")
            }

            is PatcherPhase.Uninstalling -> {
                CancelButton(onClick = onCancel, text = "Cancel Uninstall")
            }

            is PatcherPhase.AwaitingUninstallConfirm -> {
                // Dialog handles this state, show cancel button as fallback
                CancelButton(onClick = onCancel, text = "Cancel")
            }

            is PatcherPhase.Installed -> {
                ActionButton(
                    onClick = onLaunch,
                    icon = Icons.Outlined.PlayArrow,
                    text = "Launch",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = statusColors.success,
                        contentColor = statusColors.onSuccess
                    )
                )
            }

            is PatcherPhase.Failed -> {
                val (text, onClick) = when (currentPhase.failedDuring) {
                    FailedPhase.PATCHING -> "Retry" to onRetry
                    FailedPhase.INSTALL, FailedPhase.UNINSTALL -> "Retry Installation" to onRetry
                }
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    ButtonContent(
                        icon = Icons.Default.Refresh,
                        text = text,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            is PatcherPhase.Cancelled -> {
                // Can restart patching or install depending on what we have
                when {
                    canInstall -> ActionButton(
                        onClick = onInstall,
                        icon = Icons.Outlined.InstallMobile,
                        text = "Install",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    )
                    canStartPatching -> ActionButton(
                        onClick = onStartPatching,
                        icon = Icons.Outlined.PlayArrow,
                        text = "Patch",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    else -> CancelButton(onClick = onNavigateBack, text = "Go Back")
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
private fun CancelButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        ButtonContent(
            icon = Icons.Default.Close,
            text = text,
            fontWeight = FontWeight.Normal
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
