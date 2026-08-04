package ru.coffeemaniavpn.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import ru.coffeemaniavpn.app.data.AppLanguage
import ru.coffeemaniavpn.app.data.ConnectionSettingsState
import ru.coffeemaniavpn.app.data.SubscriptionAutoUpdateInterval
import ru.coffeemaniavpn.app.data.TrafficRoutingMode

@Composable
fun AppShell(
    state: MainUiState,
    onRefreshSubscription: () -> Unit,
    onSelectNode: (String) -> Unit,
    onSelectAuto: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onRefreshPing: () -> Unit,
    onRefreshConfig: () -> Unit,
    onPasteLinkClick: () -> Unit,
    onDeleteSubscriptionClick: () -> Unit,
    onSaveConnectionSettings: (ConnectionSettingsState) -> Unit,
    onSubscriptionAutoUpdateIntervalChange: (SubscriptionAutoUpdateInterval) -> Unit,
    onTrafficRoutingModeChange: (TrafficRoutingMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    var showSettings by remember { mutableStateOf(false) }
    var showAppsEditor by remember { mutableStateOf(false) }
    var showSitesEditor by remember { mutableStateOf(false) }
    var showDeleteSubscriptionConfirm by remember { mutableStateOf(false) }

    val canNavigateBack =
        showDeleteSubscriptionConfirm || showAppsEditor || showSitesEditor || showSettings

    fun navigateBack() {
        when {
            showDeleteSubscriptionConfirm -> showDeleteSubscriptionConfirm = false
            showAppsEditor -> showAppsEditor = false
            showSitesEditor -> showSitesEditor = false
            showSettings -> showSettings = false
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
            )
        },
    ) { padding ->
        when {
            showAppsEditor -> {
                Column(
                    Modifier
                        .padding(padding)
                        .fillMaxSize(),
                ) {
                    TextButton(onClick = { showAppsEditor = false }) {
                        Text(text = "← Назад", color = coffemaniaColors().yellow)
                    }
                    SplitTunnelAppsScreen(
                        settings = state.connectionSettings,
                        onSave = onSaveConnectionSettings,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            showSitesEditor -> {
                Column(
                    Modifier
                        .padding(padding)
                        .fillMaxSize(),
                ) {
                    TextButton(onClick = { showSitesEditor = false }) {
                        Text(text = "← Назад", color = coffemaniaColors().yellow)
                    }
                    SplitTunnelSitesScreen(
                        settings = state.connectionSettings,
                        onSave = onSaveConnectionSettings,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            showSettings -> ClevSettingsHost(
                modifier = Modifier.padding(padding),
                state = state,
                onClose = { showSettings = false },
                onSaveConnectionSettings = onSaveConnectionSettings,
                onPasteLink = {
                    showSettings = false
                    onPasteLinkClick()
                },
                onRefreshSubscription = onRefreshSubscription,
                onDeleteSubscription = { showDeleteSubscriptionConfirm = true },
                onSubscriptionAutoUpdateIntervalChange = onSubscriptionAutoUpdateIntervalChange,
                onTrafficRoutingModeChange = onTrafficRoutingModeChange,
                onLanguageChange = onLanguageChange,
                onOpenAppsEditor = { showAppsEditor = true },
                onOpenSitesEditor = { showSitesEditor = true },
            )
            else -> HomeScreen(
                modifier = Modifier.padding(padding),
                state = state,
                onConnectClick = onConnectClick,
                onDisconnectClick = onDisconnectClick,
                onPasteLinkClick = onPasteLinkClick,
                onOpenSettings = { showSettings = true },
                onSelectNode = onSelectNode,
                onSelectAuto = onSelectAuto,
                onToggleFavorite = onToggleFavorite,
                onRefreshPing = onRefreshPing,
                onRefreshConfig = onRefreshConfig,
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
