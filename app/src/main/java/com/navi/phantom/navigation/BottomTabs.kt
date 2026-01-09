package com.navi.phantom.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.twotone.GetApp
import androidx.compose.material.icons.twotone.LocationOn
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val destination: Destination,
)

val tabItems = listOf(
    BottomTab(
        label = "Apps",
        icon = Icons.Outlined.GetApp,
        selectedIcon = Icons.TwoTone.GetApp,
        destination = Destination.Apps
    ),
    BottomTab(
        label = "Location",
        icon = Icons.Outlined.LocationOn,
        selectedIcon = Icons.TwoTone.LocationOn,
        destination = Destination.Location
    ),
    BottomTab(
        label = "Setting",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.TwoTone.Settings,
        destination = Destination.Setting
    )
)