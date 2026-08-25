package work.bavshield.vpn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import work.bavshield.vpn.R
import work.bavshield.vpn.data.AppColorScheme
import work.bavshield.vpn.data.AppThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsScreen(
    selectedTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    selectedColorScheme: AppColorScheme,
    onColorSchemeChange: (AppColorScheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = bavShieldColors()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.theme_section_mode),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.espresso,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        AppThemeMode.entries.forEachIndexed { index, mode ->
            SettingsThemeRadioRow(
                title = stringResource(mode.labelRes),
                selected = selectedTheme == mode,
                onSelect = { onThemeChange(mode) },
            )
            if (index < AppThemeMode.entries.lastIndex) {
                SettingsDivider()
            }
        }

        SettingsDivider()
        Text(
            text = stringResource(R.string.theme_section_color),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.espresso,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppColorScheme.entries.forEach { scheme ->
                ColorSchemeSwatch(
                    scheme = scheme,
                    selected = selectedColorScheme == scheme,
                    onSelect = { onColorSchemeChange(scheme) },
                )
            }
        }
    }
}

@Composable
private fun ColorSchemeSwatch(
    scheme: AppColorScheme,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = bavShieldColors()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(role = Role.RadioButton, onClick = onSelect),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(scheme.swatch)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) colors.espresso else colors.latte,
                    shape = CircleShape,
                ),
        )
        Text(
            text = stringResource(scheme.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) colors.espresso else colors.mocha,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
