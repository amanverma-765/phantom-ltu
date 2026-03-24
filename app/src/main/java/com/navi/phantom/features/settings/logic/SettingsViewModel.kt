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
            themePreference.seedColor
        ) { themeMode, seedColor ->
            _uiState.update { it.copy(themeMode = themeMode, seedColor = seedColor) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SetThemeMode -> setThemeMode(event.mode)
            is SettingsUiEvent.SetSeedColor -> setSeedColor(event.color)
        }
    }

    private fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themePreference.setThemeMode(mode) }
    }

    private fun setSeedColor(color: SeedColor) {
        viewModelScope.launch { themePreference.setSeedColor(color) }
    }
}
