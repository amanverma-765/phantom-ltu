package com.navi.phantom.features.apps.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.navi.phantom.domain.model.DeviceApp

@Composable
internal fun SelectAppItem(
    modifier: Modifier = Modifier,
    app: DeviceApp,
    onClick: () -> Unit,
    onUnsupportedClick: () -> Unit = {}
) {
    val trailingIcon: TrailingIconConfig = when {
        app.isPatched -> TrailingIconConfig(
            icon = Icons.Filled.CheckCircle,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = "Patched"
        )
        app.usesLocation -> TrailingIconConfig(
            icon = Icons.Outlined.LocationOn,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = "Uses location"
        )
        else -> TrailingIconConfig(
            icon = Icons.Outlined.LocationOff,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            tint = MaterialTheme.colorScheme.error,
            contentDescription = "No location"
        )
    }

    val clickAction = when {
        app.isPatched -> onClick
        app.usesLocation -> onClick
        else -> onUnsupportedClick
    }

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
                color = trailingIcon.containerColor,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = trailingIcon.icon,
                    contentDescription = trailingIcon.contentDescription,
                    tint = trailingIcon.tint,
                    modifier = Modifier.size(28.dp).padding(4.dp)
                )
            }
        },
        leadingContent = {
            AppIcon(
                packageName = app.packageName,
                appName = app.appName,
                size = 48.dp,
                cornerRadius = 12.dp
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.clickable(onClick = clickAction)
    )
}

private data class TrailingIconConfig(
    val icon: ImageVector,
    val containerColor: Color,
    val tint: Color,
    val contentDescription: String
)