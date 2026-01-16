package com.navi.phantom.features.bootstrap.screens

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
import com.navi.phantom.features.bootstrap.components.AppInfoHeader
import com.navi.phantom.features.bootstrap.components.BootstrapActionButton
import com.navi.phantom.features.bootstrap.components.BootstrapTimeline
import com.navi.phantom.features.bootstrap.components.StatusBar
import com.navi.phantom.features.bootstrap.components.UninstallRequiredDialog
import com.navi.phantom.features.bootstrap.components.rememberBootstrapStatusColors
import com.navi.phantom.features.bootstrap.logic.BootstrapPhase
import com.navi.phantom.features.bootstrap.logic.BootstrapUiEvent
import com.navi.phantom.features.bootstrap.logic.BootstrapViewModel
import com.navi.phantom.features.bootstrap.logic.FailedPhase
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BootstrapScreen(
    viewModel: BootstrapViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val app = uiState.app
    val isLoadingApp = uiState.isLoadingApp
    val phase = uiState.phase

    // Derive all UI state from phase
    val isBootstrapping = uiState.isBootstrapping
    val isBootstrapped = uiState.isBootstrapped
    val isInstalling = uiState.isInstalling
    val isInstalled = uiState.isInstalled
    val isUninstalling = uiState.isUninstalling
    val hasFailed = uiState.hasFailed
    val showUninstallDialog = uiState.showUninstallDialog
    val hasCopyableError = uiState.hasCopyableError

    // For action button
    val canStartBootstrap = uiState.canStartBootstrap
    val canInstall = uiState.canInstall
    val hasInstallError = phase is BootstrapPhase.Failed && 
        phase.failedDuring == FailedPhase.INSTALL

    // Handle toast events
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val statusColors = rememberBootstrapStatusColors()
    val scrollState = rememberScrollState()

    BackHandler { onNavigateBack() }

    val accentColor by animateColorAsState(
        targetValue = when {
            isInstalled -> statusColors.success
            isBootstrapped && !isInstalling -> statusColors.success
            hasFailed -> statusColors.error
            isBootstrapping || isInstalling || isUninstalling -> MaterialTheme.colorScheme.tertiary
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
                    BootstrapActionButton(
                        phase = phase,
                        canStartBootstrap = canStartBootstrap,
                        canInstall = canInstall,
                        onStartBootstrap = { viewModel.onEvent(BootstrapUiEvent.StartBootstrap) },
                        onInstall = { viewModel.onEvent(BootstrapUiEvent.Install) },
                        onCancel = { viewModel.onEvent(BootstrapUiEvent.Cancel) },
                        onRetry = { viewModel.onEvent(BootstrapUiEvent.Retry) },
                        onLaunch = { viewModel.onEvent(BootstrapUiEvent.LaunchApp) },
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
                        isBootstrapped = isBootstrapped,
                        hasFailed = hasFailed,
                        statusColors = statusColors
                    )

                    // Status Bar with optional Copy Error button
                    StatusBar(
                        message = uiState.statusMessage,
                        isBootstrapping = isBootstrapping,
                        isBootstrapped = isBootstrapped && !isInstalling && !isInstalled,
                        hasFailed = hasFailed,
                        accentColor = accentColor,
                        statusColors = statusColors,
                        showCopyButton = hasCopyableError,
                        onCopyError = { viewModel.onEvent(BootstrapUiEvent.CopyError) },
                        isInstalling = isInstalling,
                        installationProgress = uiState.installationProgress,
                        installationProgressMax = uiState.installationProgressMax
                    )

                    // Process Timeline - always visible
                    BootstrapTimeline(
                        currentStep = uiState.currentStep,
                        isBootstrapping = isBootstrapping,
                        isBootstrapped = isBootstrapped,
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
            onConfirmUninstall = { viewModel.onEvent(BootstrapUiEvent.ConfirmUninstall) },
            onDismiss = { viewModel.onEvent(BootstrapUiEvent.DismissUninstallDialog) }
        )
    }
}
