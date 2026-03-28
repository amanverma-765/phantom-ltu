package com.navi.phantom.features.patcher.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.navi.phantom.core.theme.ErrorRedContainerDark
import com.navi.phantom.core.theme.ErrorRedContainerLight
import com.navi.phantom.core.theme.ErrorRedDark
import com.navi.phantom.core.theme.ErrorRedLight
import com.navi.phantom.core.theme.SuccessGreenContainerDark
import com.navi.phantom.core.theme.SuccessGreenContainerLight
import com.navi.phantom.core.theme.SuccessGreenDark
import com.navi.phantom.core.theme.SuccessGreenLight

data class PatcherStatusColors(
    val success: Color,
    val successContainer: Color,
    val onSuccess: Color,
    val error: Color,
    val errorContainer: Color
)

@Composable
fun rememberPatcherStatusColors(): PatcherStatusColors {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return remember(isDarkTheme) {
        PatcherStatusColors(
            success = if (isDarkTheme) SuccessGreenDark else SuccessGreenLight,
            successContainer = if (isDarkTheme) SuccessGreenContainerDark else SuccessGreenContainerLight,
            onSuccess = Color.White,
            error = if (isDarkTheme) ErrorRedDark else ErrorRedLight,
            errorContainer = if (isDarkTheme) ErrorRedContainerDark else ErrorRedContainerLight
        )
    }
}
