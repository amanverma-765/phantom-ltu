package com.navi.phantom.di

import com.navi.phantom.navigation.Navigator
import com.navi.phantom.navigation.Destination
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val navModule = module {
    single { Navigator(startDestination = Destination.Apps) }

    navigation<Destination.Apps> { }
    navigation<Destination.Location> { }
    navigation<Destination.Setting> { }
}