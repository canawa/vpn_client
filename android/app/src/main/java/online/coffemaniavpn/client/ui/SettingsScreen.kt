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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Send
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

enum class SettingsPage(val title: String) {
    Main("Настройки"),
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
        SettingsGroup {
            SettingsNavRow(
                title = "Серверы",
                icon = Icons.Default.Dns,
                onClick = onOpenServers,
            )
            SettingsNavRow(
                title = "Подписка",
                icon = Icons.Default.Link,
                onClick = { onPageChange(SettingsPage.Subscription) },
            )
        }

        SettingsGroup {
            SettingsNavRow(
                title = "Логи",
                icon = Icons.Default.BugReport,
                onClick = { onPageChange(SettingsPage.Logs) },
            )
        }

        SettingsGroup {
            SettingsNavRow(
                title = "О КОФЕМАНИЯ ВПН",
                icon = Icons.Default.Info,
                onClick = { onPageChange(SettingsPage.About) },
            )
        }

        SettingsGroup {
            SettingsActionRow(
                title = "Закрыть приложение",
                icon = Icons.Default.Cancel,
                onClick = onCloseApp,
                showChevron = false,
            )
        }
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
        SettingsGroup {
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
                    SettingsInnerDivider()
                }
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
        icon = Icons.Default.Send,
        onClick = onTelegramChannel,
    ),
    SettingsAction(
        title = "Купить подписку",
        icon = Icons.Default.ShoppingCart,
        onClick = onBuySubscription,
    ),
)

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        content()
    }
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = CoffemaniaColors.Latte,
    )
}

@Composable
private fun SettingsInnerDivider() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp),
        thickness = 1.dp,
        color = CoffemaniaColors.Latte.copy(alpha = 0.6f),
    )
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
