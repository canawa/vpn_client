package online.coffemaniavpn.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import online.coffemaniavpn.client.vpn.VpnStatus

@Composable
fun HomeScreen(
    state: MainUiState,
    selectedDisplay: ServerDisplay?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onOpenServers: () -> Unit,
    onTelegramClick: () -> Unit,
    onPasteLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = state.vpnStatus == VpnStatus.Started

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
                text = statusHeadline(state.vpnStatus),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = CoffemaniaColors.Espresso,
            )
        }

        BrewConnectButton(
            vpnStatus = state.vpnStatus,
            enabled = state.nodes.isNotEmpty(),
            onClick = {
                if (isConnected) onDisconnectClick() else onConnectClick()
            },
        )

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

        Column(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Подписка")
            SubscriptionCard(
                onTelegramClick = onTelegramClick,
                onPasteLinkClick = onPasteLinkClick,
            )
            state.message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoffemaniaColors.Mocha,
                    modifier = Modifier.padding(top = 12.dp, start = 8.dp),
                )
            }
        }
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
