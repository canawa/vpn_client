package ru.coffeemaniavpn.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.coffeemaniavpn.app.data.PingState
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.data.SubscriptionInfo

@Composable
fun ServersScreen(
    nodes: List<ProxyNode>,
    selectedNodeId: String?,
    nodePings: Map<String, PingState>,
    favoriteNodeIds: Set<String>,
    subscriptionInfo: SubscriptionInfo?,
    isRefreshing: Boolean,
    isPinging: Boolean,
    canRefreshConfig: Boolean,
    enabled: Boolean,
    onSelectNode: (String) -> Unit,
    onConnectToNode: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRefreshConfig: () -> Unit,
    onRefreshPing: () -> Unit,
    onTelegramBotClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    val subscriptionExpired = subscriptionInfo?.isExpired() == true

    var search by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }

    val filteredNodes = remember(nodes, search, favoritesOnly, favoriteNodeIds) {
        nodes.filter { node ->
            val matchesSearch = search.isBlank() ||
                node.name.contains(search, ignoreCase = true)
            val matchesFavorites = !favoritesOnly || node.id in favoriteNodeIds
            matchesSearch && matchesFavorites
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Поиск
        ServerSearchBox(
            value = search,
            onValueChange = { search = it },
        )

        // Фильтры + обновление/пинг
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterPill(
                text = "Все серверы",
                active = !favoritesOnly,
                onClick = { favoritesOnly = false },
            )
            FilterPill(
                text = "⭐ Избранные",
                active = favoritesOnly,
                onClick = { favoritesOnly = true },
            )
            Spacer(modifier = Modifier.weight(1f))
            ConfigRefreshButton(
                isRefreshing = isRefreshing,
                enabled = canRefreshConfig && !subscriptionExpired,
                onClick = onRefreshConfig,
            )
            PingTestButton(
                isPinging = isPinging,
                enabled = nodes.isNotEmpty() && !subscriptionExpired,
                onClick = onRefreshPing,
            )
        }

        // Статистика
        ServersStatsBar(
            nodes = nodes,
            nodePings = nodePings,
            subscriptionInfo = subscriptionInfo,
        )

        when {
            subscriptionExpired -> {
                SubscriptionExpiredCard(
                    onTelegramBotClick = onTelegramBotClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            nodes.isEmpty() -> {
                Text(
                    text = "Список серверов пуст. Добавьте подписку на странице подключения.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textDim,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                )
            }
            filteredNodes.isEmpty() -> {
                Text(
                    text = if (favoritesOnly) {
                        "Нет избранных серверов. Отметьте сервер звёздочкой."
                    } else {
                        "Ничего не найдено по запросу «$search»"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textDim,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(
                        items = filteredNodes,
                        key = { it.id },
                        contentType = { "server" },
                    ) { node ->
                        ServerListItem(
                            node = node,
                            ping = nodePings[node.id],
                            selected = node.id == selectedNodeId,
                            isFavorite = node.id in favoriteNodeIds,
                            enabled = enabled,
                            onSelectNode = onSelectNode,
                            onConnectToNode = onConnectToNode,
                            onToggleFavorite = onToggleFavorite,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerSearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = colors.textDim,
            modifier = Modifier.size(18.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "Поиск страны или сервера…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textDim,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textMain),
                cursorBrush = SolidColor(colors.blue),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Очистить",
                tint = colors.textDim,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
            )
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = nuboColors()
    val shape = RoundedCornerShape(999.dp)
    val activeProgress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "filterPillActive",
    )
    val backgroundColor by animateColorAsState(
        targetValue = lerp(colors.card, Color(0xFF1A4FFF), activeProgress),
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "filterPillBg",
    )
    val textColor by animateColorAsState(
        targetValue = lerp(colors.textDim, Color.White, activeProgress),
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "filterPillText",
    )
    val borderColor by animateColorAsState(
        targetValue = lerp(colors.border, colors.borderStrong, activeProgress),
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "filterPillBorder",
    )

    Box(
        modifier = Modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )
    }
}

@Composable
private fun ServersStatsBar(
    nodes: List<ProxyNode>,
    nodePings: Map<String, PingState>,
    subscriptionInfo: SubscriptionInfo?,
) {
    val colors = nuboColors()
    val countryCount = remember(nodes) {
        nodes.map { ServerDisplayMapper.map(it).flag }.distinct().size
    }
    val minPing = remember(nodePings) {
        nodePings.values
            .filterIsInstance<PingState.Result>()
            .minOfOrNull { it.latencyMs }
    }

    NuboCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem(value = nodes.size.toString(), label = "Серверов")
                StatItem(value = countryCount.toString(), label = "Стран")
                StatItem(
                    value = minPing?.let { "$it ms" } ?: "—",
                    label = "Мин. пинг",
                )
            }
            subscriptionInfo?.let { info ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TrafficProgressBar(
                        subscriptionInfo = info,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = info.trafficLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    val colors = nuboColors()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.blue,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textDim,
        )
    }
}

@Composable
private fun ServerListItem(
    node: ProxyNode,
    ping: PingState?,
    selected: Boolean,
    isFavorite: Boolean,
    enabled: Boolean,
    onSelectNode: (String) -> Unit,
    onConnectToNode: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val staticDisplay = remember(node.id, node.name) {
        ServerDisplayMapper.map(node, ping = null)
    }
    val onClick = remember(node.id, enabled, onSelectNode) {
        { if (enabled) onSelectNode(node.id) }
    }
    val onDoubleClick = remember(node.id, enabled, onConnectToNode) {
        { if (enabled) onConnectToNode(node.id) }
    }
    val onFavorite = remember(node.id, onToggleFavorite) {
        { onToggleFavorite(node.id) }
    }

    ServerListCard(
        display = staticDisplay,
        ping = ping,
        selected = selected,
        isFavorite = isFavorite,
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        onToggleFavorite = onFavorite,
    )
}
