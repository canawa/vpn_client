package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import android.content.Intent
import android.net.Uri
import ru.coffeemaniavpn.app.BuildConfig
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.HomeFilterOrder
import ru.coffeemaniavpn.app.data.PingState
import ru.coffeemaniavpn.app.data.SubscriptionInfo
import ru.coffeemaniavpn.app.vpn.VpnStatus
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
private fun ServerCategory.label(): String = stringResource(
    when (this) {
        ServerCategory.BYPASS -> R.string.clev_cat_bypass
        ServerCategory.AUTO -> R.string.clev_auto
    },
)

@Composable
private fun homeFilterLabel(id: String): String = when (id) {
    HomeFilterOrder.ALL_ID -> stringResource(R.string.clev_all)
    else -> ServerCategory.entries.find { it.name == id }?.label() ?: id
}

@Composable
fun HomeScreen(
    state: MainUiState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onPasteLinkClick: () -> Unit,
    onScanQrClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectNode: (String) -> Unit,
    onConnectToNode: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPingNode: (String) -> Unit,
    onRefreshPing: () -> Unit,
    onRefreshConfig: () -> Unit,
    onReorderFilters: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSubscription = state.subscriptionUrl.isNotBlank()
    if (!hasSubscription) {
        ActivationScreen(
            modifier = modifier,
            isLoading = state.isLoading,
            error = state.error,
            onPasteLinkClick = onPasteLinkClick,
            onScanQrClick = onScanQrClick,
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
    var filterOrder by remember { mutableStateOf(state.homeFilterOrder) }
    var isReorderingFilters by remember { mutableStateOf(false) }
    LaunchedEffect(state.homeFilterOrder) {
        if (!isReorderingFilters) {
            filterOrder = state.homeFilterOrder
        }
    }

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

    val filterListState = rememberLazyListState()
    val reorderableFilterState = rememberReorderableLazyListState(filterListState) { from, to ->
        filterOrder = filterOrder.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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
                        logoHeight = 68.dp,
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
                    onClick = {
                        if (isConnected) onDisconnectClick() else onConnectClick()
                    },
                    size = 140.dp,
                    modifier = Modifier.padding(top = 2.dp),
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

                // Компактная панель подписки — больше места под список серверов.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val screenW = maxWidth
                    val outerMargin = screenW * 0.05f
                    val outerVertical = screenW * 0.012f

                    HomeSubscriptionCard(
                        state = state,
                        onRefreshPing = onRefreshPing,
                        onRefreshConfig = onRefreshConfig,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = outerMargin, vertical = outerVertical),
                    )
                }

                LazyRow(
                    state = filterListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filterOrder, key = { it }) { id ->
                        ReorderableItem(reorderableFilterState, key = id) { isDragging ->
                            val interactionSource = remember { MutableInteractionSource() }
                            ClevFilterChip(
                                label = homeFilterLabel(id),
                                selected = when (val f = filter) {
                                    HomeFilter.All -> id == HomeFilterOrder.ALL_ID
                                    is HomeFilter.Category -> id == f.category.name
                                },
                                onClick = {
                                    homeFilterFromId(id)?.let { filter = it }
                                },
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer {
                                        val scale = if (isDragging) 1.06f else 1f
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .longPressDraggableHandle(
                                        interactionSource = interactionSource,
                                        onDragStarted = { isReorderingFilters = true },
                                        onDragStopped = {
                                            isReorderingFilters = false
                                            onReorderFilters(filterOrder)
                                        },
                                    ),
                            )
                        }
                    }
                }
            }

            if (state.isLoading && state.nodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = colors.yellow,
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
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
                            onPing = { onPingNode(node.id) },
                        )
                    }
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
    onScanQrClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        ClevLogoFull(logoHeight = 62.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.clev_tagline),
            color = colors.mocha,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CoffemaniaColors.BrandGradient)
                    .clickable(enabled = !isLoading, onClick = onPasteLinkClick)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.clev_paste_clipboard),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, colors.espresso.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .background(colors.cappuccino.copy(alpha = 0.55f))
                    .clickable(enabled = !isLoading, onClick = onScanQrClick)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = colors.espresso,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.clev_scan_qr),
                        color = colors.espresso,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                }
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
private fun HomeSubscriptionCard(
    state: MainUiState,
    onRefreshPing: () -> Unit,
    onRefreshConfig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    val context = LocalContext.current
    val info = state.subscriptionInfo
    val expired = info?.isExpired() == true
    val title = when {
        info?.hasTitle == true -> info.title
        else -> stringResource(R.string.clev_subscription_default_title)
    }
    val accent = colors.yellow
    val cardShape = RoundedCornerShape(16.dp)
    val accountUrl = BuildConfig.SUBSCRIPTION_STORE_URL.ifBlank { "https://hushvpn.net/" }

    Column(
        modifier = modifier
            .clip(cardShape)
            .background(colors.cappuccino)
            .border(1.dp, colors.latte, cardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(accountUrl)),
                        )
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "H",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.espresso,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        if (expired) R.string.clev_subscription_expired
                        else R.string.clev_subscription_active,
                    ),
                    color = if (expired) colors.error else colors.mocha,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.mocha.copy(alpha = 0.75f),
                modifier = Modifier.size(22.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = info?.expireRemainingShortLabel()
                        ?: stringResource(R.string.clev_expire_unknown),
                    color = if (expired) colors.error else colors.espresso,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    text = info?.trafficLabel() ?: "— / —",
                    color = colors.mocha,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            HomeSubscriptionProgressBar(
                info = info,
                accent = accent,
                track = colors.latte,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeSubscriptionActionButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.clev_ping_action),
                filled = false,
                accent = accent,
                enabled = !state.isPinging && state.nodes.isNotEmpty(),
                loading = state.isPinging,
                onClick = onRefreshPing,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = colors.espresso,
                        modifier = Modifier.size(15.dp),
                    )
                },
            )
            HomeSubscriptionActionButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.clev_update_action),
                filled = true,
                accent = accent,
                enabled = !state.isLoading,
                loading = state.isLoading,
                onClick = onRefreshConfig,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun HomeSubscriptionProgressBar(
    info: SubscriptionInfo?,
    accent: Color,
    track: Color,
) {
    val fraction = when {
        info == null -> 0f
        info.isUnlimitedTraffic -> if (info.used > 0) 0.08f else 0f
        else -> info.usageFraction.coerceIn(0f, 1f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(track),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent),
            )
        }
    }
}

