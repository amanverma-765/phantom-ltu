package com.navi.phantom.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI


@OptIn(KoinExperimentalAPI::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RootNavDisplay(modifier: Modifier = Modifier) {
    val entryProvider = koinEntryProvider<Destination>()
    val navigator = koinInject<Navigator>()
    val currentScreen = navigator.currentScreen()
    val showNavbar = currentScreen?.showNavbar == true

    val bottomPadding by animateDpAsState(
        targetValue = if (showNavbar) 100.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "bottomPadding"
    )

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showNavbar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar {
                    tabItems.forEach { tab ->
                        val isSelected = navigator.currentTab() == tab.destination
                        NavigationBarItem(
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
            transitionSpec = {
                if (navigator.isTabSwitch) NavAnimations.tabTransition
                else NavAnimations.forwardTransition
            },
            popTransitionSpec = {
                if (navigator.isTabSwitch) NavAnimations.tabTransition
                else NavAnimations.backwardTransition
            },
            predictivePopTransitionSpec = { NavAnimations.predictiveBackTransition },
            modifier = modifier
                .fillMaxSize()
                .padding(PaddingValues(bottom = bottomPadding))
                .background(MaterialTheme.colorScheme.background)
        )
    }
}