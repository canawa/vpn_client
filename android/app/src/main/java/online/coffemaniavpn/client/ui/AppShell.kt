package online.coffemaniavpn.client.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@Composable
fun AppShell(
    state: MainUiState,
    onRefreshSubscription: () -> Unit,
    onSelectNode: (String) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onShowLogs: () -> Unit,
    onRefreshPing: () -> Unit,
    onRefreshConfig: () -> Unit,
    onPasteLinkClick: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    var showSettingsMenu by remember { mutableStateOf(false) }

    val selectedNode = state.nodes.find { it.id == state.selectedNodeId }
    val selectedDisplay = selectedNode?.let {
        ServerDisplayMapper.map(it, state.nodePings[it.id])
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CoffemaniaColors.MilkFoam,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                CoffemaniaTopBar(
                    title = if (selectedTab == AppTab.Home) "КОФЕМАНИЯ ВПН" else "Серверы",
                    settingsMenuExpanded = showSettingsMenu,
                    onSettingsClick = { showSettingsMenu = true },
                    onSettingsMenuDismiss = { showSettingsMenu = false },
                    settingsMenuContent = {
                        SettingsMenuItems(
                            onPasteLink = {
                                showSettingsMenu = false
                                onPasteLinkClick()
                            },
                            onRefresh = {
                                showSettingsMenu = false
                                onRefreshSubscription()
                            },
                            onLogs = {
                                showSettingsMenu = false
                                onShowLogs()
                            },
                        )
                    },
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
                onPasteLinkClick = onPasteLinkClick,
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
}

@Composable
private fun ColumnScope.SettingsMenuItems(
    onPasteLink: () -> Unit,
    onRefresh: () -> Unit,
    onLogs: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text("Вставить ссылку") },
        onClick = onPasteLink,
        leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
    )
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
