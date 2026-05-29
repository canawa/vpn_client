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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import online.coffemaniavpn.client.data.ProxyNode

@Composable
fun ServersScreen(
    nodes: List<ProxyNode>,
    selectedNodeId: String?,
    enabled: Boolean,
    onSelectNode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortedNodes = remember(nodes) {
        nodes.sortedBy { ServerDisplayMapper.fakePingMs(it.id) }
    }
    val fastestId = sortedNodes.firstOrNull()?.id

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SortBar()

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
                items(sortedNodes, key = { it.id }) { node ->
                    val display = ServerDisplayMapper.map(node)
                    ServerListCard(
                        display = display,
                        selected = node.id == selectedNodeId,
                        showFastestBadge = node.id == fastestId,
                        onClick = { if (enabled) onSelectNode(node.id) },
                    )
                }
            }
        }
    }
}
