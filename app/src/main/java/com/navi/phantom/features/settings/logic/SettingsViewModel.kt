package com.navi.phantom.features.settings.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val themePreference: ThemePreference
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        combine(
            themePreference.themeMode,
            themePreference.seedColor,
            themePreference.themeStyle
        ) { themeMode, seedColor, themeStyle ->
            _uiState.update { it.copy(themeMode = themeMode, seedColor = seedColor, themeStyle = themeStyle) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SetThemeMode -> setThemeMode(event.mode)
            is SettingsUiEvent.SetSeedColor -> setSeedColor(event.color)
            is SettingsUiEvent.SetThemeStyle -> setThemeStyle(event.style)
        }
    }

    private fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themePreference.setThemeMode(mode) }
    }

    private fun setSeedColor(color: SeedColor) {
        viewModelScope.launch { themePreference.setSeedColor(color) }
    }

    private fun setThemeStyle(style: ThemeStyle) {
        viewModelScope.launch { themePreference.setThemeStyle(style) }
    }
}
