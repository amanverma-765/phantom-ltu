package com.riva.mods.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen(val showNavbar: Boolean = false) {
    @Serializable
    data object Home : Screen(true)

    @Serializable
    data object Store : Screen(true)

    @Serializable
    data object Manager : Screen(true)

    @Serializable
    data object Login : Screen()
}