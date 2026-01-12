package com.navi.phantom.features.bootstrap.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.navi.phantom.components.LoadingState
import com.navi.phantom.domain.models.DetailedAppInfo
import com.navi.phantom.features.apps.components.AppInfoCard
import com.navi.phantom.features.bootstrap.components.BootstrapActionButton
import com.navi.phantom.features.bootstrap.components.HeroSection
import com.navi.phantom.features.bootstrap.components.StatusBar
import com.navi.phantom.features.bootstrap.components.rememberBootstrapStatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BootstrapScreen(
    app: DetailedAppInfo?,
    isLoading: Boolean,
    statusMessage: String,
    isBootstrapping: Boolean,
    isBootstrapped: Boolean,
    hasFailed: Boolean,
    hasBootstrapAttempted: Boolean,
    onNavigateBack: () -> Unit,
    onStartBootstrap: () -> Unit,
    onInstallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReady = !isBootstrapping && !isBootstrapped && !hasFailed && !hasBootstrapAttempted
    val statusColors = rememberBootstrapStatusColors()

    BackHandler { onNavigateBack() }

    val accentColor by animateColorAsState(
        targetValue = when {
            isBootstrapped -> statusColors.success
            hasFailed -> statusColors.error
            isBootstrapping -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(400),
        label = "accentColor"
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Bootstrap") },
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
        when {
            isLoading -> {
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    HeroSection(
                        app = app,
                        isBootstrapping = isBootstrapping,
                        isBootstrapped = isBootstrapped,
                        hasFailed = hasFailed,
                        accentColor = accentColor,
                        statusColors = statusColors
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StatusBar(
                        message = statusMessage,
                        isBootstrapping = isBootstrapping,
                        isBootstrapped = isBootstrapped,
                        hasFailed = hasFailed,
                        accentColor = accentColor,
                        statusColors = statusColors
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    AppInfoCard(app = app)

                    Spacer(modifier = Modifier.weight(1f))

                    BootstrapActionButton(
                        isReady = isReady,
                        isBootstrapping = isBootstrapping,
                        isBootstrapped = isBootstrapped,
                        onStartBootstrap = onStartBootstrap,
                        onInstallClick = onInstallClick,
                        onCancel = onNavigateBack
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
