package online.coffemaniavpn.client.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import online.coffemaniavpn.client.data.ProxyNode
import online.coffemaniavpn.client.vpn.VpnStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    onSubscriptionUrlChange: (String) -> Unit,
    onRefreshSubscription: () -> Unit,
    onSelectNode: (String) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onShowLogs: () -> Unit,
) {
    val isConnected = state.vpnStatus == VpnStatus.Started
    val isBusy = state.vpnStatus == VpnStatus.Starting || state.vpnStatus == VpnStatus.Stopping

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("КОФЕМАНИЯ ВПН", fontWeight = FontWeight.Bold)
                        Text(
                            text = statusText(state.vpnStatus),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShowLogs) {
                        Icon(Icons.Default.BugReport, contentDescription = "Логи")
                    }
                    IconButton(onClick = onRefreshSubscription, enabled = !state.isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить подписку")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.startupCrash?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = "Последний краш:\n$it",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            OutlinedTextField(
                value = state.subscriptionUrl,
                onValueChange = onSubscriptionUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL подписки") },
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onRefreshSubscription,
                    enabled = !state.isLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Обновить")
                    }
                }

                Button(
                    onClick = if (isConnected) onDisconnectClick else onConnectClick,
                    enabled = !isBusy && state.nodes.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null)
                    Text(if (isConnected) "Отключить" else "Подключить")
                }
            }

            state.message?.let {
                Text(it, color = MaterialTheme.colorScheme.secondary)
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Text(
                text = "Серверы (${state.nodes.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (state.nodes.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Text(
                        text = "Добавьте URL подписки и нажмите «Обновить»",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.nodes, key = { it.id }) { node ->
                        ServerItem(
                            node = node,
                            selected = node.id == state.selectedNodeId,
                            enabled = !isBusy,
                            onSelect = { onSelectNode(node.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogsDialog(
    text: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Логи приложения") },
        text = {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
    )
}

@Composable
private fun ServerItem(
    node: ProxyNode,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp),
        onClick = { if (enabled) onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onSelect, enabled = enabled)
            Column(modifier = Modifier.weight(1f)) {
                Text(node.name, fontWeight = FontWeight.Medium)
                Text(
                    "${nodeProtocolLabel(node)} · ${node.host}:${node.port}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun nodeProtocolLabel(node: ProxyNode): String = when {
    node.isHysteria2 -> "HY2"
    else -> "VLESS"
}

private fun statusText(status: VpnStatus): String = when (status) {
    VpnStatus.Stopped -> "Не подключено"
    VpnStatus.Starting -> "Подключение…"
    VpnStatus.Started -> "Подключено"
    VpnStatus.Stopping -> "Отключение…"
}
