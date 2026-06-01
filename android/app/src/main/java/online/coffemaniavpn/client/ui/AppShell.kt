package online.coffemaniavpn.client.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import online.coffemaniavpn.client.BuildConfig

@Composable
fun AppShell(
    state: MainUiState,
    onRefreshSubscription: () -> Unit,
    onSelectNode: (String) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onShowLogs: () -> Unit,
    onDownloadLogs: () -> Unit,
    onRefreshPing: () -> Unit,
    onRefreshConfig: () -> Unit,
    onPasteLinkClick: () -> Unit,
    onDeleteSubscriptionClick: () -> Unit,
    onBuySubscriptionClick: () -> Unit,
    onTelegramChannelClick: () -> Unit,
    onCloseApp: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    var showSettings by remember { mutableStateOf(false) }
    var settingsPage by remember { mutableStateOf(SettingsPage.Main) }
    var showDeleteSubscriptionConfirm by remember { mutableStateOf(false) }

    val hasSubscription = state.subscriptionUrl.isNotBlank() && state.nodes.isNotEmpty()

    val selectedNode = state.nodes.find { it.id == state.selectedNodeId }
    val selectedDisplay = selectedNode?.let {
        ServerDisplayMapper.map(it, state.nodePings[it.id])
    }

    val canNavigateBack =
        showDeleteSubscriptionConfirm ||
            showSettings ||
            selectedTab != AppTab.Home

    fun navigateBack() {
        when {
            showDeleteSubscriptionConfirm -> showDeleteSubscriptionConfirm = false
            showSettings && settingsPage != SettingsPage.Main -> settingsPage = SettingsPage.Main
            showSettings -> {
                showSettings = false
                settingsPage = SettingsPage.Main
            }
            selectedTab != AppTab.Home -> selectedTab = AppTab.Home
        }
    }

    BackHandler(enabled = canNavigateBack) {
        navigateBack()
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
                when {
                    showSettings -> CoffemaniaTopBar(
                        title = settingsPage.title,
                        showBackButton = true,
                        onBackClick = { navigateBack() },
                        showSettingsButton = false,
                    )
                    selectedTab == AppTab.Servers -> CoffemaniaTopBar(
                        title = "Серверы",
                        showBackButton = true,
                        onBackClick = { navigateBack() },
                        onSettingsClick = {
                            settingsPage = SettingsPage.Main
                            showSettings = true
                        },
                    )
                    else -> CoffemaniaTopBar(
                        title = "КОФЕМАНИЯ ВПН",
                        onSettingsClick = {
                            settingsPage = SettingsPage.Main
                            showSettings = true
                        },
                    )
                }
            }
        },
        bottomBar = {
            if (!showSettings) {
                CoffemaniaBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                )
            }
        },
    ) { padding ->
        if (showSettings) {
            SettingsScreen(
                modifier = Modifier.padding(padding),
                page = settingsPage,
                onPageChange = { settingsPage = it },
                appVersion = BuildConfig.VERSION_NAME,
                hasSubscription = hasSubscription,
                onOpenServers = {
                    showSettings = false
                    settingsPage = SettingsPage.Main
                    selectedTab = AppTab.Servers
                },
                onPasteLink = {
                    showSettings = false
                    settingsPage = SettingsPage.Main
                    onPasteLinkClick()
                },
                onRefreshSubscription = {
                    showSettings = false
                    settingsPage = SettingsPage.Main
                    onRefreshSubscription()
                },
                onDeleteSubscription = { showDeleteSubscriptionConfirm = true },
                onShowLogs = {
                    showSettings = false
                    settingsPage = SettingsPage.Main
                    onShowLogs()
                },
                onDownloadLogs = onDownloadLogs,
                onBuySubscription = onBuySubscriptionClick,
                onTelegramChannel = onTelegramChannelClick,
                onCloseApp = onCloseApp,
            )
        } else {
            when (selectedTab) {
                AppTab.Home -> HomeScreen(
                    modifier = Modifier.padding(padding),
                    state = state,
                    selectedDisplay = selectedDisplay,
                    onConnectClick = onConnectClick,
                    onDisconnectClick = onDisconnectClick,
                    onOpenServers = { selectedTab = AppTab.Servers },
                    onPasteLinkClick = onPasteLinkClick,
                    onBuySubscriptionClick = onBuySubscriptionClick,
                    onTelegramChannelClick = onTelegramChannelClick,
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

    if (showDeleteSubscriptionConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSubscriptionConfirm = false },
            title = { Text("Удалить подписку?") },
            text = { Text("Ссылка и список серверов будут удалены с устройства.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSubscriptionConfirm = false
                        showSettings = false
                        onDeleteSubscriptionClick()
                    },
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSubscriptionConfirm = false }) {
                    Text("Отмена")
                }
            },
        )
    }
}

@Composable
fun LogsDialog(
    text: String,
    onDismiss: () -> Unit,
    onDownloadLogs: () -> Unit,
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
        dismissButton = {
            TextButton(onClick = onDownloadLogs) {
                Text("Скачать логи")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
    )
}
