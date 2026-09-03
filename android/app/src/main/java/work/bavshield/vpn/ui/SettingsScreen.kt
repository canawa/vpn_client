package work.bavshield.vpn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import work.bavshield.vpn.BuildConfig
import work.bavshield.vpn.R
import work.bavshield.vpn.data.AppLanguage
import work.bavshield.vpn.data.ConnectionSettingsState
import work.bavshield.vpn.data.PingAutoInterval
import work.bavshield.vpn.data.PingMethod
import work.bavshield.vpn.data.PingState
import work.bavshield.vpn.data.ServerPinger
import work.bavshield.vpn.data.SubscriptionAutoUpdateInterval
import work.bavshield.vpn.data.SubscriptionInfo
import work.bavshield.vpn.data.formatTrafficBytes

fun SettingsPage.parentPage(): SettingsPage? = when (this) {
    SettingsPage.SplitTunnelSites,
    SettingsPage.SplitTunnelApps,
    -> SettingsPage.Tunnel
    SettingsPage.Language,
    SettingsPage.Connection,
    SettingsPage.Tunnel,
    SettingsPage.Ping,
    SettingsPage.Subscription,
    SettingsPage.Logs,
    SettingsPage.About,
    -> SettingsPage.Main
    SettingsPage.Main -> null
}

enum class SettingsPage(
    @androidx.annotation.StringRes val titleRes: Int,
    @androidx.annotation.StringRes val headerTitleRes: Int = titleRes,
) {
    Main(R.string.settings_title),
    Language(R.string.settings_language),
    Connection(R.string.settings_connection),
    Tunnel(R.string.settings_tunnel),
    SplitTunnelSites(
        titleRes = R.string.settings_split_sites,
        headerTitleRes = R.string.settings_split_sites_header,
    ),
    SplitTunnelApps(
        titleRes = R.string.settings_split_apps,
        headerTitleRes = R.string.settings_split_apps_header,
    ),
    Ping(R.string.settings_ping),
    Subscription(R.string.settings_subscription),
    About(R.string.settings_about),
    Logs(R.string.settings_logs),
}

