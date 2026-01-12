package com.navi.phantom.features.bootstrap.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.navi.phantom.theme.ErrorRedContainerDark
import com.navi.phantom.theme.ErrorRedContainerLight
import com.navi.phantom.theme.ErrorRedDark
import com.navi.phantom.theme.ErrorRedLight
import com.navi.phantom.theme.SuccessGreenContainerDark
import com.navi.phantom.theme.SuccessGreenContainerLight
import com.navi.phantom.theme.SuccessGreenDark
import com.navi.phantom.theme.SuccessGreenLight

data class BootstrapStatusColors(
    val success: Color,
    val successContainer: Color,
    val error: Color,
    val errorContainer: Color
)

@Composable
fun rememberBootstrapStatusColors(): BootstrapStatusColors {
    val isDarkTheme = isSystemInDarkTheme()
    return remember(isDarkTheme) {
        BootstrapStatusColors(
            success = if (isDarkTheme) SuccessGreenDark else SuccessGreenLight,
            successContainer = if (isDarkTheme) SuccessGreenContainerDark else SuccessGreenContainerLight,
            error = if (isDarkTheme) ErrorRedDark else ErrorRedLight,
            errorContainer = if (isDarkTheme) ErrorRedContainerDark else ErrorRedContainerLight
        )
    }
}
