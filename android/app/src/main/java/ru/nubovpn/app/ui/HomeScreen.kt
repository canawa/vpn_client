package ru.nubovpn.app.ui

import android.net.TrafficStats
import android.os.Process
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import ru.nubovpn.app.data.PingState
import ru.nubovpn.app.data.SubscriptionInfo
import ru.nubovpn.app.vpn.VpnStatus

@Composable
fun HomeScreen(
    state: MainUiState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onSelectNode: (String) -> Unit,
    onRefreshConfig: () -> Unit,
    onPasteLinkClick: () -> Unit,
    onScanQrClick: () -> Unit,
    onTelegramBotClick: () -> Unit,
    onOpenSiteClick: () -> Unit,
    onSubInfoButtonClick: (String) -> Unit,
    onRefreshPing: () -> Unit,
    onToggleSortByPing: () -> Unit,
    onOpenSubscriptionSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    val isConnected = state.vpnStatus == VpnStatus.Started
    val hasSubscription = state.subscriptionUrl.isNotBlank() && state.nodes.isNotEmpty()
    val subscriptionExpired = state.subscriptionInfo?.isExpired() == true
    val connectEnabled = when {
        subscriptionExpired -> isConnected
        else -> isConnected || (hasSubscription && !subscriptionExpired)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.startupCrash?.let { ErrorBanner(text = "Последний краш: $it") }
        state.error?.let { ErrorBanner(text = it) }

        ConnectArea(
            vpnStatus = state.vpnStatus,
            connectionElapsedMs = state.connectionElapsedMs,
            subscriptionExpired = subscriptionExpired,
            enabled = connectEnabled,
            onToggleConnect = {
                if (isConnected) onDisconnectClick() else onConnectClick()
            },
        )

        if (hasSubscription && subscriptionExpired) {
            SubscriptionExpiredCard(
                onTelegramBotClick = onTelegramBotClick,
                onOpenSiteClick = onOpenSiteClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!hasSubscription) {
            SubscriptionCard(
                onPasteLinkClick = onPasteLinkClick,
                onScanQrClick = onScanQrClick,
                onTelegramBotClick = onTelegramBotClick,
                onOpenSiteClick = onOpenSiteClick,
                subscriptionLoad = state.subscriptionLoad,
            )
            state.message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMid,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        } else {
            SubscriptionSummaryCard(
                uiState = state,
                onOpenSubscriptionSettings = onOpenSubscriptionSettings,
                onRefreshConfig = onRefreshConfig,
                onRefreshPing = onRefreshPing,
                onSubInfoButtonClick = onSubInfoButtonClick,
            )
            ServersSection(
                uiState = state,
                onSelectNode = onSelectNode,
                onToggleSortByPing = onToggleSortByPing,
            )
        }
    }
}

@Composable
private fun ConnectArea(
    vpnStatus: VpnStatus,
    connectionElapsedMs: Long,
    subscriptionExpired: Boolean,
    enabled: Boolean,
    onToggleConnect: () -> Unit,
) {
    val colors = nuboColors()
    val view = LocalView.current
    val isConnected = vpnStatus == VpnStatus.Started
    val isBusy = vpnStatus == VpnStatus.Starting || vpnStatus == VpnStatus.Stopping
    var previousStatus by remember { mutableStateOf(vpnStatus) }

    LaunchedEffect(vpnStatus) {
        if (vpnStatus == VpnStatus.Started && previousStatus != VpnStatus.Started) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
        previousStatus = vpnStatus
    }

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card, shape)
            .border(1.dp, colors.border, shape),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NuboConnectButton(
                vpnStatus = vpnStatus,
                enabled = enabled,
                onClick = onToggleConnect,
                diameter = 156.dp,
            )

            Spacer(modifier = Modifier.height(10.dp))

            val statusColor by animateColorAsState(
                targetValue = when {
                    subscriptionExpired && !isConnected -> colors.red
                    isConnected -> colors.green
                    isBusy -> colors.textMain
                    else -> colors.red
                },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                label = "connectStatusColor",
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isConnected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.green),
                    )
                }
                Text(
                    text = if (subscriptionExpired && !isConnected) {
                        "ПОДПИСКА ИСТЕКЛА"
                    } else {
                        statusHeadline(vpnStatus)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                )
            }

            AnimatedVisibility(
                visible = isConnected,
                enter = fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing)),
            ) {
                Text(
                    text = formatConnectionDuration(connectionElapsedMs),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                    ),
                    color = colors.textMid,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            AnimatedVisibility(
                visible = isConnected,
                enter = fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing)),
            ) {
                val speed by rememberTrafficSpeed(isConnected = true)
                ConnectionSpeedPanel(
                    speed = speed,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

private data class TrafficSpeed(val downBps: Long = 0L, val upBps: Long = 0L)

@Composable
private fun rememberTrafficSpeed(isConnected: Boolean): State<TrafficSpeed> {
    val speed = remember { mutableStateOf(TrafficSpeed()) }

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            speed.value = TrafficSpeed()
            return@LaunchedEffect
        }

        val uid = Process.myUid()
        fun readRx(): Long =
            TrafficStats.getUidRxBytes(uid).takeIf { it >= 0 } ?: TrafficStats.getTotalRxBytes()
        fun readTx(): Long =
            TrafficStats.getUidTxBytes(uid).takeIf { it >= 0 } ?: TrafficStats.getTotalTxBytes()

        var lastRx = readRx()
        var lastTx = readTx()
        var lastTime = SystemClock.elapsedRealtime()

        while (true) {
            delay(1_000)
            val rx = readRx()
            val tx = readTx()
            val now = SystemClock.elapsedRealtime()
            val dtMs = (now - lastTime).coerceAtLeast(1)
            speed.value = TrafficSpeed(
                downBps = ((rx - lastRx) * 1_000 / dtMs).coerceAtLeast(0),
                upBps = ((tx - lastTx) * 1_000 / dtMs).coerceAtLeast(0),
            )
            lastRx = rx
            lastTx = tx
            lastTime = now
        }
    }

    return speed
}

