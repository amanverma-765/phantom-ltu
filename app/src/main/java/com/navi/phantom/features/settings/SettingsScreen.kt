package com.navi.phantom.features.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.navi.phantom.BuildConfig
import com.navi.phantom.features.settings.components.SeedColorSelector
import com.navi.phantom.features.settings.components.ThemeStyleSelector
import com.navi.phantom.features.settings.components.SettingsGroup
import com.navi.phantom.features.settings.components.SettingsItem
import com.navi.phantom.features.settings.components.SettingsItemDivider
import com.navi.phantom.features.settings.components.SettingsSectionHeader
import com.navi.phantom.features.settings.components.ThemeModeSelector
import com.navi.phantom.features.settings.logic.SettingsUiEvent
import com.navi.phantom.features.settings.logic.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onDisclaimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(title = {
                Text("Settings", style = MaterialTheme.typography.titleLarge)
            })
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

            SettingsSectionHeader("Appearance")
            ThemeModeSelector(
                selectedMode = uiState.themeMode,
                onModeSelected = { viewModel.onEvent(SettingsUiEvent.SetThemeMode(it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ThemeStyleSelector(
                selectedStyle = uiState.themeStyle,
                onStyleSelected = { viewModel.onEvent(SettingsUiEvent.SetThemeStyle(it)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SeedColorSelector(
                selectedColor = uiState.seedColor,
                onColorSelected = { viewModel.onEvent(SettingsUiEvent.SetSeedColor(it)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                    icon = Icons.Outlined.MailOutline,
                    title = "Feedback",
                    subtitle = "Report bugs or request features",
                    onClick = {
                        val subject = Uri.encode("Phantom LTU Feedback — v${BuildConfig.VERSION_NAME}")
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:akverma4aman@gmail.com?subject=$subject")
                        }
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

            Spacer(modifier = Modifier.height(24.dp))

            SettingsGroup {
                SettingsItem(
                    icon = Icons.Outlined.FavoriteBorder,
                    title = "Sponsor",
                    subtitle = "Support the development",
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/sponsors/amanverma-765")
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
