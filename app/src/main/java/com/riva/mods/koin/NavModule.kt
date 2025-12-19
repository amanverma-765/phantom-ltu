package com.riva.mods.koin

import com.riva.mods.presentation.features.home.HomeScreen
import com.riva.mods.presentation.features.tabs.TabScreen
import com.riva.mods.presentation.navigation.Destination
import com.riva.mods.presentation.navigation.Navigator
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val navModule = module {
    single { Navigator(startDestination = Destination.Tabs) }

    navigation<Destination.Tabs> {
        TabScreen()
    }
}