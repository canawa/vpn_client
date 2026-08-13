package ru.coffeemaniavpn.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.AppLanguage
import ru.coffeemaniavpn.app.data.ConnectionSettingsState
import ru.coffeemaniavpn.app.data.RoutingRuleTarget
import ru.coffeemaniavpn.app.data.SubscriptionAutoUpdateInterval
import ru.coffeemaniavpn.app.data.SubscriptionUrlValidator
import ru.coffeemaniavpn.app.data.TrafficRoutingMode

@Composable
fun AppShell(
    state: MainUiState,
    onRefreshSubscription: () -> Unit,
    onSelectNode: (String) -> Unit,
    onSelectAutoBalancer: () -> Unit,
    onConnectToNode: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPingNode: (String) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onRefreshPing: () -> Unit,
    onRefreshConfig: () -> Unit,
    onRefreshServersAndPing: () -> Unit,
    onPasteLinkClick: () -> Unit,
    onImportUrl: (String) -> Unit,
    onAcceptClipboard: () -> Unit,
    onDismissClipboard: () -> Unit,
    onDismissForeignPrompt: () -> Unit,
    onPasteNewLink: () -> Unit,
    onDeleteSubscriptionClick: () -> Unit,
    onSaveConnectionSettings: (ConnectionSettingsState) -> Unit,
    onUpdateConnectionSettings: ((ConnectionSettingsState) -> ConnectionSettingsState) -> Unit,
    onAddCustomRule: (String, RoutingRuleTarget) -> Unit,
    onAddCustomRules: (List<String>, RoutingRuleTarget) -> Unit,
    onRemoveCustomRule: (String) -> Unit,
    onSubscriptionAutoUpdateIntervalChange: (SubscriptionAutoUpdateInterval) -> Unit,
    onTrafficRoutingModeChange: (TrafficRoutingMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(XenoTab.Home) }
    var showDeleteSubscriptionConfirm by remember { mutableStateOf(false) }
    val hasSubscription = state.subscriptionUrl.isNotBlank()

    fun openWebsite() = openExternalUrl(context, SubscriptionUrlValidator.websiteUrl("home_logo"))
    fun openTelegram() = openExternalUrl(context, SubscriptionUrlValidator.telegramBotUrl("activation"))

    LaunchedEffect(hasSubscription) {
        if (!hasSubscription) tab = XenoTab.Home
    }

    BackHandler(enabled = showDeleteSubscriptionConfirm) {
        showDeleteSubscriptionConfirm = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = coffemaniaColors().milkFoam,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (hasSubscription) {
                XenoBottomNav(
                    selected = tab,
                    onSelect = { tab = it },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (!hasSubscription) {
                XenoActivationFlow(
                    modifier = Modifier.fillMaxSize(),
                    isLoading = state.isLoading,
                    error = state.error,
                    clipboardUrl = state.clipboardSubscriptionUrl,
                    showForeignPrompt = state.showForeignSubscriptionPrompt,
                    onPasteLinkClick = onPasteLinkClick,
                    onAcceptClipboard = onAcceptClipboard,
                    onDismissClipboard = onDismissClipboard,
                    onDismissForeignPrompt = onDismissForeignPrompt,
                    onBuyTelegram = ::openTelegram,
                    onBuyWebsite = ::openWebsite,
                    onImportUrl = onImportUrl,
                )
            } else {
                when (tab) {
                    XenoTab.Home -> HomeScreen(
                        state = state,
                        onConnectClick = onConnectClick,
                        onDisconnectClick = onDisconnectClick,
                        onPasteLinkClick = onPasteLinkClick,
                        onImportUrl = onImportUrl,
                        onOpenServers = { tab = XenoTab.Servers },
                        onOpenWebsite = ::openWebsite,
                        onAcceptClipboard = onAcceptClipboard,
                        onDismissClipboard = onDismissClipboard,
                        onDismissForeignPrompt = onDismissForeignPrompt,
                        onBuyTelegram = ::openTelegram,
                        onBuyWebsite = ::openWebsite,
                    )
                    XenoTab.Servers -> XenoServersScreen(
                        state = state,
                        onSelectNode = onSelectNode,
                        onSelectAutoBalancer = onSelectAutoBalancer,
                        onConnectToNode = onConnectToNode,
                        onToggleFavorite = onToggleFavorite,
                        onPingNode = onPingNode,
                        onRefreshAll = onRefreshServersAndPing,
                        modifier = Modifier.fillMaxSize(),
                    )
                    XenoTab.Settings -> XenoSettingsScreen(
                        state = state,
                        onUpdateConnectionSettings = onUpdateConnectionSettings,
                        onAddCustomRule = onAddCustomRule,
                        onAddCustomRules = onAddCustomRules,
                        onRemoveCustomRule = onRemoveCustomRule,
                        onReplaceSubscription = {
                            onDeleteSubscriptionClick()
                        },
                        onTrafficRoutingModeChange = onTrafficRoutingModeChange,
                        onLanguageChange = onLanguageChange,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    if (showDeleteSubscriptionConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSubscriptionConfirm = false },
            title = { Text(stringResource(R.string.xeno_delete_subscription_title)) },
            text = { Text(stringResource(R.string.xeno_delete_subscription_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSubscriptionConfirm = false
                        tab = XenoTab.Home
                        onDeleteSubscriptionClick()
                    },
                ) {
                    Text(stringResource(R.string.xeno_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSubscriptionConfirm = false }) {
                    Text(stringResource(R.string.xeno_cancel))
                }
            },
        )
    }
}
