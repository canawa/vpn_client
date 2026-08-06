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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.PingState
import ru.coffeemaniavpn.app.vpn.VpnStatus

/** Фильтры: All / категория. */
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
    onConnectToNode: (String) -> Unit,
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
        state.vpnStatus == VpnStatus.Starting || state.vpnStatus == VpnStatus.Stopping ->
            ConnectUiStatus.Busy
        else -> ConnectUiStatus.Off
    }

    var filter by remember { mutableStateOf<HomeFilter>(HomeFilter.All) }

    val filteredNodes = remember(state.nodes, state.favoriteNodeIds, filter) {
        val nodes = when (val f = filter) {
            HomeFilter.All -> state.nodes
            is HomeFilter.Category -> state.nodes.filter { f.category.matches(it.name) }
        }
        nodes.sortedWith { a, b ->
            val aFav = a.id in state.favoriteNodeIds
            val bFav = b.id in state.favoriteNodeIds
            when {
                aFav && !bFav -> -1
                !aFav && bFav -> 1
                else -> 0
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.milkFoam)) {
        StatusGlow(status = glow)
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 4.dp),
                ) {
                    ClevLogoFull(
                        logoHeight = 22.dp,
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
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                ClevConnectButton(
                    vpnStatus = state.vpnStatus,
                    connectionElapsedMs = state.connectionElapsedMs,
                    enabled = connectEnabled,
                    isPinging = state.isPinging && !isConnected,
                    onClick = {
                        if (isConnected) onDisconnectClick() else onConnectClick()
                    },
                    size = 196.dp,
                    modifier = Modifier.padding(top = 6.dp),
                )

                state.error?.takeIf { it.isNotBlank() }?.let { err ->
                    Text(
                        text = err,
                        color = colors.error,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }

                if (subscriptionExpired) {
                    Text(
                        text = stringResource(R.string.clev_subscription_expired),
                        color = colors.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp),
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
                                SubscriptionAnnounceContent(text = info.announce)
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
                        .padding(horizontal = 20.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ClevFilterChip(
                        label = stringResource(R.string.clev_all),
                        selected = filter is HomeFilter.All,
                        onClick = { filter = HomeFilter.All },
                    )
                    ServerCategory.ALL
                        .filter { it != ServerCategory.AUTO }
                        .forEach { category ->
                            ClevFilterChip(
                                label = category.label(),
                                selected = filter == HomeFilter.Category(category),
                                onClick = { filter = HomeFilter.Category(category) },
                            )
                        }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredNodes, key = { it.id }) { node ->
                    val display = ServerDisplayMapper.map(node, state.nodePings[node.id])
                    QuickServerRow(
                        display = display,
                        selected = state.selectedNodeId == node.id,
                        favorite = node.id in state.favoriteNodeIds,
                        isPinging = state.nodePings[node.id] is PingState.Loading,
                        onClick = { onSelectNode(node.id) },
                        onDoubleClick = { onConnectToNode(node.id) },
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
            text = buildAnnotatedString {
                append("Clev")
                addStyle(
                    SpanStyle(
                        color = colors.espresso,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                    ),
                    0,
                    4,
                )
                append("VPN")
                addStyle(
                    SpanStyle(
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
                .background(Brush.linearGradient(listOf(colors.yellow, colors.amber)))
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
                    fontWeight = FontWeight.Bold,
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
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        InfoBarTrafficCluster(
            onPingClick = onRefreshPing,
            pinging = state.isPinging,
            used = info?.used,
            total = info?.total,
            modifier = Modifier.weight(1f),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            info?.expireCalendarLabel()?.let { expire ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = colors.mocha,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = expire,
                        color = colors.espresso,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            YellowCircleIconButton(
                onClick = onRefreshConfig,
                enabled = !state.isLoading,
                loading = state.isLoading,
                circleSize = 28.dp,
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickServerRow(
    display: ServerDisplay,
    selected: Boolean,
    favorite: Boolean,
    isPinging: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val colors = coffemaniaColors()
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cappuccino)
            .border(
                width = 1.dp,
                color = if (selected) colors.yellow.copy(alpha = 0.65f) else colors.latte,
                shape = shape,
            )
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ServerListFlag(flag = display.flag, height = 18.dp)
            ServerTitleWithProtocolBadge(
                title = display.title,
                protocolLabel = display.protocolLabel,
                favorite = favorite,
                onFavoriteClick = onToggleFavorite,
                modifier = Modifier.weight(1f),
            )
            when {
                isPinging -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = colors.yellow,
                )
                display.pingMs != null -> PingLabel(ms = display.pingMs)
                display.pingText == "—" -> Text(
                    text = "—",
                    color = colors.mocha,
                    fontSize = 10.sp,
                )
                else -> Unit
            }
            ClevSelectionIndicator(selected = selected)
        }
    }
}
