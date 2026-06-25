package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.coffeemaniavpn.app.vpn.VpnStatus

@Composable
fun HomeScreen(
    state: MainUiState,
    selectedDisplay: ServerDisplay?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onOpenServers: () -> Unit,
    onPasteLinkClick: () -> Unit,
    onBuyOnWebsiteClick: () -> Unit,
    onRenewTelegramClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = state.vpnStatus == VpnStatus.Started
    val hasSubscription = state.subscriptionUrl.isNotBlank() && state.nodes.isNotEmpty()
    val subscriptionExpired = state.subscriptionInfo?.isExpired() == true
    val canConnect = hasSubscription && !subscriptionExpired
    val connectEnabled = when {
        subscriptionExpired -> isConnected
        else -> isConnected || canConnect
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        state.startupCrash?.let {
            ErrorBanner(text = "Последний краш: $it")
        }
        state.error?.let { ErrorBanner(text = it) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SectionLabel("Статус")
            Text(
                text = if (subscriptionExpired) "Подписка истекла" else statusHeadline(state.vpnStatus),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = if (subscriptionExpired) {
                    MaterialTheme.colorScheme.error
                } else {
                    coffemaniaColors().espresso
                },
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            BrewConnectButton(
                vpnStatus = state.vpnStatus,
                connectionElapsedMs = state.connectionElapsedMs,
                enabled = connectEnabled,
                onClick = {
                    if (isConnected) onDisconnectClick() else onConnectClick()
                },
            )

            if (hasSubscription && subscriptionExpired) {
                SubscriptionExpiredCard(
                    onRenewTelegramClick = onRenewTelegramClick,
                    onRenewWebsiteClick = onBuyOnWebsiteClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (!subscriptionExpired) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Текущий сервер")
                if (selectedDisplay != null) {
                    SelectedServerCard(
                        display = selectedDisplay,
                        onClick = onOpenServers,
                    )
                } else {
                    EmptyServerHint()
                }
            }
        }

        if (!hasSubscription) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Подписка")
                SubscriptionCard(
                    onPasteLinkClick = onPasteLinkClick,
                    onBuyOnWebsiteClick = onBuyOnWebsiteClick,
                )
                state.message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = coffemaniaColors().mocha,
                        modifier = Modifier.padding(top = 12.dp, start = 8.dp),
                    )
                }
            }
        }

        WebsiteBanner(onClick = onBuyOnWebsiteClick)
    }
}

@Composable
private fun EmptyServerHint() {
    Text(
        text = "Добавьте подписку, чтобы выбрать сервер",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}

@Composable
private fun ErrorBanner(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
    )
}
