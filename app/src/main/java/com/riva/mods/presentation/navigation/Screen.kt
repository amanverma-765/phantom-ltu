package com.riva.mods.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object Home : Screen()

    @Serializable
    data object Store : Screen()

    @Serializable
    data object Manager : Screen()
}