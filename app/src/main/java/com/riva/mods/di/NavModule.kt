package com.riva.mods.di

import com.riva.mods.presentation.features.auth.LoginScreen
import com.riva.mods.presentation.features.home.HomeScreen
import com.riva.mods.presentation.features.manager.ManagerScreen
import com.riva.mods.presentation.features.store.StoreScreen
import com.riva.mods.presentation.navigation.Navigator
import com.riva.mods.presentation.navigation.Screen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val navModule = module {
    single { Navigator(startDestination = Screen.Login) }

    navigation<Screen.Home> { HomeScreen() }
    navigation<Screen.Store> { StoreScreen() }
    navigation<Screen.Manager> { ManagerScreen() }
    navigation<Screen.Login> { LoginScreen() }
}