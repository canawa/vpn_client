package ru.coffeemaniavpn.app.ui

import android.net.TrafficStats
import android.os.Process
import android.os.SystemClock
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import kotlinx.coroutines.delay
import ru.coffeemaniavpn.app.data.SubscriptionInfo
import ru.coffeemaniavpn.app.vpn.VpnStatus

@Composable
fun HomeScreen(
    state: MainUiState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onOpenServers: () -> Unit,
    onSelectNode: (String) -> Unit,
    onRefreshConfig: () -> Unit,
    onPasteLinkClick: () -> Unit,
    onBuyOnWebsiteClick: () -> Unit,
    onRenewTelegramClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    val isConnected = state.vpnStatus == VpnStatus.Started
    val hasSubscription = state.subscriptionUrl.isNotBlank() && state.nodes.isNotEmpty()
    val subscriptionExpired = state.subscriptionInfo?.isExpired() == true
    val canConnect = hasSubscription && !subscriptionExpired
    val connectEnabled = when {
        subscriptionExpired -> isConnected
        else -> isConnected || canConnect
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.startupCrash?.let {
            ErrorBanner(text = "Последний краш: $it")
        }
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
                onRenewTelegramClick = onRenewTelegramClick,
                onRenewWebsiteClick = onBuyOnWebsiteClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!hasSubscription) {
            SubscriptionCard(
                onPasteLinkClick = onPasteLinkClick,
                onBuyOnWebsiteClick = onBuyOnWebsiteClick,
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
            HomeSubscriptionCard(
                uiState = state,
                onRefreshConfig = onRefreshConfig,
                onSelectNode = onSelectNode,
                onOpenServers = onOpenServers,
            )
        }

        WebsiteBanner(onClick = onBuyOnWebsiteClick)
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
    val isConnected = vpnStatus == VpnStatus.Started
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(colors.card, colors.background)),
                shape,
            )
            .border(
                1.dp,
                if (isConnected) colors.borderStrong else colors.border,
                shape,
            ),
    ) {
        if (isConnected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(colors.blue.copy(alpha = 0.12f), Color.Transparent),
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NuboConnectButton(
                vpnStatus = vpnStatus,
                enabled = enabled,
                onClick = onToggleConnect,
            )

            Spacer(modifier = Modifier.height(16.dp))

            val statusColor = when {
                subscriptionExpired && !isConnected -> colors.red
                vpnStatus == VpnStatus.Started -> colors.cyan
                vpnStatus == VpnStatus.Starting -> colors.textMain
                vpnStatus == VpnStatus.Stopping -> colors.textMain
                else -> colors.red
            }
            Text(
                text = if (subscriptionExpired && !isConnected) {
                    "ПОДПИСКА ИСТЕКЛА"
                } else {
                    statusHeadline(vpnStatus)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = statusColor,
            )
            if (isConnected) {
                Text(
                    text = formatConnectionDuration(connectionElapsedMs),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                    ),
                    color = colors.textMain,
                    modifier = Modifier.padding(top = 4.dp),
                )

                val speed by rememberTrafficSpeed(isConnected = true)
                ConnectionSpeedRow(
                    speed = speed,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** Скорость соединения (загрузка/отдача) в байтах в секунду. */
private data class TrafficSpeed(val downBps: Long = 0L, val upBps: Long = 0L)

/**
 * Замеряет скорость трафика приложения раз в секунду, пока VPN подключён.
 * Считает трафик по UID приложения (через него идёт весь туннель);
 * если статистика по UID недоступна, использует общий трафик устройства.
 */
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
    return when {
        kb >= 1024 -> String.format("%.1f МБ/с", kb / 1024.0)
        kb >= 100 -> String.format("%.0f КБ/с", kb)
        else -> String.format("%.1f КБ/с", kb)
    }
}

/** Строка со скоростью загрузки и отдачи под кнопкой подключения. */
@Composable
private fun ConnectionSpeedRow(speed: TrafficSpeed, modifier: Modifier = Modifier) {
    val colors = nuboColors()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "Загрузка",
                tint = colors.cyan,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatSpeed(speed.downBps),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = colors.textMid,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Отдача",
                tint = colors.blue,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formatSpeed(speed.upBps),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = colors.textMid,
            )
        }
    }
}

@Composable
private fun HomeSubscriptionCard(
    uiState: MainUiState,
    onRefreshConfig: () -> Unit,
    onSelectNode: (String) -> Unit,
    onOpenServers: () -> Unit,
) {
    val colors = nuboColors()
    val subscriptionInfo = uiState.subscriptionInfo

    NuboCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Заголовок
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscriptionInfo?.takeIf { it.hasTitle }?.title ?: "NUBO VPN",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textMain,
                        maxLines = 1,
                    )
                    subscriptionInfo?.expireLabel()?.let { expireText ->
                        Text(
                            text = expireText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (subscriptionInfo.isExpired()) colors.red else colors.textDim,
                        )
                    }
                }
                ConfigRefreshButton(
                    isRefreshing = uiState.isLoading,
                    enabled = true,
                    onClick = onRefreshConfig,
                )
            }

            // Трафик
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.cyan),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = subscriptionInfo?.trafficLabel() ?: "— / —",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Трафик",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textDim,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                TrafficProgressBar(subscriptionInfo = subscriptionInfo)
            }

            HorizontalDivider(thickness = 1.dp, color = colors.border)

            // Мини-список серверов
            val selectedId = uiState.selectedNodeId
            val miniNodes = remember(uiState.nodes, selectedId) {
                val selected = uiState.nodes.filter { it.id == selectedId }
                val rest = uiState.nodes.filter { it.id != selectedId }
                (selected + rest).take(5)
            }
            miniNodes.forEachIndexed { index, node ->
                val display = ServerDisplayMapper.map(node, uiState.nodePings[node.id])
                val isSelected = node.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) colors.blue.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { onSelectNode(node.id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) colors.blue.copy(alpha = 0.25f) else colors.blue.copy(alpha = 0.10f),
                            )
                            .border(
                                1.dp,
                                if (isSelected) colors.borderStrong else colors.border,
                                RoundedCornerShape(8.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = display.flag, fontSize = 14.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = display.title +
                                if (display.subtitle.isNotBlank()) " — ${display.subtitle}" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) colors.textMain else colors.textMid,
                            maxLines = 1,
                        )
                        Text(
                            text = display.protocolLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textFaint,
                        )
                    }
                    PingBadge(pingText = display.pingText, pingMs = display.pingMs)
                    SignalBars(pingMs = display.pingMs)
                }
                if (index < miniNodes.lastIndex) {
                    HorizontalDivider(thickness = 1.dp, color = colors.border.copy(alpha = 0.5f))
                }
            }

            HorizontalDivider(thickness = 1.dp, color = colors.border)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenServers)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Все серверы",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.blue,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.blue,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun TrafficProgressBar(
    subscriptionInfo: SubscriptionInfo?,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    val fillBrush = Brush.horizontalGradient(listOf(colors.cyan, colors.blue))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.blue.copy(alpha = 0.12f)),
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
                    .background(fillBrush),
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
            .clip(RoundedCornerShape(8.dp))
            .background(colors.errorContainer)
            .padding(12.dp),
    )
}
