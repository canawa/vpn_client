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
import androidx.compose.foundation.layout.offset
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
import ru.coffeemaniavpn.app.data.PingState

/** Figma: number Inter Medium 14 / #00D4A8 + "ms" Inter Medium 10 / #6B7672 */
@Composable
private fun XenoPingValue(
    pingMs: Int?,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val numberColor = when {
        pingMs == null -> Color(0xFF6B7672)
        else -> CoffemaniaColors.pingColor(pingMs)
    }
    when {
        pingMs != null -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pingMs.toString(),
                    color = numberColor,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 0.sp,
                )
                Text(
                    text = " ms",
                    color = Color(0xFF6B7672),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    letterSpacing = 0.sp,
                    modifier = Modifier.offset(y = 1.dp),
                )
            }
        }
        loading -> Text(
            text = "…",
            color = Color(0xFF6B7672),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = modifier,
        )
        else -> Text(
            text = "—",
            color = Color(0xFF6B7672),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun XenoServersScreen(
    state: MainUiState,
    onSelectNode: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onSelectAutoBalancer: () -> Unit,
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
private fun XenoLocationRow(
    display: ServerDisplay,
    selected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    val colors = coffemaniaColors()
    val pingMs = display.pingMs
    val pingColor = when {
        pingMs == null -> Color(0xFF6B7672)
        else -> CoffemaniaColors.pingColor(pingMs)
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
                    .background(Color(0xFF00D4A8)),
            )
        } else {
            Spacer(modifier = Modifier.width(3.dp))
        }
        XenoCountryTile(code = code, modifier = Modifier.size(40.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = display.title,
                color = Color(0xFFF2F5F4),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
            )
            val sub = display.protocolLabel
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    color = Color(0xFF6B7672),
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
        }
        // Figma: number + "ms" then signal bars (selected only)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            XenoPingValue(
                pingMs = pingMs,
                loading = display.pingText == "…",
            )
            if (selected && pingMs != null) {
                XenoSignalBars(
                    strength = when {
                        pingMs < 50 -> 3
                        pingMs < 100 -> 2
                        else -> 1
                    },
                    color = Color(0xFF00D4A8),
                )
            }
        }
    }
}

