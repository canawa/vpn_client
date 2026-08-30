package work.bavshield.vpn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import work.bavshield.vpn.R
import work.bavshield.vpn.data.PingState
import work.bavshield.vpn.data.ProxyNode
import work.bavshield.vpn.data.SubscriptionInfo

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (subscriptionExpired) {
            SubscriptionExpiredCard(
                onRenewTelegramClick = onRenewTelegramClick,
                onRenewWebsiteClick = onBuyOnWebsiteClick,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (nodes.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.servers_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = bavShieldColors().mocha,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
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
    val context = LocalContext.current
    val display = remember(node.id, ping) {
        ServerDisplayMapper.map(context, node, ping)
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