private fun formatSpeed(bps: Long): String {
    val kb = bps / 1024.0
    val ru = Locale("ru", "RU")
    return when {
        kb >= 1024 -> String.format(ru, "%.1f МБ/с", kb / 1024.0).replace('.', ',')
        kb >= 100 -> String.format(ru, "%.0f КБ/с", kb).replace('.', ',')
        else -> String.format(ru, "%.1f КБ/с", kb).replace('.', ',')
    }
}

@Composable
private fun ConnectionSpeedPanel(speed: TrafficSpeed, modifier: Modifier = Modifier) {
    val colors = nuboColors()

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpeedColumn(
                icon = Icons.Default.ArrowDownward,
                speed = formatSpeed(speed.downBps),
                label = "СКАЧАТЬ",
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(
                modifier = Modifier.height(42.dp),
                thickness = 1.dp,
                color = colors.border,
            )
            SpeedColumn(
                icon = Icons.Default.ArrowUpward,
                speed = formatSpeed(speed.upBps),
                label = "ЗАГРУЗИТЬ",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SpeedColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    speed: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.blue,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = speed,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.blue,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textDim,
        )
    }
}

@Composable
private fun SubscriptionSummaryCard(
    uiState: MainUiState,
    onOpenSubscriptionSettings: () -> Unit,
    onRefreshConfig: () -> Unit,
    onRefreshPing: () -> Unit,
    onSubInfoButtonClick: (String) -> Unit,
) {
    val colors = nuboColors()
    val info = uiState.subscriptionInfo
    val subscriptionExpired = info?.isExpired() == true
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card, shape)
            .border(1.dp, colors.border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSubscriptionSettings),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.blue),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "N",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info?.takeIf { it.hasTitle }?.title ?: "NUBO VPN",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain,
                    maxLines = 1,
                )
                Text(
                    text = info?.subscriptionStatusLabel() ?: "Подписка активна",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (info?.isExpired() == true) colors.red else colors.textDim,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textDim,
                modifier = Modifier.size(22.dp),
            )
        }

        SubscriptionLoadProgress(
            loadState = uiState.subscriptionLoad,
        )

        info?.let { subscription ->
            if (subscription.hasSubInfoButton || subscription.hasSubInfoText) {
                if (subscription.hasSubInfoButton) {
                    val link = subscription.subInfoButtonLink.trim()
                    SubInfoLinkBanner(
                        text = subscription.subInfoButtonText,
                        enabled = link.isNotBlank(),
                        onClick = { onSubInfoButtonClick(link) },
                    )
                }
                if (subscription.hasSubInfoText) {
                    SubInfoTextBlock(text = subscription.subInfoText)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = subscription.daysRemainingLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = subscription.trafficLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textDim,
                )
            }
            TrafficProgressBar(subscriptionInfo = subscription)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedActionButton(
                text = "ПРОПИНГОВАТЬ",
                icon = Icons.Default.Speed,
                onClick = onRefreshPing,
                enabled = uiState.nodes.isNotEmpty() && !subscriptionExpired,
                isLoading = uiState.isPinging,
                modifier = Modifier.weight(1f),
            )
            FilledActionButton(
                text = "ОБНОВИТЬ",
                icon = Icons.Default.Refresh,
                onClick = onRefreshConfig,
                isLoading = uiState.isLoading,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OutlinedActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val colors = nuboColors()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(colors.backgroundDeep, shape)
            .border(1.dp, colors.border, shape)
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = colors.blue,
            )
        } else {
            Icon(imageVector = icon, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) colors.textMain else colors.textFaint,
        )
    }
}

