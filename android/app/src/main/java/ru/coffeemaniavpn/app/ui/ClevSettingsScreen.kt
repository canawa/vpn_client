package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.AppLanguage
import ru.coffeemaniavpn.app.data.ConnectionSettingsState
import ru.coffeemaniavpn.app.data.SplitTunnelAppsMode
import ru.coffeemaniavpn.app.data.SubscriptionAutoUpdateInterval
import ru.coffeemaniavpn.app.data.TrafficRoutingMode

enum class ClevSettingsTab {
    Apps,
    Rules,
    Subscription,
    Language,
}

@Composable
fun ClevSettingsHost(
    state: MainUiState,
    onClose: () -> Unit,
    onSaveConnectionSettings: (ConnectionSettingsState) -> Unit,
    onPasteLink: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onDeleteSubscription: () -> Unit,
    onSubscriptionAutoUpdateIntervalChange: (SubscriptionAutoUpdateInterval) -> Unit,
    onTrafficRoutingModeChange: (TrafficRoutingMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onOpenAppsEditor: () -> Unit,
    onOpenSitesEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    var tab by remember { mutableStateOf(ClevSettingsTab.Apps) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.clev_settings),
                color = colors.espresso,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null, tint = colors.mocha)
            }
        }

        ClevSegmentedTabs(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        HorizontalDivider(color = colors.latte, modifier = Modifier.padding(top = 10.dp))

        when (tab) {
            ClevSettingsTab.Apps -> ClevAppsTab(
                settings = state.connectionSettings,
                onSave = onSaveConnectionSettings,
                onOpenAppsEditor = onOpenAppsEditor,
            )
            ClevSettingsTab.Rules -> ClevRulesTab(
                settings = state.connectionSettings,
                routingMode = state.trafficRoutingMode,
                onSave = onSaveConnectionSettings,
                onRoutingModeChange = onTrafficRoutingModeChange,
                onOpenSitesEditor = onOpenSitesEditor,
            )
            ClevSettingsTab.Subscription -> ClevSubscriptionTab(
                state = state,
                onPasteLink = onPasteLink,
                onRefresh = onRefreshSubscription,
                onDelete = onDeleteSubscription,
                onAutoUpdateChange = onSubscriptionAutoUpdateIntervalChange,
            )
            ClevSettingsTab.Language -> ClevLanguageTab(
                language = state.appLanguage,
                onChange = onLanguageChange,
            )
        }
    }
}

@Composable
private fun ClevSegmentedTabs(
    selected: ClevSettingsTab,
    onSelect: (ClevSettingsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    val tabs = listOf(
        ClevSettingsTab.Apps to stringResource(R.string.clev_apps),
        ClevSettingsTab.Rules to stringResource(R.string.clev_rules),
        ClevSettingsTab.Subscription to stringResource(R.string.clev_subscription),
        ClevSettingsTab.Language to stringResource(R.string.clev_language),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cappuccino)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEach { (value, label) ->
            val isSelected = selected == value
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.Black else colors.mocha,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(Brush.linearGradient(listOf(colors.yellow, colors.amber)))
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(value) }
                    .padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ClevAppsTab(
    settings: ConnectionSettingsState,
    onSave: (ConnectionSettingsState) -> Unit,
    onOpenAppsEditor: () -> Unit,
) {
    val colors = coffemaniaColors()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Маршрутизация по приложениям", color = colors.espresso, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        ModeCard(
            title = "Выкл",
            subtitle = "Весь трафик следует правилам из вкладки «Правила»",
            selected = !settings.appsEnabled,
            onClick = { onSave(settings.copy(appsEnabled = false)) },
        )
        ModeCard(
            title = "Выбранные в обход VPN",
            subtitle = "Отмеченные приложения идут напрямую, остальное через VPN",
            selected = settings.appsEnabled && settings.appsMode == SplitTunnelAppsMode.ExcludeSelected,
            onClick = {
                onSave(settings.copy(appsEnabled = true, appsMode = SplitTunnelAppsMode.ExcludeSelected))
            },
        )
        ModeCard(
            title = "Только выбранные через VPN",
            subtitle = "Через VPN только отмеченные приложения",
            selected = settings.appsEnabled && settings.appsMode == SplitTunnelAppsMode.IncludeOnly,
            onClick = {
                onSave(settings.copy(appsEnabled = true, appsMode = SplitTunnelAppsMode.IncludeOnly))
            },
        )
        if (settings.appsEnabled) {
            YellowActionButton(
                text = "Выбрать приложения (${settings.appPackages.size})",
                onClick = onOpenAppsEditor,
            )
        }
    }
}

