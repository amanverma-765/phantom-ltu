package com.navi.phantom.di

import com.navi.phantom.features.apps.screens.SelectAppScreen
import com.navi.phantom.features.apps.screens.AppScreen
import com.navi.phantom.features.apps.logic.AppViewModel
import com.navi.phantom.features.bootstrap.screens.BootstrapScreen
import com.navi.phantom.features.bootstrap.logic.BootstrapUiEvent
import com.navi.phantom.features.bootstrap.logic.BootstrapViewModel
import com.navi.phantom.features.places.LocationScreen
import com.navi.phantom.features.settings.SettingsScreen
import com.navi.phantom.navigation.Navigator
import com.navi.phantom.navigation.Destination
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val navModule = module {
    single { Navigator(startDestination = Destination.Apps) }

    navigation<Destination.Apps> {
        AppScreen(onAddAppClick = { get<Navigator>().navigateTo(Destination.SelectApp) })
    }
    navigation<Destination.Places> {
        LocationScreen(onAddPlaceClick = {})
    }
    navigation<Destination.Setting> {
        SettingsScreen()
    }
    navigation<Destination.SelectApp> {
        val appViewModel = get<AppViewModel>()
        val bootstrapViewModel = koinActivityViewModel<BootstrapViewModel>()
        val navigator = get<Navigator>()

        SelectAppScreen(
            viewModel = appViewModel,
            onNavigateBack = { navigator.goBack() },
            onAppSelected = { app ->
                bootstrapViewModel.onEvent(BootstrapUiEvent.LoadApp(app.packageName))
                navigator.navigateTo(Destination.Bootstrap)
            }
        )
    }
    navigation<Destination.Bootstrap> {
        val viewModel = koinActivityViewModel<BootstrapViewModel>()
        val navigator = get<Navigator>()

        BootstrapScreen(
            viewModel = viewModel,
            onNavigateBack = {
                // If we've done any work (not in Ready state), go back to Apps list
                // Otherwise just go back to SelectApp screen
                val phase = viewModel.uiState.value.phase
                if (phase !is com.navi.phantom.features.bootstrap.logic.BootstrapPhase.Ready) {
                    navigator.popTo(Destination.Apps)
                } else {
                    navigator.goBack()
                }
            }
        )
    }
}