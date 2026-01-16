package com.navi.phantom.di

import com.navi.phantom.features.apps.screens.SelectAppScreen
import com.navi.phantom.features.apps.screens.PatchedAppScreen
import com.navi.phantom.features.apps.logic.AppViewModel
import com.navi.phantom.features.patcher.screens.PatcherScreen
import com.navi.phantom.features.patcher.logic.PatcherUiEvent
import com.navi.phantom.features.patcher.logic.PatcherViewModel
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
    single { Navigator(startDestination = Destination.PatchedApp) }

    navigation<Destination.PatchedApp> {
        PatchedAppScreen(onAddAppClick = { get<Navigator>().navigateTo(Destination.SelectApp) })
    }
    navigation<Destination.Places> {
        LocationScreen(onAddPlaceClick = {})
    }
    navigation<Destination.Setting> {
        SettingsScreen()
    }
    navigation<Destination.SelectApp> {
        val appViewModel = get<AppViewModel>()
        val patcherViewModel = koinActivityViewModel<PatcherViewModel>()
        val navigator = get<Navigator>()

        SelectAppScreen(
            viewModel = appViewModel,
            onNavigateBack = { navigator.goBack() },
            onAppSelected = { app ->
                patcherViewModel.onEvent(PatcherUiEvent.LoadApp(app.packageName))
                navigator.navigateTo(Destination.Patcher)
            }
        )
    }
    navigation<Destination.Patcher> {
        val viewModel = koinActivityViewModel<PatcherViewModel>()
        val navigator = get<Navigator>()

        PatcherScreen(
            viewModel = viewModel,
            onNavigateBack = {
                val phase = viewModel.uiState.value.phase
                if (phase !is com.navi.phantom.features.patcher.logic.PatcherPhase.Ready) {
                    navigator.popTo(Destination.PatchedApp)
                } else {
                    navigator.goBack()
                }
            }
        )
    }
}
