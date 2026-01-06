package com.riva.mods

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.riva.mods.core.auth.AuthEventBus
import com.riva.mods.core.storage.TokenManager
import com.riva.mods.core.utils.toastLong
import com.riva.mods.presentation.navigation.Navigator
import com.riva.mods.presentation.navigation.RootNavDisplay
import com.riva.mods.presentation.navigation.Screen
import com.riva.mods.presentation.theme.RivaModsManagerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    private val tokenManager: TokenManager by inject()
    private val navigator: Navigator by inject()
    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !isReady }

        lifecycleScope.launch {
            val isLoggedIn = tokenManager.isLoggedIn.first()
            if (isLoggedIn) {
                navigator.switchTab(Screen.Home)
            }
            isReady = true
        }

        enableEdgeToEdge()
        setContent {
            RivaModsManagerTheme {
                val context = LocalContext.current
                val nav = koinInject<Navigator>()

                LaunchedEffect(Unit) {
                    AuthEventBus.events.collect { event ->
                        when (event) {
                            is AuthEventBus.AuthEvent.SessionExpired -> {
                                context.toastLong("Session expired. Please login again.")
                                nav.navigateTo(Screen.Login)
                            }
                            is AuthEventBus.AuthEvent.LogoutRequested -> {
                                nav.navigateTo(Screen.Login)
                            }
                        }
                    }
                }

                RootNavDisplay()
            }
        }
    }
}