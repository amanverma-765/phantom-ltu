package com.navi.phantom.features.apps.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.rounded.AppsOutage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.navi.phantom.R
import com.navi.phantom.core.ui.EmptyStateScreen
import com.navi.phantom.core.ui.LoadingState
import com.navi.phantom.domain.model.DeviceApp
import com.navi.phantom.features.apps.components.PatchedAppCard
import com.navi.phantom.features.apps.logic.AppUiEvent
import com.navi.phantom.features.apps.logic.AppViewModel
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchedAppScreen(
    modifier: Modifier = Modifier,
    onAddAppClick: () -> Unit,
    onPatchedAppClick: (DeviceApp) -> Unit = {},
    viewModel: AppViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val patchedApps = uiState.filteredPatchedApps
    val isScreenEmpty = patchedApps.isEmpty()
    val isLoading = uiState.isLoadingApps && uiState.allDeviceApps.isEmpty()

    val listState = rememberLazyListState()
    val isFabExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(AppUiEvent.GetAllDeviceApps)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) }
            )
        },
        floatingActionButton = {
            if (!isScreenEmpty && !isLoading) {
                FloatingActionButton(
                    onClick = onAddAppClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add App"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 88.dp
            ),
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    item {
                        LoadingState(
                            message = "Loading apps...",
                            modifier = Modifier.fillParentMaxSize()
                        )
                    }
                }
                isScreenEmpty -> {
                    item {
                        EmptyStateScreen(
                            icon = Icons.Rounded.AppsOutage,
                            title = "No Apps Yet",
                            description = "Get started by adding an app.\nEach app gets its own virtual location.",
                            buttonIcon = Icons.Outlined.AddCircleOutline,
                            buttonText = "Add Your First App",
                            onButtonClick = onAddAppClick,
                            modifier = Modifier.fillParentMaxSize()
                        )
                    }
                }
                else -> {
                    items(
                        items = patchedApps,
                        key = { it.packageName }
                    ) { patchedApp ->
                        PatchedAppCard(
                            app = patchedApp,
                            onClick = { onPatchedAppClick(patchedApp) },
                            modifier = Modifier.fillMaxWidth(),
                            activeLocation = uiState.activeLocations[patchedApp.packageName]
                        )
                    }
                }
            }
        }
    }
}