package com.navi.phantom.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Destination(val showNavbar: Boolean = false) {
    @Serializable
    data object PatchedApp : Destination(true)

    @Serializable
    data object Places : Destination(true)

    @Serializable
    data object Setting : Destination(true)

    @Serializable
    data object SelectApp : Destination()

    @Serializable
    data class Patcher(val packageName: String) : Destination()

    @Serializable
    data object MapPicker : Destination()
}
