package com.riva.mods.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.twotone.Extension
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val screen: Screen,
)

val tabItems = listOf(
    BottomTab(
        label = "Store",
        icon = Icons.Outlined.ShoppingBag,
        selectedIcon = Icons.TwoTone.ShoppingBag,
        screen = Screen.Store
    ),
    BottomTab(
        label = "Home",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.TwoTone.Home,
        screen = Screen.Home
    ),
    BottomTab(
        label = "Manager",
        icon = Icons.Outlined.Extension,
        selectedIcon = Icons.TwoTone.Extension,
        screen = Screen.Manager
    )
)