@Composable
private fun ClevRulesTab(
    settings: ConnectionSettingsState,
    routingMode: TrafficRoutingMode,
    onSave: (ConnectionSettingsState) -> Unit,
    onRoutingModeChange: (TrafficRoutingMode) -> Unit,
    onOpenSitesEditor: () -> Unit,
) {
    val colors = coffemaniaColors()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Режим трафика", color = colors.espresso, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        TrafficRoutingMode.entries.forEach { mode ->
            ModeCard(
                title = mode.label,
                subtitle = when (mode) {
                    TrafficRoutingMode.GLOBAL -> "Весь трафик идёт через VPN"
                    TrafficRoutingMode.SMART -> "RU-сайты и локальная сеть мимо VPN"
                    TrafficRoutingMode.CUSTOM -> "Только ваши правила доменов/IP"
                },
                selected = routingMode == mode,
                onClick = {
                    onRoutingModeChange(mode)
                    when (mode) {
                        TrafficRoutingMode.GLOBAL -> onSave(settings.copy(sitesEnabled = false))
                        TrafficRoutingMode.SMART -> onSave(settings.copy(sitesEnabled = true))
                        TrafficRoutingMode.CUSTOM -> onSave(settings.copy(sitesEnabled = true))
                    }
                },
            )
        }

        HorizontalDivider(color = colors.latte)
        Text("Безопасность", color = colors.espresso, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        ClevCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Защита при обрыве", color = colors.espresso, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Блокирует интернет, если VPN оборвался — реальный IP не утечёт.",
                        color = colors.mocha,
                        fontSize = 12.sp,
                    )
                }
                CoffemaniaSwitch(
                    checked = settings.killSwitchEnabled,
                    onCheckedChange = { onSave(settings.copy(killSwitchEnabled = it)) },
                )
            }
        }

        if (routingMode != TrafficRoutingMode.GLOBAL) {
            HorizontalDivider(color = colors.latte)
            Text("Мои правила", color = colors.espresso, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            YellowActionButton(
                text = "Домены / сайты (${settings.siteDomains.size})",
                onClick = onOpenSitesEditor,
            )
        }
    }
}

@Composable
private fun ClevSubscriptionTab(
    state: MainUiState,
    onPasteLink: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    onAutoUpdateChange: (SubscriptionAutoUpdateInterval) -> Unit,
) {
    val colors = coffemaniaColors()
    val info = state.subscriptionInfo
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.clev_subscription), color = colors.espresso, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        ClevCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (info != null) formatTrafficLabel(info.used, info.total) else "— / —",
                    color = colors.espresso,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = info?.expireLabel() ?: "Срок неизвестен",
                    color = colors.mocha,
                    fontSize = 13.sp,
                )
                Text(
                    text = info?.devicesLabel() ?: "Устройств: —",
                    color = colors.mocha,
                    fontSize = 13.sp,
                )
                if (info?.hasAnnounce == true) {
                    HorizontalDivider(color = colors.latte)
                    Text(info.announce, color = colors.espresso, fontSize = 13.sp)
                }
            }
        }
        YellowActionButton(text = "Обновить подписку", onClick = onRefresh)
        YellowActionButton(text = "Вставить новую ссылку", onClick = onPasteLink)
        Text("Автообновление", color = colors.espresso, fontWeight = FontWeight.SemiBold)
        SubscriptionAutoUpdateInterval.entries.forEach { interval ->
            ModeCard(
                title = interval.label,
                subtitle = "",
                selected = state.subscriptionAutoUpdateInterval == interval,
                onClick = { onAutoUpdateChange(interval) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Удалить подписку",
            color = colors.error,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onDelete)
                .padding(14.dp),
        )
    }
}

@Composable
private fun ClevLanguageTab(
    language: AppLanguage,
    onChange: (AppLanguage) -> Unit,
) {
    val colors = coffemaniaColors()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.clev_language), color = colors.espresso, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        ClevCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                AppLanguage.entries.forEachIndexed { index, lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChange(lang) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = language == lang,
                            onClick = { onChange(lang) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.yellow,
                                unselectedColor = colors.mocha,
                            ),
                        )
                        Text(
                            text = when (lang) {
                                AppLanguage.SYSTEM -> stringResource(R.string.clev_lang_system)
                                AppLanguage.RU -> stringResource(R.string.clev_lang_ru)
                                AppLanguage.EN -> stringResource(R.string.clev_lang_en)
                            },
                            color = colors.espresso,
                        )
                    }
                    if (index < AppLanguage.entries.lastIndex) {
                        HorizontalDivider(color = colors.latte)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = coffemaniaColors()
    ClevCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.border(1.dp, colors.yellow, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = colors.espresso, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = colors.mocha, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun YellowActionButton(text: String, onClick: () -> Unit) {
    val colors = coffemaniaColors()
    Text(
        text = text,
        color = Color.Black,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(colors.yellow, colors.amber)))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}
