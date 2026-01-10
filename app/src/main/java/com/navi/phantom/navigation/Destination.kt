package com.navi.phantom.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Destination(val showNavbar: Boolean = true) {
    @Serializable
    data object Apps : Destination()

    @Serializable
    data object Places : Destination()

    @Serializable
    data object Setting : Destination()

    @Serializable
    data object AppPicker : Destination(showNavbar = false)
}