package com.navi.phantom.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Destination(val showNavbar: Boolean = false) {
    @Serializable
    data object Apps : Destination(true)

    @Serializable
    data object Location : Destination(true)

    @Serializable
    data object Setting : Destination(true)
}