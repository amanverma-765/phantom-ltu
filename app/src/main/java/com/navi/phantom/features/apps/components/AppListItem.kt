package com.navi.phantom.features.apps.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.navi.phantom.domain.models.InstalledApp

@Composable
internal fun AppListItem(
    app: InstalledApp,
    onClick: () -> Unit,
    onUnsupportedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSupported = app.usesLocation

    ListItem(
        headlineContent = {
            Text(
                text = app.appName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = "v${app.versionName} • ${app.packageName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Surface(
                color = if (isSupported) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isSupported) {
                        Icons.Outlined.LocationOn
                    } else {
                        Icons.Outlined.LocationOff
                    },
                    contentDescription = if (isSupported) "Uses location" else "No location",
                    tint = if (isSupported) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(28.dp).padding(4.dp)
                )
            }
        },
        leadingContent = {
            AppIcon(
                icon = app.icon,
                appName = app.appName,
                size = 48.dp,
                cornerRadius = 12.dp
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.clickable(onClick = if (isSupported) onClick else onUnsupportedClick)
    )
}