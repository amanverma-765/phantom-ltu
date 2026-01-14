package com.navi.phantom.features.apps.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.navi.phantom.domain.models.InstalledApp

@Composable
internal fun AppList(
    apps: List<InstalledApp>,
    onAppClick: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(
            items = apps,
            key = { _, app -> app.packageName }
        ) { index, app ->
            AppListItem(
                app = app,
                onClick = { onAppClick(app) }
            )
            if (index < apps.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}