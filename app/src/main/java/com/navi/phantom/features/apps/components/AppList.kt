package com.navi.phantom.features.apps.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
        items(
            items = apps,
            key = { it.packageName }
        ) { app ->
            AppListItem(
                app = app,
                onClick = { onAppClick(app) }
            )
            if (app != apps.last()) {
                HorizontalDivider()
            }
        }
    }
}