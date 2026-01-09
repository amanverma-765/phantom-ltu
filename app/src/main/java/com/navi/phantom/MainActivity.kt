package com.navi.phantom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.navi.phantom.navigation.RootNavDisplay
import com.navi.phantom.theme.PhantomGPSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhantomGPSTheme {
                RootNavDisplay()
            }
        }
    }
}