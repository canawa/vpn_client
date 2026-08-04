package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.ui.text.style.TextOverflow
import java.util.Locale
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.PingState
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.vpn.VpnStatus

private sealed interface HomeFilter {
    data object All : HomeFilter
    data class Category(val category: ServerCategory) : HomeFilter
}

@Composable
private fun ServerCategory.label(): String = stringResource(
    when (this) {
        ServerCategory.BYPASS -> R.string.clev_cat_bypass
        ServerCategory.AUTO -> R.string.clev_auto
        ServerCategory.SPEED -> R.string.clev_cat_speed
        ServerCategory.YOUTUBE -> R.string.clev_cat_youtube
        ServerCategory.GAMING -> R.string.clev_cat_gaming
    },
)

@Composable
fun HomeScreen(
    state: MainUiState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onPasteLinkClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectNode: (String) -> Unit,
    onSelectAuto: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRefreshPing: () -> Unit,
    onRefreshConfig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSubscription = state.subscriptionUrl.isNotBlank() && state.nodes.isNotEmpty()
    if (!hasSubscription) {
        ActivationScreen(
            modifier = modifier,
            isLoading = state.isLoading,
            error = state.error,
            onPasteLinkClick = onPasteLinkClick,
        )
        return
    }

    val colors = coffemaniaColors()
    val subscriptionExpired = state.subscriptionInfo?.isExpired() == true
    val isConnected = state.vpnStatus == VpnStatus.Started
    val canConnect = !subscriptionExpired
    val connectEnabled = when {
        subscriptionExpired -> isConnected
        else -> isConnected || canConnect
    }
    val glow = when {
        isConnected -> ConnectUiStatus.On
        state.vpnStatus == VpnStatus.Starting || state.vpnStatus == VpnStatus.Stopping || state.isPinging ->
            ConnectUiStatus.Busy
        else -> ConnectUiStatus.Off
    }

    var filter by remember { mutableStateOf<HomeFilter>(HomeFilter.All) }
    val filteredNodes = remember(state.nodes, filter) {
        when (val f = filter) {
            HomeFilter.All -> state.nodes
            is HomeFilter.Category -> state.nodes.filter { f.category.matches(it.name) }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.milkFoam)) {
        StatusGlow(status = glow)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                ClevLogoFull(
                    logoHeight = 26.dp,
                    modifier = Modifier.align(Alignment.Center),
                )
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.clev_settings),
                        tint = colors.mocha,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            ClevConnectButton(
                vpnStatus = state.vpnStatus,
                connectionElapsedMs = state.connectionElapsedMs,
                isPinging = state.isPinging,
                enabled = connectEnabled,
                onClick = {
                    when {
                        state.isPinging && state.vpnStatus == VpnStatus.Stopped -> onRefreshPing()
                        isConnected -> onDisconnectClick()
                        else -> onConnectClick()
                    }
                },
                size = 158.dp,
            )

            state.error?.takeIf { it.isNotBlank() }?.let { err ->
                Text(
                    text = err,
                    color = colors.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }

            if (subscriptionExpired) {
                Text(
                    text = "Подписка истекла",
                    color = colors.error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            ClevCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Column {
                    state.subscriptionInfo
                        ?.takeIf { it.hasAnnounce }
                        ?.let { info ->
                            Text(
                                text = info.announce,
                                color = colors.espresso,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                            HorizontalDivider(color = colors.latte)
                        }
                    HomeInfoBar(
                        state = state,
                        onRefreshPing = onRefreshPing,
                        onRefreshConfig = onRefreshConfig,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ClevFilterChip(
                    label = stringResource(R.string.clev_all),
                    selected = filter is HomeFilter.All,
                    onClick = { filter = HomeFilter.All },
                )
                ServerCategory.ALL.forEach { category ->
                    ClevFilterChip(
                        label = category.label(),
                        selected = filter == HomeFilter.Category(category),
                        onClick = { filter = HomeFilter.Category(category) },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (filter is HomeFilter.All) {
                    AutoServerRow(
                        selected = state.isAutoSelected,
                        fastest = fastestNode(state),
                        ping = fastestNode(state)?.let { state.nodePings[it.id] },
                        onClick = onSelectAuto,
                    )
                }
                filteredNodes.forEach { node ->
                    val display = ServerDisplayMapper.map(node, state.nodePings[node.id])
                    QuickServerRow(
                        display = display,
                        selected = !state.isAutoSelected && state.selectedNodeId == node.id,
                        favorite = node.id in state.favoriteNodeIds,
                        onClick = { onSelectNode(node.id) },
                        onToggleFavorite = { onToggleFavorite(node.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun ActivationScreen(
    isLoading: Boolean,
    error: String?,
    onPasteLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        ClevLogo(height = 72.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = androidx.compose.ui.text.buildAnnotatedString {
                append("Clev")
                addStyle(
                    androidx.compose.ui.text.SpanStyle(
                        color = colors.espresso,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                    ),
                    0,
                    4,
                )
                append("VPN")
                addStyle(
                    androidx.compose.ui.text.SpanStyle(
                        color = colors.yellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                    ),
                    4,
                    7,
                )
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.clev_tagline),
            color = colors.mocha,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(colors.yellow, colors.amber),
                    ),
                )
                .clickable(enabled = !isLoading, onClick = onPasteLinkClick)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.clev_paste_clipboard),
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }
        error?.let {
            Text(
                text = it,
                color = colors.error,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.clev_key_hint),
            color = colors.mocha,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun HomeInfoBar(
    state: MainUiState,
    onRefreshPing: () -> Unit,
    onRefreshConfig: () -> Unit,
) {
    val colors = coffemaniaColors()
    val info = state.subscriptionInfo
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        YellowCircleIconButton(
            onClick = onRefreshPing,
            enabled = !state.isPinging,
            loading = state.isPinging,
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
        }
        if (info != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "↑↓ ${formatTrafficLabel(info.used, info.total)}",
                color = colors.espresso,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            info.expireCalendarLabel()?.let { expire ->
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = colors.mocha,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = expire,
                    color = colors.mocha,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.width(8.dp))
        YellowCircleIconButton(
            onClick = onRefreshConfig,
            enabled = !state.isLoading,
            loading = state.isLoading,
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AutoServerRow(
    selected: Boolean,
    fastest: ProxyNode?,
    ping: PingState?,
    onClick: () -> Unit,
) {
    val colors = coffemaniaColors()
    val pingMs = (ping as? PingState.Result)?.latencyMs
    val pingText = when (ping) {
        is PingState.Result -> "${ping.latencyMs} мс"
        PingState.Loading -> "…"
        PingState.Unreachable -> "нет"
        null -> "—"
    }
    val subtitle = fastest?.let { ServerDisplayMapper.map(it).title } ?: "Лучший по пингу"
    ServerListCard(
        selected = selected,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⚡", fontSize = 18.sp, color = colors.yellow)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.clev_auto_fastest),
                    color = colors.espresso,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = colors.mocha,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ProtocolLabelBadge(label = fastest?.let {
                if (it.isHysteria2) "Hysteria2" else "VLESS"
            } ?: "VLESS")
            Spacer(modifier = Modifier.width(10.dp))
            PingStatus(pingText = pingText, pingMs = pingMs)
            Spacer(modifier = Modifier.width(10.dp))
            SelectionRadio(selected = selected)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickServerRow(
    display: ServerDisplay,
    selected: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val colors = coffemaniaColors()
    ServerListCard(
        selected = selected,
        onClick = onClick,
        onLongClick = onToggleFavorite,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (favorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (favorite) colors.yellow else colors.latte,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            ServerListFlag(flag = display.flag, height = 18.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = display.title,
                    color = colors.espresso,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (display.subtitle.isNotBlank()) {
                    Text(
                        text = display.subtitle,
                        color = colors.mocha,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            ProtocolLabelBadge(label = display.protocolLabel)
            Spacer(modifier = Modifier.width(8.dp))
            PingStatus(pingText = display.pingText, pingMs = display.pingMs)
            Spacer(modifier = Modifier.width(8.dp))
            SelectionRadio(selected = selected)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerListCard(
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = coffemaniaColors()
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cappuccino)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.yellow else colors.latte,
                shape = shape,
            )
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier.clickable(onClick = onClick)
                },
            ),
    ) {
        content()
    }
}

@Composable
private fun PingStatus(
    pingText: String,
    pingMs: Int?,
) {
    val colors = coffemaniaColors()
    val dotColor = when {
        pingMs != null -> CoffemaniaColors.pingColor(pingMs)
        pingText == "…" -> colors.yellow
        pingText == "нет" -> CoffemaniaColors.PingBad
        else -> colors.mocha
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = pingText,
            color = colors.espresso,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun SelectionRadio(selected: Boolean) {
    val colors = coffemaniaColors()
    Box(
        modifier = Modifier
            .size(18.dp)
            .border(
                width = 1.5.dp,
                color = if (selected) colors.yellow else colors.latte,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(colors.yellow),
            )
        }
    }
}

private fun fastestNode(state: MainUiState): ProxyNode? =
    state.nodes
        .mapNotNull { node ->
            val ping = state.nodePings[node.id] as? PingState.Result ?: return@mapNotNull null
            node to ping.latencyMs
        }
        .minByOrNull { it.second }
        ?.first
