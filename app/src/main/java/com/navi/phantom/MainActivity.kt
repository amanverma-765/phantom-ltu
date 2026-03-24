package com.navi.phantom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.navi.phantom.core.theme.PhantomTheme
import com.navi.phantom.features.settings.logic.ThemeMode
import com.navi.phantom.features.settings.logic.ThemePreference
import com.navi.phantom.navigation.RootNavDisplay
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val themePreference: ThemePreference by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themePreference.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColor by themePreference.dynamicColor.collectAsState(initial = true)
            PhantomTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                RootNavDisplay()
            }
        }
    }
}
