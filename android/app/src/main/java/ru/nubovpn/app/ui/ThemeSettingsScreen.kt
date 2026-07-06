package ru.nubovpn.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.nubovpn.app.data.AppThemeMode

@Composable
fun ThemeSettingsScreen(
    selectedTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AppThemeMode.entries.forEachIndexed { index, mode ->
            SettingsThemeRadioRow(
                title = mode.label,
                selected = selectedTheme == mode,
                onSelect = { onThemeChange(mode) },
            )
            if (index < AppThemeMode.entries.lastIndex) {
                SettingsDivider()
            }
        }
    }
}
