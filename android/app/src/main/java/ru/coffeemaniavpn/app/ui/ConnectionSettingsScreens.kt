package ru.coffeemaniavpn.app.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.coffeemaniavpn.app.data.InstalledAppsLoader
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import ru.coffeemaniavpn.app.data.ConnectionSettingsState
import ru.coffeemaniavpn.app.data.SplitTunnelAppsMode
import ru.coffeemaniavpn.app.data.SplitTunnelSitesMode

data class InstalledAppItem(
    val packageName: String,
    val label: String,
)

@Composable
fun SplitTunnelSitesScreen(
    settings: ConnectionSettingsState,
    onSave: (ConnectionSettingsState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialDomainsText = settings.siteDomains.joinToString("\n")

    var enabled by remember { mutableStateOf(settings.sitesEnabled) }
    var mode by remember { mutableStateOf(settings.sitesMode) }
    var domainsText by remember { mutableStateOf(initialDomainsText) }
    var appliedDomainsText by remember { mutableStateOf(initialDomainsText) }

    LaunchedEffect(
        settings.sitesEnabled,
        settings.sitesMode,
        settings.siteDomains.joinToString("\n"),
    ) {
        enabled = settings.sitesEnabled
        mode = settings.sitesMode
        val saved = settings.siteDomains.joinToString("\n")
        domainsText = saved
        appliedDomainsText = saved
    }

    val domainsDirty = normalizeDomains(domainsText) != normalizeDomains(appliedDomainsText)

    fun parseDomains(text: String): List<String> =
        text.lines().map { it.trim() }.filter { it.isNotBlank() }

    fun applyDomainChanges() {
        val domains = parseDomains(domainsText)
        onSave(
            settings.copy(
                sitesEnabled = enabled,
                sitesMode = mode,
                siteDomains = domains,
            ),
        )
        appliedDomainsText = domainsText
    }

    fun saveToggleAndMode() {
        onSave(
            settings.copy(
                sitesEnabled = enabled,
                sitesMode = mode,
                siteDomains = settings.siteDomains,
            ),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsToggleRow(
            title = "Включить",
            checked = enabled,
            onCheckedChange = {
                enabled = it
                saveToggleAndMode()
            },
        )
        SettingsDivider()

        SettingsSectionLabel(text = "Режим")
        SettingsDivider()

        SettingsRadioRow(
            title = "Только выбранные сайты через VPN",
            selected = mode == SplitTunnelSitesMode.ProxyOnly,
            enabled = enabled,
            onSelect = {
                mode = SplitTunnelSitesMode.ProxyOnly
                saveToggleAndMode()
            },
        )
        SettingsDivider()

        SettingsRadioRow(
            title = "Использовать VPN везде, кроме этих сайтов",
            selected = mode == SplitTunnelSitesMode.DirectBypass,
            enabled = enabled,
            onSelect = {
                mode = SplitTunnelSitesMode.DirectBypass
                saveToggleAndMode()
            },
        )
        SettingsDivider()

        OutlinedTextField(
            value = domainsText,
            onValueChange = { domainsText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            enabled = enabled,
            label = { Text("Сайты (по одному в строке)") },
            placeholder = { Text("example.com\ngoogle.com") },
            minLines = 6,
        )
        SettingsDivider()

        Text(
            text = "Укажите домены без протокола, например youtube.com",
            style = MaterialTheme.typography.bodySmall,
            color = nuboColors().textDim,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        SettingsDivider()

        Button(
            onClick = { applyDomainChanges() },
            enabled = domainsDirty,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = nuboColors().blue,
                contentColor = androidx.compose.ui.graphics.Color.White,
                disabledContainerColor = nuboColors().cardHigh,
                disabledContentColor = nuboColors().textDim,
            ),
        ) {
            Text(if (domainsDirty) "Применить" else "Применено")
        }
    }
}

private fun normalizeDomains(text: String): List<String> =
    text.lines()
        .map { it.trim().lowercase().removePrefix(".") }
        .filter { it.isNotBlank() }

@Composable
fun SplitTunnelAppsScreen(
    settings: ConnectionSettingsState,
    onSave: (ConnectionSettingsState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var enabled by remember(settings) { mutableStateOf(settings.appsEnabled) }
    var mode by remember(settings) { mutableStateOf(settings.appsMode) }
    var selected by remember(settings) { mutableStateOf(settings.appPackages) }
    var apps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var isLoadingApps by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoadingApps = true
        apps = withContext(Dispatchers.Default) {
            InstalledAppsLoader.load(context.packageManager, context.packageName)
        }
        isLoadingApps = false
    }

    fun persist() {
        onSave(
            settings.copy(
                appsEnabled = enabled,
                appsMode = mode,
                appPackages = selected,
            ),
        )
    }

    val filtered = apps.filter {
        query.isBlank() ||
            it.label.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            SettingsToggleRow(
                title = "Включить",
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    persist()
                },
            )
            SettingsDivider()

            SettingsSectionLabel(text = "Режим")
            SettingsDivider()

            SettingsRadioRow(
                title = "Только выбранные приложения через VPN",
                selected = mode == SplitTunnelAppsMode.IncludeOnly,
                enabled = enabled,
                onSelect = {
                    mode = SplitTunnelAppsMode.IncludeOnly
                    persist()
                },
            )
            SettingsDivider()

            SettingsRadioRow(
                title = "Все приложения, кроме выбранных",
                selected = mode == SplitTunnelAppsMode.ExcludeSelected,
                enabled = enabled,
                onSelect = {
                    mode = SplitTunnelAppsMode.ExcludeSelected
                    persist()
                },
            )
            SettingsDivider()

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                enabled = enabled,
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Поиск приложений") },
            )
            SettingsDivider()

            SettingsSectionLabel(
                text = when {
                    isLoadingApps -> "Приложения…"
                    else -> "Приложения (${apps.size})"
                },
            )
            SettingsDivider()
        }

        if (isLoadingApps) {
            Text(
                text = "Загрузка списка…",
                style = MaterialTheme.typography.bodyMedium,
                color = nuboColors().textDim,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
                val checked = app.packageName in selected
                SettingsToggleRow(
                    title = app.label,
                    checked = checked,
                    enabled = enabled,
                    leadingContent = { AppListIcon(packageName = app.packageName) },
                    onCheckedChange = { isChecked ->
                        selected = if (isChecked) {
                            selected + app.packageName
                        } else {
                            selected - app.packageName
                        }
                        persist()
                    },
                )
                if (index < filtered.lastIndex) {
                    SettingsDivider()
                }
            }
        }
    }
}

@Composable
private fun AppListIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap(width = 96, height = 96).asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.size(40.dp),
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = nuboColors().textMain,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        if (leadingContent != null) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingContent()
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    SettingsToggleTitle(title, enabled)
                    SettingsToggleSubtitle(subtitle, enabled)
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                SettingsToggleTitle(title, enabled)
                SettingsToggleSubtitle(subtitle, enabled)
            }
        }
        NuboSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun SettingsToggleTitle(title: String, enabled: Boolean) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = if (enabled) nuboColors().textMain else nuboColors().textDim,
    )
}

@Composable
private fun SettingsToggleSubtitle(subtitle: String?, enabled: Boolean) {
    if (!subtitle.isNullOrBlank()) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = nuboColors().textDim,
        )
    }
}

@Composable
private fun SettingsRadioRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = enabled,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) nuboColors().textMain else nuboColors().textDim,
            modifier = Modifier.padding(start = 4.dp, end = 20.dp),
        )
    }
}

