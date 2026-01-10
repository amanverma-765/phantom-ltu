package com.navi.phantom.features.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.rounded.BookmarkBorder
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
import androidx.compose.ui.unit.dp
import com.navi.phantom.components.EmptyStateScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    modifier: Modifier = Modifier,
    onAddPlaceClick: () -> Unit
) {

    val isScreenEmpty by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Places") }
            )
        },
        floatingActionButton = {
            if (!isScreenEmpty) {
                FloatingActionButton(onClick = onAddPlaceClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Place"
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
                        icon = Icons.Rounded.BookmarkBorder,
                        title = "No Saved Places",
                        description = "Save your favorite locations for quick access.\nApply them to any app with one tap.",
                        buttonIcon = Icons.Outlined.AddLocationAlt,
                        buttonText = "Save a Place",
                        onButtonClick = onAddPlaceClick,
                        modifier = modifier.fillParentMaxSize()
                    )
                }
            }
        }
    }
}