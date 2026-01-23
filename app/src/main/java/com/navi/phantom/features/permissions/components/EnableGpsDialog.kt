package com.navi.phantom.features.permissions.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EnableGpsDialog(
    onEnableGps: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Enable Location")
        },
        text = {
            Text(
                text = "GPS is currently disabled. Enable location services to use your current location on the map.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onEnableGps) {
                Text(text = "Enable")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Skip")
            }
        },
        modifier = modifier
    )
}
