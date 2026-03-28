package com.navi.phantom.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.navi.phantom.features.settings.logic.SeedColor
import com.navi.phantom.features.settings.logic.ThemeMode
import com.navi.phantom.features.settings.logic.ThemeStyle

@Composable
fun PhantomTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    seedColor: Color = SeedColor.Sakura.color,
    themeStyle: ThemeStyle = ThemeStyle.TonalSpot,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val paletteStyle = when (themeStyle) {
        ThemeStyle.TonalSpot -> PaletteStyle.TonalSpot
        ThemeStyle.Neutral -> PaletteStyle.Neutral
        ThemeStyle.Vibrant -> PaletteStyle.Vibrant
        ThemeStyle.Expressive -> PaletteStyle.Expressive
        ThemeStyle.Rainbow -> PaletteStyle.Rainbow
        ThemeStyle.FruitSalad -> PaletteStyle.FruitSalad
        ThemeStyle.Monochrome -> PaletteStyle.Monochrome
        ThemeStyle.Fidelity -> PaletteStyle.Fidelity
        ThemeStyle.Content -> PaletteStyle.Content
    }

    val colorScheme = rememberDynamicColorScheme(seedColor = seedColor, isDark = darkTheme, style = paletteStyle)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
