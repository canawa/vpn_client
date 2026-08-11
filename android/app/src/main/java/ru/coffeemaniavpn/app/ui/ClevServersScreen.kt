package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.LoadBalancer
import ru.coffeemaniavpn.app.data.PingState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun XenoServersScreen(
    state: MainUiState,
    onSelectNode: (String) -> Unit,
    onSelectAutoBalancer: () -> Unit,
    onConnectToNode: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onToggleFavorite: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onPingNode: (String) -> Unit,
    onRefreshAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    var query by remember { mutableStateOf("") }
    val nodeCount = state.nodes.size

    val filteredNodes = remember(state.nodes, state.favoriteNodeIds, query) {
        val base = state.nodes.sortedWith { a, b ->
            val aFav = a.id in state.favoriteNodeIds
            val bFav = b.id in state.favoriteNodeIds
            when {
                aFav && !bFav -> -1
                !aFav && bFav -> 1
                else -> 0
            }
        }
        if (query.isBlank()) base
        else base.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.host.contains(query, ignoreCase = true)
        }
    }

    val bestPing = filteredNodes
        .mapNotNull { (state.nodePings[it.id] as? PingState.Result)?.latencyMs }
        .minOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam),
    ) {
        XenoScreenHeader(
            title = stringResource(R.string.xeno_servers_title),
            subtitle = stringResource(
                R.string.xeno_servers_subtitle,
                filteredNodes.size.coerceAtMost(nodeCount),
                nodeCount,
            ),
        )

        PullToRefreshBox(
            isRefreshing = state.isLoading || state.isPinging,
            onRefresh = onRefreshAll,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    XenoSearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = stringResource(R.string.xeno_search_location),
                    )
                }

                item {
                    XenoAutoCard(
                        selected = state.selectedNodeId == LoadBalancer.AUTO_NODE_ID,
                        bestPingMs = bestPing,
                        onClick = onSelectAutoBalancer,
                        onDoubleClick = {
                            onSelectAutoBalancer()
                            onConnectToNode(LoadBalancer.AUTO_NODE_ID)
                        },
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.xeno_locations).uppercase(),
                        color = colors.mocha,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }

                item {
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(colors.cappuccino)
                            .border(1.dp, colors.latte, shape),
                    ) {
                        filteredNodes.forEachIndexed { index, node ->
                            val display = ServerDisplayMapper.map(node, state.nodePings[node.id])
                            XenoLocationRow(
                                display = display,
                                selected = state.selectedNodeId == node.id,
                                onClick = { onSelectNode(node.id) },
                                onDoubleClick = { onConnectToNode(node.id) },
                            )
                            if (index < filteredNodes.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(colors.latte),
                                )
                            }
                        }
                        if (filteredNodes.isEmpty()) {
                            Text(
                                text = stringResource(R.string.xeno_no_server),
                                color = colors.mocha,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XenoSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val colors = coffemaniaColors()
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cappuccino)
            .border(1.dp, colors.latte, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = colors.espresso,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isBlank()) {
                    Text(placeholder, color = colors.mocha, fontSize = 14.sp)
                }
                inner()
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun XenoAutoCard(
    selected: Boolean,
    bestPingMs: Int?,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    val colors = coffemaniaColors()
    val shape = RoundedCornerShape(16.dp)
    val border = if (selected) colors.primary else colors.primary.copy(alpha = 0.45f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF0F2A22))
            .border(1.dp, border, shape)
            .combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF163B30)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.xeno_auto),
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Text(
                text = stringResource(R.string.xeno_auto_subtitle),
                color = colors.mocha,
                fontSize = 12.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = bestPingMs?.let { "$it ms" } ?: "—",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Text(
                text = "BEST",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun XenoLocationRow(
    display: ServerDisplay,
    selected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    val colors = coffemaniaColors()
    val pingColor = when {
        display.pingMs == null -> colors.mocha
        display.pingMs < 80 -> colors.primary
        display.pingMs < 150 -> colors.orange
        else -> colors.error
    }
    val code = FlagUtils.resolveCountryCodeOrDefault(display.flag)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color(0xFF102820) else Color.Transparent)
            .combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.primary),
            )
        } else {
            Spacer(modifier = Modifier.width(3.dp))
        }
        XenoCountryTile(code = code, modifier = Modifier.size(40.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = display.title,
                color = colors.espresso,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
            )
            val sub = display.group ?: display.subtitle
            if (sub.isNotBlank()) {
                Text(text = sub, color = colors.mocha, fontSize = 12.sp, maxLines = 1)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selected && display.pingMs != null) {
                XenoSignalBars(
                    strength = when {
                        display.pingMs < 50 -> 4
                        display.pingMs < 100 -> 3
                        display.pingMs < 180 -> 2
                        else -> 1
                    },
                    color = pingColor,
                )
            }
            Text(
                text = display.pingMs?.let { "$it ms" } ?: display.pingText,
                color = pingColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }
}