@Composable
fun SettingsScreen(
    page: SettingsPage,
    onPageChange: (SettingsPage) -> Unit,
    hasSubscription: Boolean,
    subscriptionInfo: work.bavshield.vpn.data.SubscriptionInfo?,
    connectionSettings: ConnectionSettingsState,
    onSaveConnectionSettings: (ConnectionSettingsState) -> Unit,
    onPasteLink: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onDeleteSubscription: () -> Unit,
    onBuyOnWebsite: () -> Unit,
    subscriptionAutoUpdateInterval: SubscriptionAutoUpdateInterval,
    onSubscriptionAutoUpdateIntervalChange: (SubscriptionAutoUpdateInterval) -> Unit,
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    pingAutoInterval: PingAutoInterval,
    onPingAutoIntervalChange: (PingAutoInterval) -> Unit,
    pingTestHosts: String,
    onPingTestHostsChange: (String) -> Unit,
    pingMethod: PingMethod,
    onPingMethodChange: (PingMethod) -> Unit,
    onPingNow: () -> Unit,
    isPinging: Boolean,
    onSiteClick: () -> Unit,
    onTelegramBotClick: () -> Unit,
    onTelegramChannelClick: () -> Unit,
    onSupportClick: () -> Unit,
    onEmailClick: () -> Unit,
    onShowLogs: () -> Unit,
    onDownloadLogs: () -> Unit,
    onCloseApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (page) {
        SettingsPage.Main -> SettingsMainScreen(
            modifier = modifier,
            onPageChange = onPageChange,
        )
        SettingsPage.Language -> LanguageSettingsScreen(
            modifier = modifier,
            selectedLanguage = appLanguage,
            onLanguageChange = onAppLanguageChange,
        )
        SettingsPage.Connection -> ConnectionMenuScreen(
            modifier = modifier,
            connectionSettings = connectionSettings,
            onSaveConnectionSettings = onSaveConnectionSettings,
            pingMethod = pingMethod,
            onPingMethodChange = onPingMethodChange,
            pingAutoInterval = pingAutoInterval,
            onPingAutoIntervalChange = onPingAutoIntervalChange,
        )
        SettingsPage.Tunnel -> TunnelMenuScreen(
            modifier = modifier,
            onPageChange = onPageChange,
        )
        SettingsPage.Ping -> PingSettingsScreen(
            modifier = modifier,
            interval = pingAutoInterval,
            onIntervalChange = onPingAutoIntervalChange,
            testHosts = pingTestHosts,
            onTestHostsChange = onPingTestHostsChange,
            pingMethod = pingMethod,
            onPingMethodChange = onPingMethodChange,
            isPinging = isPinging,
            onPingNow = onPingNow,
        )
        SettingsPage.About -> AboutSettingsScreen(
            modifier = modifier,
            onPageChange = onPageChange,
            onSiteClick = onSiteClick,
            onTelegramBotClick = onTelegramBotClick,
            onTelegramChannelClick = onTelegramChannelClick,
            onSupportClick = onSupportClick,
            onEmailClick = onEmailClick,
        )
        SettingsPage.SplitTunnelSites -> SplitTunnelSitesScreen(
            modifier = modifier,
            settings = connectionSettings,
            onSave = onSaveConnectionSettings,
        )
        SettingsPage.SplitTunnelApps -> SplitTunnelAppsScreen(
            modifier = modifier,
            settings = connectionSettings,
            onSave = onSaveConnectionSettings,
        )
        SettingsPage.Subscription -> SubscriptionSettingsScreen(
            modifier = modifier,
            hasSubscription = hasSubscription,
            subscriptionInfo = subscriptionInfo,
            autoUpdateInterval = subscriptionAutoUpdateInterval,
            onAutoUpdateIntervalChange = onSubscriptionAutoUpdateIntervalChange,
            onPasteLink = onPasteLink,
            onRefreshSubscription = onRefreshSubscription,
            onDeleteSubscription = onDeleteSubscription,
            onBuyOnWebsite = onBuyOnWebsite,
        )
        SettingsPage.Logs -> SettingsDetailScreen(
            modifier = modifier,
            items = listOf(
                SettingsAction(
                    title = stringResource(R.string.settings_view_logs),
                    icon = Icons.Default.BugReport,
                    onClick = onShowLogs,
                ),
                SettingsAction(
                    title = stringResource(R.string.settings_download_logs),
                    icon = Icons.Default.Download,
                    onClick = onDownloadLogs,
                ),
            ),
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = bavShieldColors().latte,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConnectionMenuScreen(
    connectionSettings: ConnectionSettingsState,
    onSaveConnectionSettings: (ConnectionSettingsState) -> Unit,
    pingMethod: PingMethod,
    onPingMethodChange: (PingMethod) -> Unit,
    pingAutoInterval: PingAutoInterval,
    onPingAutoIntervalChange: (PingAutoInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = bavShieldColors()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.settings_connection_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.mocha,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        SettingsDivider()
        SettingsToggleRow(
            title = stringResource(R.string.settings_kill_switch),
            checked = connectionSettings.killSwitchEnabled,
            onCheckedChange = { enabled ->
                onSaveConnectionSettings(connectionSettings.copy(killSwitchEnabled = enabled))
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = colors.espresso,
                    modifier = Modifier.size(24.dp),
                )
            },
        )
        SettingsDivider()
        Text(
            text = stringResource(R.string.ping_method),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = colors.espresso,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        Text(
            text = stringResource(R.string.ping_method_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colors.mocha,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PingMethod.entries.forEach { method ->
                ChoiceChip(
                    selected = pingMethod == method,
                    label = pingMethodLabel(method),
                    onClick = { onPingMethodChange(method) },
                )
            }
        }
        SettingsDivider()
        Text(
            text = stringResource(R.string.ping_interval),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = colors.espresso,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PingAutoInterval.entries.forEach { value ->
                ChoiceChip(
                    selected = pingAutoInterval == value,
                    label = pingIntervalLabel(value),
                    onClick = { onPingAutoIntervalChange(value) },
                )
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val colors = bavShieldColors()
    val background = if (selected) colors.espresso else colors.cappuccino
    val contentColor = if (selected) colors.milkFoam else colors.espresso
    val borderColor = if (selected) colors.espresso else colors.latte
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
    }
}

@Composable
private fun pingMethodLabel(method: PingMethod): String = when (method) {
    PingMethod.TCP -> stringResource(R.string.ping_method_tcp)
    PingMethod.HTTP_GET -> stringResource(R.string.ping_method_http_get)
}

@Composable
private fun TunnelMenuScreen(
    onPageChange: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsNavRow(
            title = stringResource(R.string.settings_split_sites),
            icon = Icons.Default.Language,
            onClick = { onPageChange(SettingsPage.SplitTunnelSites) },
        )
        SettingsDivider()
        SettingsNavRow(
            title = stringResource(R.string.settings_split_apps),
            icon = Icons.Default.Apps,
            onClick = { onPageChange(SettingsPage.SplitTunnelApps) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PingSettingsScreen(
    interval: PingAutoInterval,
    onIntervalChange: (PingAutoInterval) -> Unit,
    testHosts: String,
    onTestHostsChange: (String) -> Unit,
    pingMethod: PingMethod,
    onPingMethodChange: (PingMethod) -> Unit,
    isPinging: Boolean,
    onPingNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = bavShieldColors()
    val scope = rememberCoroutineScope()
    var hostsText by remember(testHosts) { mutableStateOf(testHosts) }
    var testPings by remember { mutableStateOf<Map<String, PingState>>(emptyMap()) }
    val hostsDirty = hostsText != testHosts

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.ping_method),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = colors.espresso,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PingMethod.entries.forEach { method ->
                ChoiceChip(
                    selected = pingMethod == method,
                    label = pingMethodLabel(method),
                    onClick = { onPingMethodChange(method) },
                )
            }
        }
        SettingsDivider()
        Text(
            text = stringResource(R.string.ping_interval),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = colors.espresso,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PingAutoInterval.entries.forEach { value ->
                ChoiceChip(
                    selected = interval == value,
                    label = pingIntervalLabel(value),
                    onClick = { onIntervalChange(value) },
                )
            }
        }

        SettingsActionRow(
            title = stringResource(R.string.ping_now),
            icon = Icons.Default.NetworkPing,
            onClick = onPingNow,
            enabled = !isPinging,
            showChevron = false,
        )
        SettingsDivider()

        Text(
            text = stringResource(R.string.ping_test_hosts),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = colors.espresso,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
        )
        OutlinedTextField(
            value = hostsText,
            onValueChange = { hostsText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            label = { Text(stringResource(R.string.ping_test_hosts_hint)) },
            minLines = 3,
        )
        SettingsActionRow(
            title = stringResource(R.string.action_apply),
            icon = Icons.Default.Refresh,
            onClick = { onTestHostsChange(hostsText) },
            enabled = hostsDirty,
            showChevron = false,
        )
        SettingsActionRow(
            title = stringResource(R.string.ping_test_now),
            icon = Icons.Default.NetworkPing,
            onClick = {
                val hosts = parsePingHosts(hostsText)
                scope.launch {
                    testPings = hosts.associate { (host, port) ->
                        "$host:$port" to PingState.Loading
                    }
                    hosts.forEach { (host, port) ->
                        val key = "$host:$port"
                        val state = ServerPinger.pingHost(host, port, pingMethod)
                        testPings = testPings + (key to state)
                    }
                }
            },
            showChevron = false,
        )
        parsePingHosts(hostsText).forEach { (host, port) ->
            val key = "$host:$port"
            val ping = testPings[key]
            val pingText = when (ping) {
                null -> "—"
                PingState.Loading -> "…"
                is PingState.Result -> "${ping.latencyMs} ms"
                PingState.Unreachable -> "N/A"
            }
            val pingColor = when {
                ping is PingState.Result -> BavShieldColors.pingColor(ping.latencyMs)
                ping == PingState.Unreachable -> BavShieldColors.PingBad
                else -> colors.mocha
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.espresso,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = pingText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = pingColor,
                )
            }
        }
    }
}

@Composable
private fun pingIntervalLabel(interval: PingAutoInterval): String =
    if (interval == PingAutoInterval.OFF) {
        stringResource(R.string.interval_off)
    } else {
        stringResource(R.string.interval_minutes, interval.minutes)
    }

private fun parsePingHosts(raw: String): List<Pair<String, Int>> =
    raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val host = line.substringBefore(':').trim()
            val port = line.substringAfter(':', "443").trim().toIntOrNull() ?: return@mapNotNull null
            if (host.isBlank() || port !in 1..65535) null else host to port
        }
        .toList()

@Composable
private fun AboutSettingsScreen(
    onPageChange: (SettingsPage) -> Unit,
    onSiteClick: () -> Unit,
    onTelegramBotClick: () -> Unit,
    onTelegramChannelClick: () -> Unit,
    onSupportClick: () -> Unit,
    onEmailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsActionRow(
            title = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            icon = Icons.Default.Info,
            onClick = {},
            enabled = false,
            showChevron = false,
        )
        SettingsDivider()
        SettingsActionRow(
            title = BuildConfig.COPYRIGHT,
            icon = Icons.Default.Shield,
            onClick = {},
            enabled = false,
            showChevron = false,
        )
        SettingsDivider()
        SettingsNavRow(
            title = stringResource(R.string.about_site),
            icon = Icons.Default.Language,
            onClick = onSiteClick,
        )
        SettingsDivider()
        SettingsNavRow(
            title = stringResource(R.string.home_open_channel),
            icon = Icons.AutoMirrored.Filled.Send,
            onClick = onTelegramChannelClick,
        )
        SettingsDivider()
        SettingsNavRow(
            title = stringResource(R.string.about_bot),
            icon = Icons.AutoMirrored.Filled.Send,
            onClick = onTelegramBotClick,
        )
        SettingsDivider()
        SettingsNavRow(
            title = stringResource(R.string.about_support),
            icon = Icons.Default.HeadsetMic,
            onClick = onSupportClick,
        )
        SettingsDivider()
        SettingsNavRow(
            title = stringResource(R.string.about_email),
            icon = Icons.Default.Email,
            onClick = onEmailClick,
        )
        SettingsDivider()
        SettingsNavRow(
            title = stringResource(R.string.settings_logs),
            icon = Icons.Default.BugReport,
            onClick = { onPageChange(SettingsPage.Logs) },
        )
    }
}

@Composable
private fun SettingsMainScreen(
    onPageChange: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsNavRow(
            title = stringResource(R.string.settings_language),
            icon = Icons.Default.Translate,
            onClick = { onPageChange(SettingsPage.Language) },
        )
        SettingsDivider()
        SettingsNavRow(
            title = stringResource(R.string.settings_connection),
            icon = Icons.Default.SettingsInputAntenna,
            onClick = { onPageChange(SettingsPage.Connection) },
        )
        SettingsDivider()
        SettingsNavRow(
            title = stringResource(R.string.settings_tunnel),
            icon = Icons.Default.Apps,
            onClick = { onPageChange(SettingsPage.Tunnel) },
        )
        SettingsDivider()
        SettingsNavRow(
            title = stringResource(R.string.settings_subscription),
            icon = Icons.Default.Link,
            onClick = { onPageChange(SettingsPage.Subscription) },
        )
        SettingsDivider()
        SettingsNavRow(
            title = stringResource(R.string.settings_logs),
            icon = Icons.Default.BugReport,
            onClick = { onPageChange(SettingsPage.Logs) },
        )
    }
}

@Composable
private fun SettingsDetailScreen(
    items: List<SettingsAction>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        items.forEachIndexed { index, action ->
            SettingsActionRow(
                title = action.title,
                icon = action.icon,
                onClick = action.onClick,
                enabled = action.enabled,
                destructive = action.destructive,
                showChevron = action.showChevron,
            )
            if (index < items.lastIndex) {
                SettingsDivider()
            }
        }
    }
}

private data class SettingsAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val showChevron: Boolean = false,
)

@Composable
private fun SubscriptionSettingsScreen(
    hasSubscription: Boolean,
    subscriptionInfo: SubscriptionInfo?,
    autoUpdateInterval: SubscriptionAutoUpdateInterval,
    onAutoUpdateIntervalChange: (SubscriptionAutoUpdateInterval) -> Unit,
    onPasteLink: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onDeleteSubscription: () -> Unit,
    onBuyOnWebsite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = subscriptionItems(
        hasSubscription = hasSubscription,
        onPasteLink = onPasteLink,
        onRefreshSubscription = onRefreshSubscription,
        onDeleteSubscription = onDeleteSubscription,
        onBuyOnWebsite = onBuyOnWebsite,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (hasSubscription) {
            SubscriptionInfoPanel(subscriptionInfo = subscriptionInfo)
            SettingsDivider()
        }
        SubscriptionAutoUpdateButtons(
            selectedInterval = autoUpdateInterval,
            onIntervalChange = onAutoUpdateIntervalChange,
        )
        SettingsDivider()

        actions.forEachIndexed { index, action ->
            SettingsActionRow(
                title = action.title,
                icon = action.icon,
                onClick = action.onClick,
                enabled = action.enabled,
                destructive = action.destructive,
                showChevron = action.showChevron,
            )
            if (index < actions.lastIndex) {
                SettingsDivider()
            }
        }
    }
}

@Composable
private fun SubscriptionInfoPanel(
    subscriptionInfo: SubscriptionInfo?,
) {
    val colors = bavShieldColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.subscription_panel_title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = colors.espresso,
        )

        if (subscriptionInfo == null) {
            Text(
                text = stringResource(R.string.subscription_info_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mocha,
                modifier = Modifier.padding(top = 12.dp),
            )
            return
        }

        if (subscriptionInfo.hasTitle) {
            Text(
                text = subscriptionInfo.title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mocha,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            subscriptionInfo.expire <= 0 -> {
                SubscriptionProgressRow(
                    label = stringResource(R.string.subscription_days_left),
                    value = stringResource(R.string.subscription_unlimited),
                    progress = 1f,
                )
            }
            else -> {
                val daysLeft = subscriptionInfo.remainingDays() ?: 0L
                val expired = daysLeft <= 0L
                SubscriptionProgressRow(
                    label = stringResource(R.string.subscription_days_left),
                    value = if (expired) {
                        stringResource(R.string.subscription_expired)
                    } else {
                        stringResource(R.string.subscription_days_count, daysLeft)
                    },
                    progress = subscriptionInfo.daysProgressFraction() ?: 0f,
                    expired = expired,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (subscriptionInfo.isUnlimitedTraffic) {
            Text(
                text = stringResource(R.string.subscription_traffic_unlimited),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mocha,
            )
        } else {
            val remaining = subscriptionInfo.remainingTrafficBytes() ?: 0L
            SubscriptionProgressRow(
                label = stringResource(R.string.subscription_traffic_left),
                value = formatTrafficBytes(remaining),
                progress = subscriptionInfo.remainingTrafficFraction(),
            )
        }
    }
}

@Composable
private fun SubscriptionProgressRow(
    label: String,
    value: String,
    progress: Float,
    expired: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = bavShieldColors()
    val barColor = if (expired) colors.error else colors.espresso
    val fillFraction = progress.coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mocha,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = barColor,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.cappuccino),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fillFraction)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubscriptionAutoUpdateButtons(
    selectedInterval: SubscriptionAutoUpdateInterval,
    onIntervalChange: (SubscriptionAutoUpdateInterval) -> Unit,
) {
    val colors = bavShieldColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.subscription_auto_update),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = colors.espresso,
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SubscriptionAutoUpdateInterval.selectable.forEach { interval ->
                val selected = selectedInterval == interval
                val background = if (selected) colors.espresso else colors.cappuccino
                val contentColor = if (selected) colors.milkFoam else colors.espresso
                val borderColor = if (selected) colors.espresso else colors.latte

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(background)
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { onIntervalChange(interval) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = intervalLabel(interval),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun intervalLabel(interval: SubscriptionAutoUpdateInterval): String =
    if (interval == SubscriptionAutoUpdateInterval.OFF) {
        stringResource(R.string.interval_off)
    } else {
        stringResource(R.string.interval_hours, interval.hours)
    }

@Composable
private fun LanguageSettingsScreen(
    selectedLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsThemeRadioRow(
            title = stringResource(R.string.language_ru),
            selected = selectedLanguage == AppLanguage.RU,
            onSelect = { onLanguageChange(AppLanguage.RU) },
        )
        SettingsDivider()
        SettingsThemeRadioRow(
            title = stringResource(R.string.language_en),
            selected = selectedLanguage == AppLanguage.EN,
            onSelect = { onLanguageChange(AppLanguage.EN) },
        )
    }
}

@Composable
private fun subscriptionItems(
    hasSubscription: Boolean,
    onPasteLink: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onDeleteSubscription: () -> Unit,
    onBuyOnWebsite: () -> Unit,
): List<SettingsAction> = buildList {
    add(
        SettingsAction(
            title = stringResource(R.string.subscription_paste_link),
            icon = Icons.Default.ContentPaste,
            onClick = onPasteLink,
        ),
    )
    add(
        SettingsAction(
            title = stringResource(R.string.subscription_buy_website),
            icon = Icons.Default.ShoppingCart,
            onClick = onBuyOnWebsite,
        ),
    )
    add(
        SettingsAction(
            title = stringResource(R.string.subscription_refresh),
            icon = Icons.Default.Refresh,
            onClick = onRefreshSubscription,
            enabled = hasSubscription,
        ),
    )
    if (hasSubscription) {
        add(
            SettingsAction(
                title = stringResource(R.string.subscription_delete),
                icon = Icons.Default.Delete,
                onClick = onDeleteSubscription,
                destructive = true,
            ),
        )
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    SettingsActionRow(
        title = title,
        icon = icon,
        onClick = onClick,
        showChevron = true,
    )
}

@Composable
private fun SettingsActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
    showChevron: Boolean = true,
) {
    val colors = bavShieldColors()
    val contentColor = when {
        !enabled -> colors.mocha.copy(alpha = 0.5f)
        destructive -> MaterialTheme.colorScheme.error
        else -> colors.espresso
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        )
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.mocha,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
