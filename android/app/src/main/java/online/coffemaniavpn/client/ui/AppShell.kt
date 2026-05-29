package online.coffemaniavpn.client.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun AppShell(
    state: MainUiState,
    onSubscriptionUrlChange: (String) -> Unit,
    onRefreshSubscription: () -> Unit,
    onSelectNode: (String) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onShowLogs: () -> Unit,
    onRefreshPing: () -> Unit,
    onRefreshConfig: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val selectedNode = state.nodes.find { it.id == state.selectedNodeId }
    val selectedDisplay = selectedNode?.let {
        ServerDisplayMapper.map(it, state.nodePings[it.id])
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CoffemaniaColors.Background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                CoffemaniaTopBar(
                    title = if (selectedTab == AppTab.Home) "КОФЕМАНИЯ ВПН" else "Серверы",
                    onSettingsClick = { showSettingsMenu = true },
                )
                SettingsMenu(
                    expanded = showSettingsMenu,
                    onDismiss = { showSettingsMenu = false },
                    onRefresh = {
                        showSettingsMenu = false
                        onRefreshSubscription()
                    },
                    onLogs = {
                        showSettingsMenu = false
                        onShowLogs()
                    },
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        },
        bottomBar = {
            CoffemaniaBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            )
        },
    ) { padding ->
        when (selectedTab) {
            AppTab.Home -> HomeScreen(
                modifier = Modifier.padding(padding),
                state = state,
                selectedDisplay = selectedDisplay,
                onConnectClick = onConnectClick,
                onDisconnectClick = onDisconnectClick,
                onOpenServers = { selectedTab = AppTab.Servers },
                onTelegramClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/coffemaniavpn_bot")),
                        )
                    }
                },
                onPasteLinkClick = { showPasteDialog = true },
            )
            AppTab.Servers -> ServersScreen(
                modifier = Modifier.padding(padding),
                nodes = state.nodes,
                selectedNodeId = state.selectedNodeId,
                nodePings = state.nodePings,
                subscriptionInfo = state.subscriptionInfo,
                isRefreshing = state.isLoading,
                isPinging = state.isPinging,
                canRefreshConfig = state.subscriptionUrl.isNotBlank(),
                enabled = state.vpnStatus != online.coffemaniavpn.client.vpn.VpnStatus.Starting &&
                    state.vpnStatus != online.coffemaniavpn.client.vpn.VpnStatus.Stopping,
                onSelectNode = onSelectNode,
                onRefreshConfig = onRefreshConfig,
                onRefreshPing = onRefreshPing,
            )
        }
    }

    if (showPasteDialog) {
        SubscriptionUrlDialog(
            url = state.subscriptionUrl,
            isLoading = state.isLoading,
            onUrlChange = onSubscriptionUrlChange,
            onDismiss = { showPasteDialog = false },
            onConfirm = {
                onRefreshSubscription()
                showPasteDialog = false
            },
        )
    }
}

@Composable
private fun SettingsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier.padding(top = 48.dp, end = 16.dp),
    ) {
        DropdownMenuItem(
            text = { Text("Обновить подписку") },
            onClick = onRefresh,
            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
        )
        DropdownMenuItem(
            text = { Text("Логи") },
            onClick = onLogs,
            leadingIcon = { Icon(Icons.Default.BugReport, contentDescription = null) },
        )
    }
}

@Composable
fun SubscriptionUrlDialog(
    url: String,
    isLoading: Boolean,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ссылка подписки") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxSize(),
                placeholder = { Text("https://…") },
                singleLine = false,
                minLines = 2,
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isLoading && url.isNotBlank()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Загрузить")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
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
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
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
