package com.navi.phantom.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.twotone.Apps
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
        icon = Icons.Outlined.Apps,
        selectedIcon = Icons.TwoTone.Apps,
        destination = Destination.Apps
    ),
    BottomTab(
        label = "Places",
        icon = Icons.Outlined.LocationOn,
        selectedIcon = Icons.TwoTone.LocationOn,
        destination = Destination.Places
    ),
    BottomTab(
        label = "Setting",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.TwoTone.Settings,
        destination = Destination.Setting
    )
)