@Composable
private fun HomeSubscriptionActionButton(
    text: String,
    filled: Boolean,
    accent: Color,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    val shape = RoundedCornerShape(12.dp)
    val bg = if (filled) accent else Color.Transparent
    val borderColor = if (filled) Color.Transparent else colors.latte
    val contentColor = if (filled) Color.White else colors.espresso

    Row(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                strokeWidth = 1.75.dp,
                color = contentColor,
            )
        } else {
            icon()
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
    onPing: () -> Unit,
) {
    val colors = coffemaniaColors()
    val shape = RoundedCornerShape(10.dp)
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
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
                    onLongClick = { showMenu = true },
                )
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ServerListFlag(flag = display.flag, height = 20.dp)
                ServerTitleWithProtocolBadge(
                    title = display.title,
                    protocolLabel = display.protocolLabel,
                    favorite = favorite,
                    onFavoriteClick = onToggleFavorite,
                    modifier = Modifier.weight(1f),
                )
                when {
                    isPinging && display.pingMs == null && display.pingText != "—" -> CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = colors.yellow,
                    )
                    display.pingMs != null -> PingLabel(ms = display.pingMs)
                    display.pingText == "—" -> PingUnavailableLabel()
                    else -> Unit
                }
                ClevSelectionIndicator(selected = selected, size = 16.dp)
            }
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(12.dp, 0.dp),
            containerColor = colors.cappuccino,
            shape = RoundedCornerShape(10.dp),
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(
                            if (favorite) R.string.clev_remove_favorite else R.string.clev_add_favorite,
                        ),
                        color = colors.espresso,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                },
                onClick = {
                    showMenu = false
                    onToggleFavorite()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = colors.yellow,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.clev_ping_server),
                        color = colors.espresso,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                },
                onClick = {
                    showMenu = false
                    onPing()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = colors.yellow,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
    }
}
