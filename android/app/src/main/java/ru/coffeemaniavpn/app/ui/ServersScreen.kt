package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.coffeemaniavpn.app.data.PingState
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.data.SubscriptionInfo

@Composable
fun ServersScreen(
    nodes: List<ProxyNode>,
    selectedNodeId: String?,
    nodePings: Map<String, PingState>,
    subscriptionInfo: SubscriptionInfo?,
    enabled: Boolean,
    onSelectNode: (String) -> Unit,
    onConnectToNode: (String) -> Unit,
    onTelegramChannelClick: () -> Unit,
    onRenewTelegramClick: () -> Unit,
    onBuyOnWebsiteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subscriptionExpired = subscriptionInfo?.isExpired() == true

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
    ) {
        val bannerMetrics = remember(maxWidth, maxHeight) {
            PromoBannerMetrics.compute(
                widthDp = maxWidth.value,
                heightDp = maxHeight.value,
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (subscriptionExpired) {
                SubscriptionExpiredCard(
                    onRenewTelegramClick = onRenewTelegramClick,
                    onRenewWebsiteClick = onBuyOnWebsiteClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (nodes.isEmpty()) {
                Text(
                    text = "Список серверов пуст. Добавьте подписку на главной.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(
                        items = nodes,
                        key = { it.id },
                        contentType = { "server" },
                    ) { node ->
                        ServerListItem(
                            node = node,
                            ping = nodePings[node.id],
                            selected = node.id == selectedNodeId,
                            enabled = enabled,
                            onSelectNode = onSelectNode,
                            onConnectToNode = onConnectToNode,
                        )
                    }
                }
            }

            TelegramChannelBanner(
                onClick = onTelegramChannelClick,
                modifier = Modifier.heightIn(max = bannerMetrics.maxHeightDp.dp),
                metrics = bannerMetrics,
            )
        }
    }
}

@Composable
private fun ServerListItem(
    node: ProxyNode,
    ping: PingState?,
    selected: Boolean,
    enabled: Boolean,
    onSelectNode: (String) -> Unit,
    onConnectToNode: (String) -> Unit,
) {
    val display = remember(node.id, ping) {
        ServerDisplayMapper.map(node, ping)
    }
    val onClick = remember(node.id, enabled, onSelectNode) {
        { if (enabled) onSelectNode(node.id) }
    }
    val onDoubleClick = remember(node.id, enabled, onConnectToNode) {
        { if (enabled) onConnectToNode(node.id) }
    }

    ServerListCard(
        display = display,
        selected = selected,
        onClick = onClick,
        onDoubleClick = onDoubleClick,
    )
}
