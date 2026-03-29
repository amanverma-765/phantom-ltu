package com.navi.phantom.features.patcher.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import com.navi.phantom.features.patcher.components.AppInfoHeader
import com.navi.phantom.features.patcher.components.PatcherActionButton
import com.navi.phantom.features.patcher.components.PatcherTimeline
import com.navi.phantom.features.patcher.components.StatusBar
import com.navi.phantom.features.patcher.components.SupportCard
import com.navi.phantom.features.patcher.components.UninstallRequiredDialog
import com.navi.phantom.features.patcher.components.rememberPatcherStatusColors
import com.navi.phantom.features.patcher.logic.PatcherPhase
import com.navi.phantom.features.patcher.logic.PatcherUiEvent
import com.navi.phantom.features.patcher.logic.PatcherViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatcherScreen(
    viewModel: PatcherViewModel,
    packageName: String,
    onNavigateBack: () -> Unit,
    onShowDisclaimer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(packageName) {
        viewModel.onEvent(PatcherUiEvent.LoadApp(packageName))
    }

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        onStartPatching = {
                            if (onShowDisclaimer != null) {
                                onShowDisclaimer()
                            } else {
                                viewModel.onEvent(PatcherUiEvent.StartPatching)
                            }
                        },
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

                    // Support card — appears once patched, stays visible
                    AnimatedVisibility(
                        visible = isPatched,
                        enter = expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = tween(400)
                        ) + fadeIn(animationSpec = tween(400))
                    ) {
                        SupportCard()
                    }

                    // Patch options (only in Ready state)
                    if (phase is PatcherPhase.Ready) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Inject DEX mode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Use if the app crashes after patching",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.injectDex,
                                    onCheckedChange = {
                                        viewModel.onEvent(PatcherUiEvent.ToggleInjectDex(it))
                                    }
                                )
                            }
                        }
                    }

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
                        statusColors = statusColors,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }

    // Uninstall Required Dialog
    if (showUninstallDialog) {
        UninstallRequiredDialog(
            packageName = uiState.conflictingPackageName,
            onConfirmUninstall = { viewModel.onEvent(PatcherUiEvent.ConfirmUninstall) },
            onDismiss = { viewModel.onEvent(PatcherUiEvent.DismissUninstallDialog) }
        )
    }
}