package com.navi.phantom.features.settings.logic

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val seedColor: SeedColor = SeedColor.Sakura
)
