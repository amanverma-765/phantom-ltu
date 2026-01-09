package com.navi.phantom.di

import com.navi.phantom.features.apps.AppPicker
import com.navi.phantom.features.apps.AppScreen
import com.navi.phantom.features.location.LocationScreen
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
        AppScreen(navigateToAppPicker =  { get<Navigator>().navigateTo(Destination.AppPicker) })
    }
    navigation<Destination.Location> {
        LocationScreen()
    }
    navigation<Destination.Setting> {
        SettingScreen()
    }
    navigation<Destination.AppPicker> {
        AppPicker()
    }
}