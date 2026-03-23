package com.navi.phantom.di

import com.navi.phantom.features.apps.screens.SelectAppScreen
import com.navi.phantom.features.apps.screens.PatchedAppScreen
import com.navi.phantom.features.apps.logic.AppViewModel
import com.navi.phantom.features.map.screens.MapPickerScreen
import com.navi.phantom.features.patcher.screens.PatcherScreen
import com.navi.phantom.features.patcher.logic.PatcherPhase
import com.navi.phantom.features.patcher.logic.PatcherViewModel
import com.navi.phantom.features.places.logic.PlaceSelectionViewModel
import com.navi.phantom.features.places.screens.PlaceSelectionScreen
import com.navi.phantom.features.places.screens.PlacesScreen
import com.navi.phantom.features.settings.SettingsScreen
import com.navi.phantom.navigation.Navigator
import com.navi.phantom.navigation.Destination
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val navModule = module {
    single { Navigator(startDestination = Destination.PatchedApp) }

    navigation<Destination.PatchedApp> {
        val navigator = get<Navigator>()
        PatchedAppScreen(
            onAddAppClick = { navigator.navigateTo(Destination.SelectApp) },
            onPatchedAppClick = { app ->
                navigator.navigateTo(Destination.SelectPlace(app.packageName))
            }
        )
    }
    navigation<Destination.Places> {
        val navigator = get<Navigator>()
        PlacesScreen(
            onAddPlaceClick = { navigator.navigateTo(Destination.MapPicker()) },
            onEditPlaceClick = { placeId -> navigator.navigateTo(Destination.MapPicker(placeId = placeId)) }
        )
    }
    navigation<Destination.Setting> {
        SettingsScreen()
    }
    navigation<Destination.SelectApp> {
        val viewModel = koinViewModel<AppViewModel>()
        val navigator = get<Navigator>()

        SelectAppScreen(
            viewModel = viewModel,
            onNavigateBack = { navigator.goBack() },
            onAppSelected = { app ->
                navigator.navigateTo(Destination.Patcher(app.packageName))
            }
        )
    }
    navigation<Destination.Patcher> { destination ->
        val viewModel = koinViewModel<PatcherViewModel>()
        val navigator = get<Navigator>()

        PatcherScreen(
            viewModel = viewModel,
            packageName = destination.packageName,
            onNavigateBack = {
                val phase = viewModel.uiState.value.phase
                if (phase !is PatcherPhase.Ready) {
                    navigator.popTo(Destination.PatchedApp)
                } else {
                    navigator.goBack()
                }
            }
        )
    }
    navigation<Destination.MapPicker> { destination ->
        val navigator = get<Navigator>()
        MapPickerScreen(
            onNavigateBack = { navigator.goBack() },
            placeId = destination.placeId
        )
    }
    navigation<Destination.SelectPlace> { destination ->
        val viewModel = koinViewModel<PlaceSelectionViewModel>()
        val navigator = get<Navigator>()
        PlaceSelectionScreen(
            packageName = destination.packageName,
            onNavigateBack = { navigator.goBack() },
            viewModel = viewModel
        )
    }
}
