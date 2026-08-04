package ru.coffeemaniavpn.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.stringResource
import ru.coffeemaniavpn.app.R
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
    var showDeleteSubscriptionConfirm by remember { mutableStateOf(false) }

    val canNavigateBack = showDeleteSubscriptionConfirm || showSettings

    fun navigateBack() {
        when {
            showDeleteSubscriptionConfirm -> showDeleteSubscriptionConfirm = false
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
        AnimatedContent(
            targetState = showSettings,
            transitionSpec = {
                fadeIn(ClevMotion.settingsEnterSpec) togetherWith fadeOut(ClevMotion.settingsExitSpec)
            },
            label = "settingsScreen",
            modifier = Modifier.padding(padding),
        ) { settingsVisible ->
            if (settingsVisible) {
                ClevSettingsHost(
                    state = state,
                    onClose = { showSettings = false },
                    onSaveConnectionSettings = onSaveConnectionSettings,
                    onRefreshSubscription = onRefreshSubscription,
                    onDeleteSubscription = { showDeleteSubscriptionConfirm = true },
                    onTrafficRoutingModeChange = onTrafficRoutingModeChange,
                    onLanguageChange = onLanguageChange,
                )
            } else {
                HomeScreen(
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
    }

    if (showDeleteSubscriptionConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSubscriptionConfirm = false },
            title = { Text(stringResource(R.string.clev_delete_subscription_title)) },
            text = { Text(stringResource(R.string.clev_delete_subscription_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteSubscriptionConfirm = false
                        showSettings = false
                        onDeleteSubscriptionClick()
                    },
                ) {
                    Text(stringResource(R.string.clev_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSubscriptionConfirm = false }) {
                    Text(stringResource(R.string.clev_cancel))
                }
            },
        )
    }
}
