package com.navi.phantom.di

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.navi.phantom.features.apps.screens.AddAppScreen
import com.navi.phantom.features.apps.screens.AppScreen
import com.navi.phantom.features.apps.screens.PatchingScreen
import com.navi.phantom.features.apps.logic.AppViewModel
import com.navi.phantom.features.apps.logic.PatchingUiEvent
import com.navi.phantom.features.apps.logic.PatchingViewModel
import com.navi.phantom.features.places.LocationScreen
import com.navi.phantom.features.setting.SettingScreen
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
        AppScreen(onAddAppClick = { get<Navigator>().navigateTo(Destination.AppPicker) })
    }
    navigation<Destination.Places> {
        LocationScreen(onAddPlaceClick = {})
    }
    navigation<Destination.Setting> {
        SettingScreen()
    }
    navigation<Destination.AppPicker> {
        val appViewModel = get<AppViewModel>()
        val patchingViewModel = koinActivityViewModel<PatchingViewModel>()
        val navigator = get<Navigator>()

        AddAppScreen(
            viewModel = appViewModel,
            onNavigateBack = { navigator.goBack() },
            onAppSelected = { app ->
                patchingViewModel.onEvent(PatchingUiEvent.LoadApp(app.packageName))
                navigator.navigateTo(Destination.AppPatching)
            }
        )
    }
    navigation<Destination.AppPatching> {
        val viewModel = koinActivityViewModel<PatchingViewModel>()
        val navigator = get<Navigator>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        PatchingScreen(
            app = uiState.app,
            isLoading = uiState.isLoading,
            statusMessage = uiState.errorMessage ?: uiState.statusMessage,
            isPatching = uiState.isPatching,
            isPatchComplete = uiState.isPatchComplete,
            hasPatchFailed = uiState.errorMessage != null,
            hasPatchingAttempted = uiState.hasPatchingAttempted,
            onNavigateBack = {
                if (uiState.hasPatchingAttempted) {
                    navigator.popTo(Destination.Apps)
                } else {
                    navigator.goBack()
                }
            },
            onStartPatching = { viewModel.onEvent(PatchingUiEvent.StartPatching) },
            onInstallClick = { viewModel.onEvent(PatchingUiEvent.InstallPatchedApp) }
        )
    }
}