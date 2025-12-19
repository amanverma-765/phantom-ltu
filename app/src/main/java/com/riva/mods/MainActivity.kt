package com.riva.mods

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.riva.mods.presentation.navigation.Navigator
import com.riva.mods.presentation.navigation.TabDestinations
import com.riva.mods.presentation.theme.RivaModsManagerTheme
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RivaModsManagerTheme {
                RivaModsManagerApp()
            }
        }
    }
}

@OptIn(KoinExperimentalAPI::class)
@Composable
fun RivaModsManagerApp(modifier: Modifier = Modifier) {
    val entryProvider = koinEntryProvider()
    val navigator = koinInject<Navigator>()

    NavDisplay(
        backStack = navigator.backStack,
        onBack = { navigator.goBack() },
        entryProvider = entryProvider,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )
}