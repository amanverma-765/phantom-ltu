package com.navi.phantom.features.apps.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.navi.phantom.core.ui.LoadingState
import com.navi.phantom.features.apps.components.EmptyPlacesState
import com.navi.phantom.features.apps.components.PlaceOptionCard
import com.navi.phantom.features.apps.components.RealLocationCard
import com.navi.phantom.features.apps.logic.AppDetailUiEvent
import com.navi.phantom.features.apps.logic.AppDetailViewModel
import com.navi.phantom.features.patcher.components.AppInfoHeader
import com.navi.phantom.features.patcher.components.rememberPatcherStatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLocationScreen(
    packageName: String,
    viewModel: AppDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(packageName) {
        viewModel.onEvent(AppDetailUiEvent.Load(packageName))
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("App Location", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null)
                        )
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "App settings"
                        )
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Uninstall",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingState(
                    message = "Loading...",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            uiState.app == null -> {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(32.dp)
                ) {
                    Text(
                        text = "App not found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                val app = uiState.app!!
                val hasActiveLocation = uiState.activeLocation != null

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 0.dp,
                        bottom = 88.dp
                    ),
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    item(key = "header") {
                        Box {
                            AppInfoHeader(
                                app = app,
                                isPatched = false,
                                hasFailed = false,
                                statusColors = rememberPatcherStatusColors()
                            )
                            IconButton(
                                onClick = {
                                    val intent = context.packageManager
                                        .getLaunchIntentForPackage(packageName)
                                    if (intent != null) {
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Cannot launch this app",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = "Open app",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    item(key = "real-location") {
                        Spacer(modifier = Modifier.height(12.dp))
                        RealLocationCard(
                            isSelected = !hasActiveLocation,
                            onClick = {
                                if (hasActiveLocation) {
                                    viewModel.onEvent(
                                        AppDetailUiEvent.ClearLocation(packageName)
                                    )
                                }
                            }
                        )
                    }

                    item(key = "divider") {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Spoof with a saved place",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (uiState.places.isEmpty()) {
                        item(key = "empty") {
                            EmptyPlacesState()
                        }
                    } else {
                        items(
                            items = uiState.places,
                            key = { it.id }
                        ) { place ->
                            val isSelected = uiState.activeLocation?.placeId == place.id
                            PlaceOptionCard(
                                place = place,
                                isSelected = isSelected,
                                onClick = {
                                    viewModel.onEvent(
                                        AppDetailUiEvent.AssignPlace(packageName, place)
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
