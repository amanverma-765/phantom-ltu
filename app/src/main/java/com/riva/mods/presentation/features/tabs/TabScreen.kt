package com.riva.mods.presentation.features.tabs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.riva.mods.presentation.features.home.HomeScreen
import com.riva.mods.presentation.features.manager.ManagerScreen
import com.riva.mods.presentation.features.store.StoreScreen
import com.riva.mods.presentation.navigation.TabDestinations


@Composable
fun TabScreen(modifier: Modifier = Modifier) {
    var currentDestination by rememberSaveable { mutableStateOf(TabDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TabDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            if (currentDestination == it) it.selectedIcon else it.icon,
                            contentDescription = it.label,
                            tint = if (currentDestination == it) {
                                MaterialTheme.colorScheme.primary
                            } else LocalContentColor.current
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Surface(modifier.fillMaxSize()) {
            when (currentDestination) {
                TabDestinations.HOME -> HomeScreen()
                TabDestinations.Store -> StoreScreen()
                TabDestinations.Manager -> ManagerScreen()
            }
        }
    }
}