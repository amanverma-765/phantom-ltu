package com.navi.phantom.features.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.navi.phantom.BuildConfig
import com.navi.phantom.features.settings.components.SettingsGroup
import com.navi.phantom.features.settings.components.SettingsItem
import com.navi.phantom.features.settings.components.SettingsItemDivider
import com.navi.phantom.features.settings.components.SettingsSectionHeader
import com.navi.phantom.features.settings.components.SettingsToggleItem
import com.navi.phantom.features.settings.components.ThemeModeSelector
import com.navi.phantom.features.settings.logic.ThemeMode
import com.navi.phantom.features.settings.logic.ThemePreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDisclaimerClick: () -> Unit,
    modifier: Modifier = Modifier,
    themePreference: ThemePreference
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentTheme by themePreference.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val dynamicColor by themePreference.dynamicColor.collectAsState(initial = true)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Appearance ──────────────────────────────────────
            SettingsSectionHeader("Appearance")
            ThemeModeSelector(
                selectedMode = currentTheme,
                onModeSelected = { mode ->
                    scope.launch { themePreference.setThemeMode(mode) }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SettingsGroup {
                    SettingsToggleItem(
                        icon = Icons.Outlined.Palette,
                        title = "Dynamic colors",
                        subtitle = "Match your wallpaper palette",
                        checked = dynamicColor,
                        onCheckedChange = { enabled ->
                            scope.launch { themePreference.setDynamicColor(enabled) }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── General ─────────────────────────────────────────
            SettingsSectionHeader("General")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.Share,
                    title = "Share Phantom",
                    subtitle = "Spread the word",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out Phantom LTU — GPS location spoofing without root!"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, "Share via"))
                    },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                SettingsItemDivider()
                SettingsItem(
                    icon = Icons.Outlined.Gavel,
                    title = "Disclaimer",
                    subtitle = "Terms and legal information",
                    onClick = onDisclaimerClick,
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── About ───────────────────────────────────────────
            SettingsSectionHeader("About")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    onClick = {
                        val clip = ClipData.newPlainText(
                            "Version",
                            "Phantom LTU v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                        )
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Version copied", Toast.LENGTH_SHORT).show()
                    },
                    trailing = {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy version",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                SettingsItemDivider()
                SettingsItem(
                    icon = Icons.Outlined.Person,
                    title = "Developer",
                    subtitle = "Aman Verma"
                )
                SettingsItemDivider()
                SettingsItem(
                    icon = Icons.Outlined.Code,
                    title = "GitHub",
                    subtitle = "amanverma-765",
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/amanverma-765")
                        )
                        context.startActivity(intent)
                    },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
