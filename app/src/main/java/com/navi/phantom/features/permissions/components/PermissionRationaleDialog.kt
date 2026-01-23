package com.navi.phantom.features.permissions.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun PermissionRationaleDialog(
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Location Permission Required")
        },
        text = {
            Text(
                text = "PhantomGPS needs access to your location to show your current position on the map and enable the mock location feature.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onGrantPermission) {
                Text(text = "Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Not Now")
            }
        },
        modifier = modifier
    )
}
