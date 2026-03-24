package com.navi.phantom.features.settings.logic

sealed interface SettingsUiEvent {
    data class SetThemeMode(val mode: ThemeMode) : SettingsUiEvent
    data class SetSeedColor(val color: SeedColor) : SettingsUiEvent
}
