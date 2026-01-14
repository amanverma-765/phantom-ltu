package com.navi.phantom.features.apps.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.navi.phantom.domain.models.DetailedAppInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppInfoCard(
    app: DetailedAppInfo,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            InfoRow(
                icon = Icons.Outlined.AccountBox,
                label = "Package",
                value = app.packageName,
                useMono = true
            )

            InfoRow(
                icon = Icons.Outlined.Info,
                label = "Version",
                value = "${app.versionName} (${app.versionCode})"
            )

            InfoRow(
                icon = Icons.AutoMirrored.Outlined.List,
                label = "Size",
                value = app.formattedSize
            )

            InfoRow(
                icon = Icons.Outlined.Settings,
                label = "SDK",
                value = "${app.minSdk} → ${app.targetSdk}"
            )

            InfoRow(
                icon = Icons.Outlined.DateRange,
                label = "Installed",
                value = dateFormat.format(Date(app.installTimeMillis))
            )

            InfoRow(
                icon = Icons.Outlined.Refresh,
                label = "Updated",
                value = dateFormat.format(Date(app.lastUpdateTimeMillis))
            )

            if (app.usesLocation) {
                InfoRow(
                    icon = Icons.Outlined.LocationOn,
                    label = "Location",
                    value = "Required",
                    highlighted = true
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    useMono: Boolean = false,
    highlighted: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlighted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (useMono) FontFamily.Monospace else FontFamily.Default,
            color = if (highlighted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
