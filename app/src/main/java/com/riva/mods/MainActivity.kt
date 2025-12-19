package com.riva.mods

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.riva.mods.presentation.navigation.RootNavDisplay
import com.riva.mods.presentation.theme.RivaModsManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RivaModsManagerTheme {
                RootNavDisplay()
            }
        }
    }
}