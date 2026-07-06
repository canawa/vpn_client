package ru.nubovpn.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ru.nubovpn.app.data.ConnectionSettingsState
import ru.nubovpn.app.data.SubscriptionAutoUpdateInterval
import ru.nubovpn.app.data.SubscriptionInfo

fun SettingsPage.parentPage(): SettingsPage? = when (this) {
    SettingsPage.SplitTunnelSites,
    SettingsPage.SplitTunnelApps,
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
    SplitTunnelSites(
        title = "Раздельное туннелирование сайтов",
        headerTitle = "Туннелирование сайтов",
    ),
    SplitTunnelApps(
        title = "Раздельное туннелирование приложений",
        headerTitle = "Туннелирование приложений",
    ),
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
    subscriptionInfo: SubscriptionInfo?,
    connectionSettings: ConnectionSettingsState,
    onSaveConnectionSettings: (ConnectionSettingsState) -> Unit,
    onPasteLink: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onDeleteSubscription: () -> Unit,
    onTelegramBot: () -> Unit,
    subscriptionAutoUpdateInterval: SubscriptionAutoUpdateInterval,
    onSubscriptionAutoUpdateIntervalChange: (SubscriptionAutoUpdateInterval) -> Unit,
    onShowLogs: () -> Unit,
    onDownloadLogs: () -> Unit,
    onTelegramChannel: () -> Unit,
    onCloseApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (page) {
        SettingsPage.Main -> SettingsMainScreen(
            modifier = modifier,
            appVersion = appVersion,
            hasSubscription = hasSubscription,
            subscriptionInfo = subscriptionInfo,
            connectionSettings = connectionSettings,
            onSaveConnectionSettings = onSaveConnectionSettings,
            onPageChange = onPageChange,
            onTelegramChannel = onTelegramChannel,
            onDeleteSubscription = onDeleteSubscription,
            onCloseApp = onCloseApp,
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
            autoUpdateInterval = subscriptionAutoUpdateInterval,
            onAutoUpdateIntervalChange = onSubscriptionAutoUpdateIntervalChange,
            onPasteLink = onPasteLink,
            onRefreshSubscription = onRefreshSubscription,
            onDeleteSubscription = onDeleteSubscription,
            onTelegramBot = onTelegramBot,
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
            items = listOf(
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
            ),
        )
    }
}

/** Главная страница настроек в стиле NUBO: секции карточек-строк. */
@Composable
private fun SettingsMainScreen(
    appVersion: String,
    hasSubscription: Boolean,
    subscriptionInfo: SubscriptionInfo?,
    connectionSettings: ConnectionSettingsState,
    onSaveConnectionSettings: (ConnectionSettingsState) -> Unit,
    onPageChange: (SettingsPage) -> Unit,
    onTelegramChannel: () -> Unit,
    onDeleteSubscription: () -> Unit,
    onCloseApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubscriptionProfileCard(
            hasSubscription = hasSubscription,
            subscriptionInfo = subscriptionInfo,
            onClick = { onPageChange(SettingsPage.Subscription) },
        )

        SettingsSection(title = "Защита") {
            SettingsToggleCard(
                icon = Icons.Default.Shield,
                title = "Kill Switch",
                subtitle = "Блокировать трафик при обрыве VPN",
                checked = connectionSettings.killSwitchEnabled,
                onCheckedChange = { enabled ->
                    onSaveConnectionSettings(connectionSettings.copy(killSwitchEnabled = enabled))
                },
            )
            SettingsNavCard(
                icon = Icons.Default.Language,
                title = "Туннелирование сайтов",
                subtitle = if (connectionSettings.sitesEnabled) {
                    "Включено · ${connectionSettings.siteDomains.size} сайтов"
                } else {
                    "Выключено"
                },
                onClick = { onPageChange(SettingsPage.SplitTunnelSites) },
            )
            SettingsNavCard(
                icon = Icons.Default.Apps,
                title = "Туннелирование приложений",
                subtitle = if (connectionSettings.appsEnabled) {
                    "Включено · ${connectionSettings.appPackages.size} приложений"
                } else {
                    "Выключено"
                },
                onClick = { onPageChange(SettingsPage.SplitTunnelApps) },
            )
        }

        SettingsSection(title = "Подписка") {
            SettingsNavCard(
                icon = Icons.Default.Update,
                title = "Управление подпиской",
                subtitle = "Автообновление, ссылка, продление",
                onClick = { onPageChange(SettingsPage.Subscription) },
            )
        }

        SettingsSection(title = "Приложение") {
            SettingsNavCard(
                icon = Icons.Default.BugReport,
                title = "Логи",
                subtitle = "Просмотр и выгрузка",
                onClick = { onPageChange(SettingsPage.Logs) },
            )
        }

        SettingsSection(title = "Поддержка") {
            SettingsNavCard(
                icon = Icons.AutoMirrored.Filled.Send,
                title = "Канал в Telegram",
                subtitle = null,
                onClick = onTelegramChannel,
            )
            SettingsNavCard(
                icon = Icons.Default.Info,
                title = "О приложении",
                subtitle = "NUBO VPN v$appVersion",
                onClick = { onPageChange(SettingsPage.About) },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (hasSubscription) {
                SettingsDangerCard(
                    icon = Icons.Default.Delete,
                    title = "Удалить подписку",
                    onClick = onDeleteSubscription,
                )
            }
            SettingsDangerCard(
                icon = Icons.Default.Cancel,
                title = "Закрыть приложение",
                onClick = onCloseApp,
            )
        }
    }
}

