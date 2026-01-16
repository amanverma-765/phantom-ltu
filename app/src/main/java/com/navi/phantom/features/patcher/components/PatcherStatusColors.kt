package com.navi.phantom.features.bootstrap.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.navi.phantom.core.theme.ErrorRedContainerDark
import com.navi.phantom.core.theme.ErrorRedContainerLight
import com.navi.phantom.core.theme.ErrorRedDark
import com.navi.phantom.core.theme.ErrorRedLight
import com.navi.phantom.core.theme.SuccessGreenContainerDark
import com.navi.phantom.core.theme.SuccessGreenContainerLight
import com.navi.phantom.core.theme.SuccessGreenDark
import com.navi.phantom.core.theme.SuccessGreenLight

data class BootstrapStatusColors(
    val success: Color,
    val successContainer: Color,
    val onSuccess: Color,
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
            onSuccess = Color.White,
            error = if (isDarkTheme) ErrorRedDark else ErrorRedLight,
            errorContainer = if (isDarkTheme) ErrorRedContainerDark else ErrorRedContainerLight
        )
    }
}
