package com.navi.phantom.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Destination(val showNavbar: Boolean = false) {
    @Serializable
    data object Apps : Destination(true)

    @Serializable
    data object Places : Destination(true)

    @Serializable
    data object Setting : Destination(true)

    @Serializable
    data object SelectApp : Destination()

    @Serializable
    data object Bootstrap : Destination()
}