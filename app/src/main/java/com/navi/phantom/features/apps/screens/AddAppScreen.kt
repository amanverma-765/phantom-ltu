package com.navi.phantom.features.apps.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.navi.phantom.components.EmptyState
import com.navi.phantom.components.ErrorState
import com.navi.phantom.components.LoadingState
import com.navi.phantom.features.apps.components.AppList
import com.navi.phantom.features.apps.components.AppSearchBar
import com.navi.phantom.features.apps.logic.AppUiEvent
import com.navi.phantom.features.apps.logic.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onEvent(AppUiEvent.GetAllInstalledApps)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Installed Apps") },
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
                uiState.isLoading -> LoadingState(
                    message = "Loading installed apps...",
                    modifier = Modifier.fillMaxSize()
                )
                uiState.errorMsg != null -> ErrorState(
                    message = uiState.errorMsg!!,
                    onRetry = { viewModel.onEvent(AppUiEvent.GetAllInstalledApps) },
                    modifier = Modifier.fillMaxSize()
                )
                uiState.filteredApps.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    EmptyState(
                        message = "No apps found for \"${uiState.searchQuery}\"",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.allInstalledApps.isEmpty() -> EmptyState(
                    message = "No apps found on your device",
                    modifier = Modifier.fillMaxSize()
                )
                else -> AppList(
                    apps = uiState.filteredApps,
                    onAppClick = { app ->
                        viewModel.onEvent(AppUiEvent.SelectApp(app))
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}