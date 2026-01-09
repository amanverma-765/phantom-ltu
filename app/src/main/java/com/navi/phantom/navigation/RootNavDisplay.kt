package com.navi.phantom.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI


@OptIn(KoinExperimentalAPI::class)
@Composable
fun RootNavDisplay(modifier: Modifier = Modifier) {
    val entryProvider = koinEntryProvider()
    val navigator = koinInject<Navigator>()
    val currentScreen = navigator.currentScreen()

    NavigationSuiteScaffold(
        layoutType = if (currentScreen?.showNavbar == true) {
            NavigationSuiteType.NavigationBar
        } else {
            NavigationSuiteType.None
        },
        navigationSuiteItems = {
            tabItems.forEach { tab ->
                val isSelected = navigator.currentTab() == tab.destination
                item(
                    icon = {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                LocalContentColor.current
                            }
                        )
                    },
                    label = { Text(tab.label) },
                    selected = isSelected,
                    onClick = { navigator.switchTab(tab.destination) }
                )
            }
        }
    ) {
        NavDisplay(
            backStack = navigator.backStack,
            onBack = navigator::goBack,
            entryProvider = entryProvider,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            transitionSpec = { NavAnimations.forwardTransition },
            popTransitionSpec = { NavAnimations.backwardTransition },
            predictivePopTransitionSpec = { NavAnimations.predictiveBackTransition },
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
    }
}