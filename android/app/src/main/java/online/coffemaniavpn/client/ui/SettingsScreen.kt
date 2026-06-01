package online.coffemaniavpn.client.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import online.coffemaniavpn.client.data.ConnectionSettingsState

fun SettingsPage.parentPage(): SettingsPage? = when (this) {
    SettingsPage.SplitTunnelSites,
    SettingsPage.SplitTunnelApps,
    SettingsPage.KillSwitch,
    -> SettingsPage.Connection
    SettingsPage.Connection,
    SettingsPage.Subscription,
    SettingsPage.Logs,
    SettingsPage.About,
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
    KillSwitch("Kill switch"),
    Subscription("Подписка"),
    Logs("Логи"),
    About("О приложении"),
}

@Composable
fun SettingsScreen(
    page: SettingsPage,
    onPageChange: (SettingsPage) -> Unit,
    appVersion: String,
    hasSubscription: Boolean,
    connectionSettings: ConnectionSettingsState,
    onSaveConnectionSettings: (ConnectionSettingsState) -> Unit,
    onOpenServers: () -> Unit,
    onPasteLink: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onDeleteSubscription: () -> Unit,
    onShowLogs: () -> Unit,
    onDownloadLogs: () -> Unit,
    onBuySubscription: () -> Unit,
    onTelegramChannel: () -> Unit,
    onCloseApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (page) {
        SettingsPage.Main -> SettingsMainScreen(
            modifier = modifier,
            onOpenServers = onOpenServers,
            onPageChange = onPageChange,
            onCloseApp = onCloseApp,
        )
        SettingsPage.Connection -> ConnectionMenuScreen(
            modifier = modifier,
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
        SettingsPage.KillSwitch -> KillSwitchScreen(
            modifier = modifier,
            settings = connectionSettings,
            onSave = onSaveConnectionSettings,
        )
        SettingsPage.Subscription -> SettingsDetailScreen(
            modifier = modifier,
            items = subscriptionItems(
                hasSubscription = hasSubscription,
                onPasteLink = onPasteLink,
                onRefreshSubscription = onRefreshSubscription,
                onDeleteSubscription = onDeleteSubscription,
            ),
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
        SettingsPage.About -> SettingsDetailScreen(
            modifier = modifier,
            items = aboutItems(
                appVersion = appVersion,
                onBuySubscription = onBuySubscription,
                onTelegramChannel = onTelegramChannel,
            ),
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = CoffemaniaColors.Latte,
    )
}

@Composable
private fun ConnectionMenuScreen(
    onPageChange: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        SettingsNavRow(
            title = "Kill switch",
            icon = Icons.Default.Shield,
            onClick = { onPageChange(SettingsPage.KillSwitch) },
        )
    }
}

@Composable
private fun SettingsMainScreen(
    onOpenServers: () -> Unit,
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
            title = "Серверы",
            icon = Icons.Default.Dns,
            onClick = onOpenServers,
        )
        SettingsDivider()
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
            title = "Логи",
            icon = Icons.Default.BugReport,
            onClick = { onPageChange(SettingsPage.Logs) },
        )
        SettingsDivider()
        SettingsNavRow(
            title = "О КОФЕМАНИЯ ВПН",
            icon = Icons.Default.Info,
            onClick = { onPageChange(SettingsPage.About) },
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

private fun subscriptionItems(
    hasSubscription: Boolean,
    onPasteLink: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onDeleteSubscription: () -> Unit,
): List<SettingsAction> = buildList {
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

private fun aboutItems(
    appVersion: String,
    onBuySubscription: () -> Unit,
    onTelegramChannel: () -> Unit,
): List<SettingsAction> = listOf(
    SettingsAction(
        title = "Версия $appVersion",
        icon = Icons.Default.Info,
        onClick = {},
        enabled = false,
        showChevron = false,
    ),
    SettingsAction(
        title = "Канал в Telegram",
        icon = Icons.AutoMirrored.Filled.Send,
        onClick = onTelegramChannel,
    ),
    SettingsAction(
        title = "Купить подписку",
        icon = Icons.Default.ShoppingCart,
        onClick = onBuySubscription,
    ),
)

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
    val contentColor = when {
        !enabled -> CoffemaniaColors.Mocha.copy(alpha = 0.5f)
        destructive -> MaterialTheme.colorScheme.error
        else -> CoffemaniaColors.Espresso
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
                tint = CoffemaniaColors.Mocha,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
