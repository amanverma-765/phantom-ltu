package com.navi.phantom.features.apps.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.navi.phantom.R
import com.navi.phantom.components.EmptyStateScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    onAddAppClick: () -> Unit
) {
    val isScreenEmpty by remember { mutableStateOf(true) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) }
            )
        },
        floatingActionButton = {
            if (!isScreenEmpty) {
                FloatingActionButton(onClick = onAddAppClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Apps"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(12.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isScreenEmpty) {
                item {
                    EmptyStateScreen(
                        icon = Icons.Rounded.AppsOutage,
                        title = "No Apps Yet",
                        description = "Add apps to start spoofing their location.\nEach app can have its own fake location.",
                        buttonIcon = Icons.Outlined.AddCircleOutline,
                        buttonText = "Add Your First App",
                        onButtonClick = onAddAppClick,
                        modifier = modifier.fillParentMaxSize()
                    )
                }
            }
        }
    }
}