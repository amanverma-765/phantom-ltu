package com.navi.phantom.features.patcher.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun UninstallRequiredDialog(
    packageName: String?,
    onConfirmUninstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reinstall Required",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = buildString {
                    if (packageName != null) {
                        append("Package: $packageName\n\n")
                    }
                    append("The existing app must be uninstalled to continue. App data will be removed.")
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmUninstall) {
                Text(
                    text = "Uninstall",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
        modifier = modifier
    )
}