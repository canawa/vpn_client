package work.bavshield.vpn.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import work.bavshield.vpn.R
import work.bavshield.vpn.data.AppLanguage
import work.bavshield.vpn.data.LocaleHelper
import work.bavshield.vpn.vpn.VpnStatus

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
    onPasteLinkClick: () -> Unit,
    onBuyOnWebsiteClick: () -> Unit,
    onTelegramBotClick: () -> Unit,
    onSupportClick: () -> Unit,
    onPaySubscriptionClick: () -> Unit,
    onPayDevicesClick: () -> Unit,
    onDeleteSubscriptionClick: () -> Unit,
    onTelegramChannelClick: () -> Unit,
    onCloseApp: () -> Unit,
    onSaveConnectionSettings: (work.bavshield.vpn.data.ConnectionSettingsState) -> Unit,
    onSubscriptionAutoUpdateIntervalChange: (work.bavshield.vpn.data.SubscriptionAutoUpdateInterval) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onPingAutoIntervalChange: (work.bavshield.vpn.data.PingAutoInterval) -> Unit,
    onPingTestHostsChange: (String) -> Unit,
    onPingNow: () -> Unit,
    onEmailClick: () -> Unit,
) {
    val context = LocalContext.current
    val appLanguage = remember { LocaleHelper.current(context) }
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    var showSettings by remember { mutableStateOf(false) }
    var settingsPage by remember { mutableStateOf(SettingsPage.Main) }
    var showDeleteSubscriptionConfirm by remember { mutableStateOf(false) }

    val hasSubscription = state.nodes.isNotEmpty()

    if (!hasSubscription) {
        FirstLaunchScreen(
            isLoading = state.isLoading,
            error = state.error,
            onPasteClick = onPasteLinkClick,
        )
        return
    }

    val selectedNode = state.nodes.find { it.id == state.selectedNodeId }
    val selectedDisplay = selectedNode?.let {
        ServerDisplayMapper.map(context, it, state.nodePings[it.id])
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

    Box(modifier = Modifier.fillMaxSize()) {
        CyberBackground(
            modifier = Modifier.fillMaxSize(),
            connected = !showSettings &&
                selectedTab == AppTab.Home &&
                (state.vpnStatus == VpnStatus.Started || state.vpnStatus == VpnStatus.Starting),
        )
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                if (showSettings || selectedTab == AppTab.Servers) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars),
                    ) {
                        when {
                            showSettings -> BavShieldTopBar(
                                title = stringResource(settingsPage.headerTitleRes),
                                showBackButton = true,
                                onBackClick = { navigateBack() },
                                showSettingsButton = false,
                            )
                            selectedTab == AppTab.Servers -> BavShieldTopBar(
                                title = stringResource(R.string.tab_servers),
                                showBackButton = true,
                                onBackClick = { navigateBack() },
                                onSettingsClick = {
                                    settingsPage = SettingsPage.Main
                                    showSettings = true
                                },
                            )
                        }
                    }
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
                appLanguage = appLanguage,
                onAppLanguageChange = onAppLanguageChange,
                pingAutoInterval = state.pingAutoInterval,
                onPingAutoIntervalChange = onPingAutoIntervalChange,
                pingTestHosts = state.pingTestHosts,
                onPingTestHostsChange = onPingTestHostsChange,
                onPingNow = onPingNow,
                isPinging = state.isPinging,
                onSiteClick = onBuyOnWebsiteClick,
                onTelegramBotClick = onTelegramBotClick,
                onTelegramChannelClick = onTelegramChannelClick,
                onSupportClick = onSupportClick,
                onEmailClick = onEmailClick,
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
                onShowLogs = onShowLogs,
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
                    onSelectNode = onSelectNode,
                    onConnectToNode = onConnectToNode,
                    onPasteLinkClick = onPasteLinkClick,
                    onSiteClick = onBuyOnWebsiteClick,
                    onTelegramBotClick = onTelegramBotClick,
                    onSupportClick = onSupportClick,
                    onSettingsClick = {
                        settingsPage = SettingsPage.Main
                        showSettings = true
                    },
                    onRefreshSubscription = onRefreshSubscription,
                    onPingNow = onPingNow,
                )
                AppTab.Servers -> ServersScreen(
                    modifier = Modifier.padding(padding),
                    nodes = state.nodes,
                    selectedNodeId = state.selectedNodeId,
                    nodePings = state.nodePings,
                    subscriptionInfo = state.subscriptionInfo,
                    enabled = state.vpnStatus != work.bavshield.vpn.vpn.VpnStatus.Starting &&
                        state.vpnStatus != work.bavshield.vpn.vpn.VpnStatus.Stopping,
                    onSelectNode = onSelectNode,
                    onConnectToNode = { nodeId ->
                        selectedTab = AppTab.Home
                        onConnectToNode(nodeId)
                    },
                    onTelegramChannelClick = onTelegramChannelClick,
                    onRenewTelegramClick = onTelegramBotClick,
                    onBuyOnWebsiteClick = onBuyOnWebsiteClick,
                )
            }
        }
        }
    }

    if (showDeleteSubscriptionConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSubscriptionConfirm = false },
            title = { Text(stringResource(R.string.subscription_delete_title)) },
            text = { Text(stringResource(R.string.subscription_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSubscriptionConfirm = false
                        showSettings = false
                        onDeleteSubscriptionClick()
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSubscriptionConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
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
        title = { Text(stringResource(R.string.logs_title)) },
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
                Text(stringResource(R.string.settings_download_logs))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.logs_close))
            }
        },
    )
}
