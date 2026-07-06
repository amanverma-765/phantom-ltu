package com.navi.phantom.features.map.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.navi.phantom.features.map.logic.RouteField

@Composable
fun MapTopBar(
    onBackClick: () -> Unit,
    onQueryChange: (RouteField, String) -> Unit,
    onFieldFocused: (RouteField) -> Unit,
    onClearSearch: () -> Unit,
    isSearchMode: Boolean,
    isSearching: Boolean,
    onSearchModeChange: (Boolean) -> Unit,
    routeDistanceText: String? = null,
    routeDurationText: String? = null,
    modifier: Modifier = Modifier
) {
    var originQuery by remember { mutableStateOf("") }
    var destQuery by remember { mutableStateOf("") }
    val originFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            originQuery = ""
            destQuery = ""
            originFocusRequester.requestFocus()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = if (isSearchMode) MaterialTheme.shapes.large else MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 3.dp
    ) {
        if (isSearchMode) {
            Column {
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        originQuery = ""
                        destQuery = ""
                        onSearchModeChange(false)
                        onClearSearch()
                        focusManager.clearFocus()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    RouteTextField(
                        value = originQuery,
                        onValueChange = {
                            originQuery = it
                            onQueryChange(RouteField.ORIGIN, it)
                        },
                        onFocused = { onFieldFocused(RouteField.ORIGIN) },
                        placeholder = "From...",
                        focusRequester = originFocusRequester,
                        focusManager = focusManager,
                        modifier = Modifier.weight(1f)
                    )

                    if (isSearching) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (originQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            originQuery = ""
                            onQueryChange(RouteField.ORIGIN, "")
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(48.dp))

                    RouteTextField(
                        value = destQuery,
                        onValueChange = {
                            destQuery = it
                            onQueryChange(RouteField.DEST, it)
                        },
                        onFocused = { onFieldFocused(RouteField.DEST) },
                        placeholder = "To...",
                        focusRequester = null,
                        focusManager = focusManager,
                        modifier = Modifier.weight(1f)
                    )

                    if (destQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            destQuery = ""
                            onQueryChange(RouteField.DEST, "")
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (routeDistanceText != null || routeDurationText != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = listOfNotNull(routeDistanceText, routeDurationText).joinToString("  •  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSearchModeChange(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pick a location",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { onSearchModeChange(true) }) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocused: () -> Unit,
    placeholder: String,
    focusRequester: FocusRequester?,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocused() },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { focusManager.clearFocus() }
        ),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        }
    )
}
