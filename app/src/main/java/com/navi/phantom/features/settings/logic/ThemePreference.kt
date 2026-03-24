package com.navi.phantom.features.settings.logic

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "phantom_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class SeedColor(val label: String, val color: Color) {
    Sakura("Sakura", Color(0xFFE91E90)),
    Cobalt("Cobalt", Color(0xFF2962FF)),
    Ocean("Ocean", Color(0xFF0277BD)),
    Mint("Mint", Color(0xFF00BFA5)),
    Emerald("Emerald", Color(0xFF00C853)),
    Chartreuse("Chartreuse", Color(0xFF9ACD32)),
    Amber("Amber", Color(0xFFFFAB00)),
    Tangerine("Tangerine", Color(0xFFFF6D00)),
    Coral("Coral", Color(0xFFFF3D00)),
    Crimson("Crimson", Color(0xFFD50000)),
    Magenta("Magenta", Color(0xFFC51162)),
    Graphite("Graphite", Color(0xFF455A64)),
}

class ThemePreference(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val themeKey = stringPreferencesKey("theme_mode")
    private val seedColorKey = stringPreferencesKey("seed_color")

    val themeMode: StateFlow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[themeKey]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }.stateIn(scope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val seedColor: StateFlow<SeedColor> = context.dataStore.data.map { prefs ->
        val name = prefs[seedColorKey]
        SeedColor.entries.find { it.name == name } ?: SeedColor.Sakura
    }.stateIn(scope, SharingStarted.Eagerly, SeedColor.Sakura)

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = mode.name
        }
    }

    suspend fun setSeedColor(color: SeedColor) {
        context.dataStore.edit { prefs ->
            prefs[seedColorKey] = color.name
        }
    }
}
