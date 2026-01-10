package com.navi.phantom.features.apps.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PatchingActionButton(
    isReady: Boolean,
    isPatching: Boolean,
    isPatchComplete: Boolean,
    onStartPatching: () -> Unit,
    onInstallClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isReady -> {
            Button(
                onClick = onStartPatching,
                modifier = modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Start Patching",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        isPatchComplete -> {
            Button(
                onClick = onInstallClick,
                modifier = modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Install Patched App",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        isPatching -> {
            FilledTonalButton(
                onClick = onCancel,
                modifier = modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
