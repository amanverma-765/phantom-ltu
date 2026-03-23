package com.navi.phantom.features.places.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.navi.phantom.core.ui.EmptyStateScreen
import com.navi.phantom.features.places.components.PlaceCard
import com.navi.phantom.features.places.logic.PlacesUiEvent
import com.navi.phantom.features.places.logic.PlacesViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    onAddPlaceClick: () -> Unit,
    onEditPlaceClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlacesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isScreenEmpty = uiState.places.isEmpty() && !uiState.isLoading

    val listState = rememberLazyListState()
    val isFabExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(PlacesUiEvent.LoadPlaces)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Saved Places") }
            )
        },
        floatingActionButton = {
            if (!isScreenEmpty) {
                ExtendedFloatingActionButton(
                    onClick = onAddPlaceClick,
                    expanded = isFabExpanded,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add new place"
                        )
                    },
                    text = { Text("Add Place") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isScreenEmpty -> {
                    EmptyStateScreen(
                        icon = Icons.Rounded.BookmarkBorder,
                        title = "No Saved Places",
                        description = "Save your favorite locations for quick access.\nApply them to any app with one tap.",
                        buttonIcon = Icons.Outlined.AddLocationAlt,
                        buttonText = "Save a Place",
                        onButtonClick = onAddPlaceClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
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
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.places,
                            key = { it.id }
                        ) { place ->
                            PlaceCard(
                                place = place,
                                onClick = { onEditPlaceClick(place.id) },
                                onDeleteClick = {
                                    viewModel.onEvent(PlacesUiEvent.DeletePlace(place))
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}