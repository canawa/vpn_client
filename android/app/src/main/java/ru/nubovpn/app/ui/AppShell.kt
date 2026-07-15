package ru.nubovpn.app.ui

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
import androidx.compose.ui.text.font.FontFamily
import ru.nubovpn.app.BuildConfig

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
    onScanQrClick: () -> Unit,
    onToggleSortByPing: () -> Unit,
    onDeleteSubscriptionClick: () -> Unit,
    onTelegramChannelClick: () -> Unit,
    onTelegramBotClick: () -> Unit,
    onSubInfoButtonClick: (String) -> Unit,
    onOpenSiteClick: () -> Unit,
    onCloseApp: () -> Unit,
    onSaveConnectionSettings: (ru.nubovpn.app.data.ConnectionSettingsState) -> Unit,
    onSubscriptionAutoUpdateIntervalChange: (ru.nubovpn.app.data.SubscriptionAutoUpdateInterval) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    var settingsPage by remember { mutableStateOf(SettingsPage.Main) }
    var showDeleteSubscriptionConfirm by remember { mutableStateOf(false) }

    val hasSubscription = state.subscriptionUrl.isNotBlank() && state.nodes.isNotEmpty()

    val inSettingsSubPage = selectedTab == AppTab.Settings && settingsPage != SettingsPage.Main

    val canNavigateBack =
        showDeleteSubscriptionConfirm ||
            inSettingsSubPage ||
            selectedTab != AppTab.Home

    fun navigateBack() {
        when {
            showDeleteSubscriptionConfirm -> showDeleteSubscriptionConfirm = false
            inSettingsSubPage -> settingsPage = settingsPage.parentPage() ?: SettingsPage.Main
            selectedTab != AppTab.Home -> {
                settingsPage = SettingsPage.Main
                selectedTab = AppTab.Home
            }
        }
    }

    BackHandler(enabled = canNavigateBack) {
        navigateBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = nuboColors().background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                if (inSettingsSubPage) {
                    NuboTopBar(
                        title = settingsPage.headerTitle,
                        showBackButton = true,
                        onBackClick = { navigateBack() },
                    )
                } else if (selectedTab == AppTab.Home) {
                    HomeTopBar(
                        onGlobeClick = onOpenSiteClick,
                    )
                } else {
                    NuboTopBar(
                        title = when (selectedTab) {
                            AppTab.Settings -> "Настройки"
                            AppTab.Home -> null
                        },
                    )
                }
            }
        },
        bottomBar = {
            NuboBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab != AppTab.Settings) {
                        settingsPage = SettingsPage.Main
                    }
                    selectedTab = tab
                },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            )
        },
    ) { padding ->
        when (selectedTab) {
            AppTab.Home -> HomeScreen(
                modifier = Modifier.padding(padding),
                state = state,
                onConnectClick = onConnectClick,
                onDisconnectClick = onDisconnectClick,
                onSelectNode = onSelectNode,
                onRefreshConfig = onRefreshConfig,
                onPasteLinkClick = onPasteLinkClick,
                onScanQrClick = onScanQrClick,
                onTelegramBotClick = onTelegramBotClick,
                onOpenSiteClick = onOpenSiteClick,
                onSubInfoButtonClick = onSubInfoButtonClick,
                onRefreshPing = onRefreshPing,
                onToggleSortByPing = onToggleSortByPing,
                onOpenSubscriptionSettings = {
                    selectedTab = AppTab.Settings
                    settingsPage = SettingsPage.Subscription
                },
            )
            AppTab.Settings -> SettingsScreen(
                modifier = Modifier.padding(padding),
                page = settingsPage,
                onPageChange = { settingsPage = it },
                appVersion = BuildConfig.VERSION_NAME,
                hasSubscription = hasSubscription,
                subscriptionInfo = state.subscriptionInfo,
                connectionSettings = state.connectionSettings,
                onSaveConnectionSettings = onSaveConnectionSettings,
                subscriptionAutoUpdateInterval = state.subscriptionAutoUpdateInterval,
                onSubscriptionAutoUpdateIntervalChange = onSubscriptionAutoUpdateIntervalChange,
                onPasteLink = onPasteLinkClick,
                onScanQr = onScanQrClick,
                onOpenSite = onOpenSiteClick,
                onRefreshSubscription = onRefreshSubscription,
                onDeleteSubscription = { showDeleteSubscriptionConfirm = true },
                onTelegramBot = onTelegramBotClick,
                onShowLogs = onShowLogs,
                onDownloadLogs = onDownloadLogs,
                onTelegramChannel = onTelegramChannelClick,
                onCloseApp = onCloseApp,
                subscriptionLoad = state.subscriptionLoad,
            )
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
                        settingsPage = SettingsPage.Main
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
