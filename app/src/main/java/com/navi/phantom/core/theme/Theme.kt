package com.navi.phantom.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.rememberDynamicColorScheme
import com.navi.phantom.features.settings.logic.SeedColor
import com.navi.phantom.features.settings.logic.ThemeMode

@Composable
fun PhantomTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    seedColor: Color = SeedColor.Sakura.color,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = rememberDynamicColorScheme(seedColor = seedColor, isDark = darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
