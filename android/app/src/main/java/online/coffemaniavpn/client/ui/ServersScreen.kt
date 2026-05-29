package online.coffemaniavpn.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import online.coffemaniavpn.client.data.PingState
import online.coffemaniavpn.client.data.ProxyNode
import online.coffemaniavpn.client.data.SubscriptionInfo

@Composable
fun ServersScreen(
    nodes: List<ProxyNode>,
    selectedNodeId: String?,
    nodePings: Map<String, PingState>,
    subscriptionInfo: SubscriptionInfo?,
    isRefreshing: Boolean,
    isPinging: Boolean,
    canRefreshConfig: Boolean,
    enabled: Boolean,
    onSelectNode: (String) -> Unit,
    onRefreshConfig: () -> Unit,
    onRefreshPing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubscriptionStatusBar(
            nodeCount = nodes.size,
            subscriptionInfo = subscriptionInfo,
            isRefreshing = isRefreshing,
            isPinging = isPinging,
            canRefresh = canRefreshConfig,
            canPing = nodes.isNotEmpty(),
            onRefreshConfig = onRefreshConfig,
            onRefreshPing = onRefreshPing,
        )

        if (nodes.isEmpty()) {
            Text(
                text = "Список серверов пуст. Добавьте подписку на главной.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(nodes, key = { it.id }) { node ->
                    val display = ServerDisplayMapper.map(node, nodePings[node.id])
                    ServerListCard(
                        display = display,
                        selected = node.id == selectedNodeId,
                        onClick = { if (enabled) onSelectNode(node.id) },
                    )
                }
            }
        }
    }
}
