package com.navi.phantom.features.patcher.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.navi.phantom.core.ui.LoadingState
import com.navi.phantom.features.patcher.components.AppInfoHeader
import com.navi.phantom.features.patcher.components.PatcherActionButton
import com.navi.phantom.features.patcher.components.PatcherTimeline
import com.navi.phantom.features.patcher.components.StatusBar
import com.navi.phantom.features.patcher.components.UninstallRequiredDialog
import com.navi.phantom.features.patcher.components.rememberPatcherStatusColors
import com.navi.phantom.features.patcher.logic.PatcherPhase
import com.navi.phantom.features.patcher.logic.PatcherUiEvent
import com.navi.phantom.features.patcher.logic.PatcherViewModel
import com.navi.phantom.features.patcher.logic.FailedPhase
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatcherScreen(
    viewModel: PatcherViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val app = uiState.app
    val isLoadingApp = uiState.isLoadingApp
    val phase = uiState.phase

    val isPatching = uiState.isPatching
    val isPatched = uiState.isPatched
    val isInstalling = uiState.isInstalling
    val isInstalled = uiState.isInstalled
    val isUninstalling = uiState.isUninstalling
    val hasFailed = uiState.hasFailed
    val showUninstallDialog = uiState.showUninstallDialog
    val hasCopyableError = uiState.hasCopyableError

    val canStartPatching = uiState.canStartPatching
    val canInstall = uiState.canInstall
    val hasInstallError = phase is PatcherPhase.Failed &&
        phase.failedDuring == FailedPhase.INSTALL

    // Handle toast events
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val statusColors = rememberPatcherStatusColors()
    val scrollState = rememberScrollState()

    BackHandler { onNavigateBack() }

    val accentColor by animateColorAsState(
        targetValue = when {
            isInstalled -> statusColors.success
            isPatched && !isInstalling -> statusColors.success
            hasFailed -> statusColors.error
            isPatching || isInstalling || isUninstalling -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(400),
        label = "accentColor"
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (app != null && !isLoadingApp) {
                BottomAppBar {
                    PatcherActionButton(
                        phase = phase,
                        canStartPatching = canStartPatching,
                        canInstall = canInstall,
                        onStartPatching = { viewModel.onEvent(PatcherUiEvent.StartPatching) },
                        onInstall = { viewModel.onEvent(PatcherUiEvent.Install) },
                        onCancel = { viewModel.onEvent(PatcherUiEvent.Cancel) },
                        onRetry = { viewModel.onEvent(PatcherUiEvent.Retry) },
                        onLaunch = { viewModel.onEvent(PatcherUiEvent.LaunchApp) },
                        onNavigateBack = onNavigateBack,
                        statusColors = statusColors,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .navigationBarsPadding()
                    )
                }
            }
        }
    ) { innerPadding ->
        when {
            isLoadingApp -> {
                LoadingState(
                    message = "Loading app details...",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            app == null -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Text(
                        text = "App not found",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState)
                ) {
                    // Compact App Info Header
                    AppInfoHeader(
                        app = app,
                        isPatched = isPatched,
                        hasFailed = hasFailed,
                        statusColors = statusColors
                    )

                    // Status Bar with optional Copy Error button
                    StatusBar(
                        message = uiState.statusMessage,
                        isPatching = isPatching,
                        isPatched = isPatched && !isInstalling && !isInstalled,
                        hasFailed = hasFailed,
                        accentColor = accentColor,
                        statusColors = statusColors,
                        showCopyButton = hasCopyableError,
                        onCopyError = { viewModel.onEvent(PatcherUiEvent.CopyError) },
                        isInstalling = isInstalling,
                        installationProgress = uiState.installationProgress,
                        installationProgressMax = uiState.installationProgressMax
                    )

                    // Process Timeline - always visible
                    PatcherTimeline(
                        currentStep = uiState.currentStep,
                        isPatching = isPatching,
                        isPatched = isPatched,
                        hasFailed = hasFailed,
                        statusColors = statusColors
                    )
                }
            }
        }
    }

    // Uninstall Required Dialog
    if (showUninstallDialog) {
        UninstallRequiredDialog(
            packageName = uiState.conflictingPackageName,
            errorMessage = uiState.error?.message,
            onConfirmUninstall = { viewModel.onEvent(PatcherUiEvent.ConfirmUninstall) },
            onDismiss = { viewModel.onEvent(PatcherUiEvent.DismissUninstallDialog) }
        )
    }
}
