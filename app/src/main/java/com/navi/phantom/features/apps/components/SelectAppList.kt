package com.navi.phantom.features.apps.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.navi.phantom.domain.model.DeviceApp

@Composable
fun SelectAppList(
    patchedApps: List<DeviceApp>,
    unpatchedApps: List<DeviceApp>,
    onPatchedAppClick: (DeviceApp) -> Unit,
    onUnpatchedAppClick: (DeviceApp) -> Unit,
    onUnsupportedAppClick: (DeviceApp) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier, state = listState) {
        // Patched apps section
        if (patchedApps.isNotEmpty()) {
            item(key = "patched_header") {
                Text(
                    text = "Patched Apps",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            itemsIndexed(
                items = patchedApps,
                key = { _, app -> "patched_${app.packageName}" }
            ) { index, patchedApp ->
                SelectAppItem(
                    app = patchedApp,
                    onClick = { onPatchedAppClick(patchedApp) },
                    modifier = Modifier.animateItem()
                )
                if (index < patchedApps.lastIndex) {
                    HorizontalDivider()
                }
            }

            // Separator between patched and installed apps
            if (unpatchedApps.isNotEmpty()) {
                item(key = "separator") {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "Device Apps",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Device apps section
        itemsIndexed(
            items = unpatchedApps,
            key = { _, app -> "installed_${app.packageName}" }
        ) { index, app ->
            SelectAppItem(
                app = app,
                onClick = { onUnpatchedAppClick(app) },
                onUnsupportedClick = { onUnsupportedAppClick(app) },
                modifier = Modifier.animateItem()
            )
            if (index < unpatchedApps.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}