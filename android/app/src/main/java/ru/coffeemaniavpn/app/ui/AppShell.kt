package ru.coffeemaniavpn.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ru.coffeemaniavpn.app.R

@Composable
fun AppShell(
    state: MainUiState,
    onRefreshSubscription: () -> Unit,
    onSelectNode: (String) -> Unit,
    onConnectToNode: (String) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onShowLogs: () -> Unit,
    onDownloadLogs: () -> Unit,
    onRefreshPing: () -> Unit,
    onRefreshConfig: () -> Unit,
    onPasteLinkClick: () -> Unit,
    onBuyOnWebsiteClick: () -> Unit,
    onDeleteSubscriptionClick: () -> Unit,
    onRenewTelegramClick: () -> Unit,
    onCloseApp: () -> Unit,
    onSaveConnectionSettings: (ru.coffeemaniavpn.app.data.ConnectionSettingsState) -> Unit,
    onSubscriptionAutoUpdateIntervalChange: (ru.coffeemaniavpn.app.data.SubscriptionAutoUpdateInterval) -> Unit,
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
            showSettings -> {
                val parent = settingsPage.parentPage()
                if (parent != null) {
                    settingsPage = parent
                } else {
                    showSettings = false
                    settingsPage = SettingsPage.Main
                }
            }
            selectedTab != AppTab.Home -> selectedTab = AppTab.Home
        }
    }

    BackHandler(enabled = canNavigateBack) {
        navigateBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = coffemaniaColors().milkFoam,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                when {
                    showSettings -> CoffemaniaTopBar(
                        title = settingsPage.headerTitle,
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
                        title = stringResource(R.string.app_name),
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
                hasSubscription = hasSubscription,
                connectionSettings = state.connectionSettings,
                onSaveConnectionSettings = onSaveConnectionSettings,
                subscriptionAutoUpdateInterval = state.subscriptionAutoUpdateInterval,
                onSubscriptionAutoUpdateIntervalChange = onSubscriptionAutoUpdateIntervalChange,
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
                onBuyOnWebsite = {
                    showSettings = false
                    settingsPage = SettingsPage.Main
                    onBuyOnWebsiteClick()
                },
                onShowLogs = {
                    showSettings = false
                    settingsPage = SettingsPage.Main
                    onShowLogs()
                },
                onDownloadLogs = onDownloadLogs,
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
                    onBuyOnWebsiteClick = onBuyOnWebsiteClick,
                    onRenewTelegramClick = onRenewTelegramClick,
                )
                AppTab.Servers -> ServersScreen(
                    modifier = Modifier.padding(padding),
                    nodes = state.nodes,
                    selectedNodeId = state.selectedNodeId,
                    nodePings = state.nodePings,
                    subscriptionInfo = state.subscriptionInfo,
                    lastUpdatedAtMs = state.subscriptionLastUpdatedAtMs,
                    isRefreshing = state.isLoading,
                    isPinging = state.isPinging,
                    canRefreshConfig = state.subscriptionUrl.isNotBlank(),
                    enabled = state.vpnStatus != ru.coffeemaniavpn.app.vpn.VpnStatus.Starting &&
                        state.vpnStatus != ru.coffeemaniavpn.app.vpn.VpnStatus.Stopping,
                    onSelectNode = onSelectNode,
                    onConnectToNode = { nodeId ->
                        selectedTab = AppTab.Home
                        onConnectToNode(nodeId)
                    },
                    onRefreshConfig = onRefreshConfig,
                    onRefreshPing = onRefreshPing,
                    onRenewTelegramClick = onRenewTelegramClick,
                    onBuyOnWebsiteClick = onBuyOnWebsiteClick,
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