@Composable
private fun FilledActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(colors.blue, shape)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun ServersSection(
    uiState: MainUiState,
    onSelectNode: (String) -> Unit,
    onToggleSortByPing: () -> Unit,
) {
    val colors = nuboColors()
    val selectedId = uiState.selectedNodeId
    val orderedNodes = remember(uiState.nodes, uiState.sortByPing, uiState.nodePings) {
        if (uiState.sortByPing) {
            uiState.nodes.sortedBy { node ->
                when (val ping = uiState.nodePings[node.id]) {
                    is PingState.Result -> ping.latencyMs
                    else -> Int.MAX_VALUE
                }
            }
        } else {
            uiState.nodes
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "СЕРВЕРЫ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textDim,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.clickable { onToggleSortByPing() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Интеллектуальный выбор",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.sortByPing) colors.green else colors.textDim,
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (uiState.sortByPing) colors.green else colors.textDim,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        orderedNodes.forEach { node ->
            val display = remember(node.id, node.name) {
                ServerDisplayMapper.map(node, ping = null)
            }
            val ping = uiState.nodePings[node.id]
            val isSelected = node.id == selectedId
            HomeServerRow(
                display = display,
                ping = ping,
                selected = isSelected,
                onClick = { onSelectNode(node.id) },
            )
        }
    }
}

@Composable
private fun HomeServerRow(
    display: ServerDisplay,
    ping: PingState?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = nuboColors()
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (selected) colors.blue.copy(alpha = 0.55f) else colors.border
    val backgroundColor = if (selected) colors.blue.copy(alpha = 0.08f) else colors.card

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ServerListFlag(flag = display.flag, height = 28.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = display.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMain,
                maxLines = 1,
            )
            if (display.subtitle.isNotBlank()) {
                Text(
                    text = display.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textDim,
                    maxLines = 1,
                )
            }
        }
        ServerPingIndicator(ping = ping)
        if (selected) {
            SelectedCheck()
        }
    }
}

@Composable
fun TrafficProgressBar(
    subscriptionInfo: SubscriptionInfo?,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.border),
    ) {
        val fraction = when {
            subscriptionInfo == null -> 0f
            subscriptionInfo.isUnlimitedTraffic -> if (subscriptionInfo.used > 0) 0.08f else 0f
            else -> subscriptionInfo.usageFraction.coerceIn(0f, 1f)
        }
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.blue),
            )
        }
    }
}

@Composable
private fun ErrorBanner(text: String) {
    val colors = nuboColors()
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = colors.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.errorContainer)
            .padding(12.dp),
    )
}
