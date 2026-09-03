package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.coffeemaniavpn.app.data.AppThemeMode
import ru.coffeemaniavpn.app.data.ConnectionSettingsState
import ru.coffeemaniavpn.app.data.DnsMode
import ru.coffeemaniavpn.app.data.SubscriptionAutoUpdateInterval

fun SettingsPage.parentPage(): SettingsPage? = when (this) {
    SettingsPage.SplitTunnelSites,
    SettingsPage.SplitTunnelApps,
    -> SettingsPage.Connection
    SettingsPage.Connection,
    SettingsPage.Subscription,
    SettingsPage.Theme,
    SettingsPage.Logs,
    -> SettingsPage.Main
    SettingsPage.Main -> null
}

enum class SettingsPage(
    val title: String,
    val headerTitle: String = title,
) {
    Main("Настройки"),
    Connection("Соединение"),
    SplitTunnelSites(
        title = "Раздельное туннелирование сайтов",
        headerTitle = "Туннелирование сайтов",
    ),
    SplitTunnelApps(
        title = "Раздельное туннелирование приложений",
        headerTitle = "Туннелирование приложений",
    ),
    Subscription("Подписка"),
    Theme("Тема"),
    Logs("Логи"),
}

@Composable
fun SettingsScreen(
    page: SettingsPage,
    onPageChange: (SettingsPage) -> Unit,
    hasSubscription: Boolean,
    connectionSettings: ConnectionSettingsState,
    onSaveConnectionSettings: (ConnectionSettingsState) -> Unit,
    onPasteLink: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onDeleteSubscription: () -> Unit,
    onBuyOnWebsite: () -> Unit,
    subscriptionAutoUpdateInterval: SubscriptionAutoUpdateInterval,
    onSubscriptionAutoUpdateIntervalChange: (SubscriptionAutoUpdateInterval) -> Unit,
    appThemeMode: AppThemeMode,
    onAppThemeModeChange: (AppThemeMode) -> Unit,
    onShowLogs: () -> Unit,
    onDownloadLogs: () -> Unit,
    onCloseApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (page) {
        SettingsPage.Main -> SettingsMainScreen(
            modifier = modifier,
            onPageChange = onPageChange,
            onCloseApp = onCloseApp,
        )
        SettingsPage.Connection -> ConnectionMenuScreen(
            modifier = modifier,
            connectionSettings = connectionSettings,
            onSaveConnectionSettings = onSaveConnectionSettings,
            onPageChange = onPageChange,
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
        SettingsPage.Theme -> ThemeSettingsScreen(
            modifier = modifier,
            selectedTheme = appThemeMode,
            onThemeChange = onAppThemeModeChange,
        )
        SettingsPage.Subscription -> SubscriptionSettingsScreen(
            modifier = modifier,
            hasSubscription = hasSubscription,
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
                    title = "Просмотр логов",
                    icon = Icons.Default.BugReport,
                    onClick = onShowLogs,
                ),
                SettingsAction(
                    title = "Скачать логи",
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
        color = coffemaniaColors().latte,
    )
}

@Composable
private fun ConnectionMenuScreen(
    connectionSettings: ConnectionSettingsState,
    onSaveConnectionSettings: (ConnectionSettingsState) -> Unit,
    onPageChange: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsNavRow(
            title = "Раздельное туннелирование сайтов",
            icon = Icons.Default.Language,
            onClick = { onPageChange(SettingsPage.SplitTunnelSites) },
        )
        SettingsDivider()
        SettingsNavRow(
            title = "Раздельное туннелирование приложений",
            icon = Icons.Default.Apps,
            onClick = { onPageChange(SettingsPage.SplitTunnelApps) },
        )
        SettingsDivider()
        SettingsToggleRow(
            title = "Kill switch",
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                tint = colors.espresso,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = "Режим DNS",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = colors.espresso,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        DnsMode.entries.forEachIndexed { index, mode ->
            SettingsThemeRadioRow(
                title = mode.label,
                selected = connectionSettings.dnsMode == mode,
                onSelect = {
                    if (connectionSettings.dnsMode != mode) {
                        onSaveConnectionSettings(connectionSettings.copy(dnsMode = mode))
                    }
                },
            )
            if (index < DnsMode.entries.lastIndex) {
                SettingsDivider()
            }
        }
    }
}

@Composable
private fun SettingsMainScreen(
    onPageChange: (SettingsPage) -> Unit,
    onCloseApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsNavRow(
            title = "Соединение",
            icon = Icons.Default.SettingsInputAntenna,
            onClick = { onPageChange(SettingsPage.Connection) },
        )
        SettingsDivider()
        SettingsNavRow(
            title = "Подписка",
            icon = Icons.Default.Link,
            onClick = { onPageChange(SettingsPage.Subscription) },
        )
        SettingsDivider()
        SettingsNavRow(
            title = "Тема",
            icon = Icons.Default.Palette,
            onClick = { onPageChange(SettingsPage.Theme) },
        )
        SettingsDivider()
        SettingsNavRow(
            title = "Логи",
            icon = Icons.Default.BugReport,
            onClick = { onPageChange(SettingsPage.Logs) },
        )
        SettingsDivider()
        SettingsActionRow(
            title = "Закрыть приложение",
            icon = Icons.Default.Cancel,
            onClick = onCloseApp,
            showChevron = false,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubscriptionAutoUpdateButtons(
    selectedInterval: SubscriptionAutoUpdateInterval,
    onIntervalChange: (SubscriptionAutoUpdateInterval) -> Unit,
) {
    val colors = coffemaniaColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = "Автообновление подписки",
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
            SubscriptionAutoUpdateInterval.entries.forEach { interval ->
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
                        text = interval.buttonLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                    )
                }
            }
        }
    }
}

private val SubscriptionAutoUpdateInterval.buttonLabel: String
    get() = when (this) {
        SubscriptionAutoUpdateInterval.OFF -> "Выкл"
        else -> "${hours}ч"
    }

private fun subscriptionItems(
    hasSubscription: Boolean,
    onPasteLink: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onDeleteSubscription: () -> Unit,
    onBuyOnWebsite: () -> Unit,
): List<SettingsAction> = buildList {
    add(
        SettingsAction(
            title = "Купить на сайте",
            icon = Icons.Default.ShoppingCart,
            onClick = onBuyOnWebsite,
        ),
    )
    add(
        SettingsAction(
            title = "Вставить ссылку",
            icon = Icons.Default.ContentPaste,
            onClick = onPasteLink,
        ),
    )
    add(
        SettingsAction(
            title = "Обновить подписку",
            icon = Icons.Default.Refresh,
            onClick = onRefreshSubscription,
            enabled = hasSubscription,
        ),
    )
    if (hasSubscription) {
        add(
            SettingsAction(
                title = "Удалить подписку",
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
    val colors = coffemaniaColors()
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
            style = MaterialTheme.typography.bodyLarge,
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
