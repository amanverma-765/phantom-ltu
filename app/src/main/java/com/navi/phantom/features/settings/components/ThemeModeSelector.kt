package com.navi.phantom.features.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.navi.phantom.features.settings.logic.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeModeSelector(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = ThemeMode.entries

    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            val label = when (mode) {
                ThemeMode.SYSTEM -> "Auto"
                ThemeMode.LIGHT -> "Light"
                ThemeMode.DARK -> "Dark"
            }
            val icon = when (mode) {
                ThemeMode.SYSTEM -> Icons.Outlined.PhoneAndroid
                ThemeMode.LIGHT -> Icons.Outlined.LightMode
                ThemeMode.DARK -> Icons.Outlined.DarkMode
            }

            SegmentedButton(
                selected = mode == selectedMode,
                onClick = { onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = mode == selectedMode) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                }
            ) {
                Text(text = label)
            }
        }
    }
}
