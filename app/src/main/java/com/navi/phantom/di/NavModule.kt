package com.navi.phantom.di

import com.navi.phantom.features.apps.screens.AddAppScreen
import com.navi.phantom.features.apps.screens.AppScreen
import com.navi.phantom.features.apps.logic.AppViewModel
import com.navi.phantom.features.places.LocationScreen
import com.navi.phantom.features.setting.SettingScreen
import com.navi.phantom.navigation.Navigator
import com.navi.phantom.navigation.Destination
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
        AddAppScreen(
            viewModel = get<AppViewModel>(),
            onNavigateBack = { get<Navigator>().goBack() }
        )
    }
}