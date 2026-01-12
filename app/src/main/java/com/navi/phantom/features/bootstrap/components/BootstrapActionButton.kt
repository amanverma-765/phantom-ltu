package com.navi.phantom.features.bootstrap.components

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
fun BootstrapActionButton(
    isReady: Boolean,
    isBootstrapping: Boolean,
    isBootstrapped: Boolean,
    onStartBootstrap: () -> Unit,
    onInstallClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isReady -> {
            Button(
                onClick = onStartBootstrap,
                modifier = modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Bootstrap",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        isBootstrapped -> {
            Button(
                onClick = onInstallClick,
                modifier = modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Install Bootstrapped App",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        isBootstrapping -> {
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
