package com.navi.phantom.features.apps.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.navi.phantom.core.ui.EmptyState
import com.navi.phantom.core.ui.ErrorState
import com.navi.phantom.core.ui.LoadingState
import com.navi.phantom.domain.model.DeviceApp
import com.navi.phantom.domain.model.PatchedApp
import com.navi.phantom.features.apps.components.AppListItem
import com.navi.phantom.features.apps.components.AppSearchBar
import com.navi.phantom.features.apps.components.PatchedAppListItem
import com.navi.phantom.features.apps.logic.AppUiEvent
import com.navi.phantom.features.apps.logic.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAppScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onAppSelected: (DeviceApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    LaunchedEffect(Unit) {
        viewModel.onEvent(AppUiEvent.GetAllDeviceApps)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Select App") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            AppSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onEvent(AppUiEvent.UpdateSearchQuery(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            )

            when {
                uiState.isLoadingApps && uiState.allDeviceApps.isEmpty() -> LoadingState(
                    message = "Loading installed apps...",
                    modifier = Modifier.fillMaxSize()
                )
                uiState.errorMessage != null -> uiState.errorMessage?.let { errorMsg ->
                    ErrorState(
                        message = errorMsg,
                        onRetry = { viewModel.onEvent(AppUiEvent.GetAllDeviceApps) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.filteredPatchedApps.isEmpty() &&
                    uiState.filteredDeviceApps.isEmpty() &&
                    uiState.searchQuery.isNotBlank() -> {
                    EmptyState(
                        message = "No apps found for \"${uiState.searchQuery}\"",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.allDeviceApps.isEmpty() -> EmptyState(
                    message = "No apps found on your device",
                    modifier = Modifier.fillMaxSize()
                )
                else -> {
                    SelectAppList(
                        patchedApps = uiState.filteredPatchedApps,
                        unpatchedApps = uiState.filteredDeviceApps,
                        onPatchedAppClick = { },
                        onUnPatchedAppClick = { installedApp ->
                            onAppSelected(installedApp)
                        },
                        onUnsupportedAppClick = { installedApp ->
                            Toast.makeText(
                                context,
                                "${installedApp.appName} is not supported",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        listState = listState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Combined list showing patched apps on top with a separator, followed by installed apps.
 */
@Composable
private fun SelectAppList(
    patchedApps: List<PatchedApp>,
    unpatchedApps: List<DeviceApp>,
    onPatchedAppClick: (PatchedApp) -> Unit,
    onUnPatchedAppClick: (DeviceApp) -> Unit,
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
                PatchedAppListItem(
                    patchedApp = patchedApp,
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
            AppListItem(
                app = app,
                onClick = { onUnPatchedAppClick(app) },
                onUnsupportedClick = { onUnsupportedAppClick(app) },
                modifier = Modifier.animateItem()
            )
            if (index < unpatchedApps.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}