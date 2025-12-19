package com.riva.mods.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.twotone.Extension
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
sealed class Destination {
    @Serializable
    object Tabs : Destination()

}

enum class TabDestinations(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Store("Store", Icons.Outlined.ShoppingBag, Icons.TwoTone.ShoppingBag),
    HOME("Home", Icons.Outlined.Home, Icons.TwoTone.Home),
    Manager("Manager", Icons.Outlined.Extension, Icons.TwoTone.Extension),
}