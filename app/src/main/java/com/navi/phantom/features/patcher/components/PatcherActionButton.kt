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


private enum class ButtonState {
    READY,
    PATCHING,
    PATCHED,
    INSTALLING,
    INSTALLED,
    FAILED_PATCHING,
    FAILED_INSTALL,
    UNINSTALLING,
    AWAITING_UNINSTALL_CONFIRM,
    CANCELLED
}


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
    // Derive button state from phase
    val buttonState = when (phase) {
        is PatcherPhase.Ready -> ButtonState.READY
        is PatcherPhase.Patching -> ButtonState.PATCHING
        is PatcherPhase.Patched -> ButtonState.PATCHED
        is PatcherPhase.Installing -> ButtonState.INSTALLING
        is PatcherPhase.Installed -> ButtonState.INSTALLED
        is PatcherPhase.AwaitingUninstallConfirm -> ButtonState.AWAITING_UNINSTALL_CONFIRM
        is PatcherPhase.Uninstalling -> ButtonState.UNINSTALLING
        is PatcherPhase.Cancelled -> ButtonState.CANCELLED
        is PatcherPhase.Failed -> when (phase.failedDuring) {
            FailedPhase.PATCHING -> ButtonState.FAILED_PATCHING
            FailedPhase.INSTALL -> ButtonState.FAILED_INSTALL
            FailedPhase.UNINSTALL -> ButtonState.FAILED_INSTALL // Treat as install failure for retry
        }
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
                    onClick = onStartPatching,
                    icon = Icons.Outlined.PlayArrow,
                    text = "Patch",
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            ButtonState.PATCHED -> {
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

            ButtonState.PATCHING -> {
                CancelButton(
                    onClick = onCancel,
                    text = "Cancel"
                )
            }

            ButtonState.INSTALLING -> {
                CancelButton(
                    onClick = onCancel,
                    text = "Cancel Installation"
                )
            }

            ButtonState.UNINSTALLING -> {
                CancelButton(
                    onClick = onCancel,
                    text = "Cancel Uninstall"
                )
            }

            ButtonState.AWAITING_UNINSTALL_CONFIRM -> {
                // Dialog handles this state, show cancel button as fallback
                CancelButton(
                    onClick = onCancel,
                    text = "Cancel"
                )
            }

            ButtonState.INSTALLED -> {
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

            ButtonState.FAILED_PATCHING -> {
                Button(
                    onClick = onRetry,
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
                        text = "Retry",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            ButtonState.FAILED_INSTALL -> {
                Button(
                    onClick = onRetry,
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
                        text = "Retry Installation",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            ButtonState.CANCELLED -> {
                // Can restart patching or install depending on what we have
                if (canInstall) {
                    ActionButton(
                        onClick = onInstall,
                        icon = Icons.Outlined.InstallMobile,
                        text = "Install",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    )
                } else if (canStartPatching) {
                    ActionButton(
                        onClick = onStartPatching,
                        icon = Icons.Outlined.PlayArrow,
                        text = "Patch",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                } else {
                    // Fallback - go back
                    CancelButton(
                        onClick = onNavigateBack,
                        text = "Go Back"
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