package com.navi.phantom.features.apps.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.navi.phantom.domain.model.DeviceApp
import com.navi.phantom.features.apps.components.AppSearchBar
import com.navi.phantom.features.apps.components.SelectAppList
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                .padding(bottom = 8.dp)
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
                uiState.errorMessage != null -> uiState.errorMessage?.let { errorMsg ->
                    ErrorState(
                        message = errorMsg,
                        onRetry = { viewModel.onEvent(AppUiEvent.GetAllDeviceApps) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.filteredPatchedApps.isEmpty() &&
                    uiState.filteredUnpatchedApps.isEmpty() &&
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
                        unpatchedApps = uiState.filteredUnpatchedApps,
                        onPatchedAppClick = { app ->
                            Toast.makeText(context, "${app.appName} is already patched", Toast.LENGTH_SHORT).show()
                        },
                        onUnpatchedAppClick = { onAppSelected(it) },
                        listState = listState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}