@Composable
private fun SubscriptionProfileCard(
    hasSubscription: Boolean,
    subscriptionInfo: SubscriptionInfo?,
    onClick: () -> Unit,
) {
    val colors = nuboColors()
    val shape = RoundedCornerShape(16.dp)
    val expired = subscriptionInfo?.isExpired() == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(colors.cardHigh, colors.card),
                ),
                shape,
            )
            .border(1.dp, colors.borderStrong, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NuboLogoBadge(size = 52.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subscriptionInfo?.takeIf { it.hasTitle }?.title ?: "NUBO VPN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textMain,
                maxLines = 1,
            )
            Text(
                text = when {
                    !hasSubscription -> "Подписка не добавлена"
                    else -> subscriptionInfo?.expireLabel() ?: "Подписка добавлена"
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textDim,
                modifier = Modifier.padding(top = 2.dp),
            )
            SubscriptionBadge(
                hasSubscription = hasSubscription,
                expired = expired,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textFaint,
        )
    }
}

@Composable
private fun SubscriptionBadge(
    hasSubscription: Boolean,
    expired: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    val (text, color) = when {
        !hasSubscription -> "Нет подписки" to colors.textDim
        expired -> "Подписка истекла" to colors.red
        else -> "Подписка активна" to colors.yellow
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Default.WorkspacePremium,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        SectionLabel(text = title)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsRowScaffold(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    danger: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    val colors = nuboColors()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card, shape)
            .border(1.dp, colors.border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (danger) colors.red.copy(alpha = 0.15f) else colors.blue.copy(alpha = 0.15f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (danger) colors.red else colors.blue,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (danger) colors.red else colors.textMain,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textDim,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        trailing()
    }
}

@Composable
private fun SettingsNavCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    val colors = nuboColors()
    SettingsRowScaffold(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textFaint,
        )
    }
}

@Composable
private fun SettingsToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRowScaffold(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = { onCheckedChange(!checked) },
    ) {
        NuboSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsDangerCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    SettingsRowScaffold(
        icon = icon,
        title = title,
        subtitle = null,
        danger = true,
        onClick = onClick,
    ) {}
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = nuboColors().border,
    )
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
    onTelegramBot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = subscriptionItems(
        hasSubscription = hasSubscription,
        onPasteLink = onPasteLink,
        onRefreshSubscription = onRefreshSubscription,
        onDeleteSubscription = onDeleteSubscription,
        onTelegramBot = onTelegramBot,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SubscriptionAutoUpdateExpandable(
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

private const val SETTINGS_EXPAND_ANIM_MS = 280

@Composable
private fun SubscriptionAutoUpdateExpandable(
    selectedInterval: SubscriptionAutoUpdateInterval,
    onIntervalChange: (SubscriptionAutoUpdateInterval) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = nuboColors()
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(SETTINGS_EXPAND_ANIM_MS),
        label = "subscriptionAutoUpdateChevron",
    )
    val expandSpec = tween<IntSize>(SETTINGS_EXPAND_ANIM_MS)
    val fadeSpec = tween<Float>(SETTINGS_EXPAND_ANIM_MS)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Автообновление подписки"
                    stateDescription = if (expanded) "Развёрнуто" else "Свёрнуто"
                }
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Автообновление подписки",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textMain,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = selectedInterval.label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textDim,
                modifier = Modifier.padding(end = 8.dp),
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.textDim,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(chevronRotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = expandSpec,
                expandFrom = Alignment.Top,
            ) + fadeIn(animationSpec = fadeSpec),
            exit = shrinkVertically(
                animationSpec = expandSpec,
                shrinkTowards = Alignment.Top,
            ) + fadeOut(animationSpec = fadeSpec),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SubscriptionAutoUpdateInterval.entries.forEachIndexed { index, interval ->
                    SettingsThemeRadioRow(
                        title = interval.label,
                        selected = selectedInterval == interval,
                        onSelect = {
                            onIntervalChange(interval)
                            expanded = false
                        },
                    )
                    if (index < SubscriptionAutoUpdateInterval.entries.lastIndex) {
                        SettingsDivider()
                    }
                }
            }
        }
    }
}

private fun subscriptionItems(
    hasSubscription: Boolean,
    onPasteLink: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onDeleteSubscription: () -> Unit,
    onTelegramBot: () -> Unit,
): List<SettingsAction> = buildList {
    add(
        SettingsAction(
            title = "Купить в боте",
            icon = Icons.AutoMirrored.Filled.Send,
            onClick = onTelegramBot,
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
private fun SettingsActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
    showChevron: Boolean = true,
) {
    val colors = nuboColors()
    val contentColor = when {
        !enabled -> colors.textDim.copy(alpha = 0.6f)
        destructive -> colors.red
        else -> colors.textMain
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
                tint = colors.textDim,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
