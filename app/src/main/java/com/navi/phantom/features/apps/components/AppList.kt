package com.navi.phantom.features.apps.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.navi.phantom.domain.models.InstalledApp

@Composable
internal fun AppList(
    apps: List<InstalledApp>,
    onAppClick: (InstalledApp) -> Unit,
    onUnsupportedAppClick: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    LazyColumn(modifier = modifier, state = listState) {
        itemsIndexed(
            items = apps,
            key = { _, app -> app.packageName }
        ) { index, app ->
            AppListItem(
                app = app,
                onClick = { onAppClick(app) },
                onUnsupportedClick = { onUnsupportedAppClick(app) },
                modifier = Modifier.animateItem()
            )
            if (index < apps.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}