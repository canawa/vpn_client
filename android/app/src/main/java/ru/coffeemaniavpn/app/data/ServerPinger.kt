package ru.coffeemaniavpn.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import libv2ray.Libv2ray
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.util.AppLog
import ru.coffeemaniavpn.app.vpn.XrayCoreManager

sealed interface PingState {
    data object Loading : PingState
    data class Result(val latencyMs: Int) : PingState
    data object Unreachable : PingState
}

object ServerPinger {
    private const val DELAY_TEST_URL = "https://www.gstatic.com/generate_204"
    private const val MAX_CONCURRENT = 3

    suspend fun pingAll(
        nodes: List<ProxyNode>,
        onUpdate: suspend (nodeId: String, state: PingState) -> Unit,
    ) = coroutineScope {
        ensureXrayReady()
        val semaphore = Semaphore(MAX_CONCURRENT)
        nodes.map { node ->
            async {
                semaphore.withPermit {
                    onUpdate(node.id, PingState.Loading)
                    val latencyMs = httpGetLatency(node)
                    val state = latencyMs?.let(PingState::Result) ?: PingState.Unreachable
                    AppLog.i("ping ${node.name} ${node.host}:${node.port} GET $DELAY_TEST_URL -> $state")
                    onUpdate(node.id, state)
                }
            }
        }.awaitAll()
    }

    private fun ensureXrayReady() {
        if (App.xrayReady.get()) return
        runCatching {
            XrayCoreManager.init(App.instance)
            App.xrayReady.set(true)
        }.onFailure {
            AppLog.e("ping: xray init failed", it)
        }
    }

    private suspend fun httpGetLatency(node: ProxyNode): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val config = XrayConfigBuilder.buildForDelayTest(node)
            val delayMs = Libv2ray.measureOutboundDelay(config, DELAY_TEST_URL)
            if (delayMs < 0L) null else delayMs.toInt().coerceAtLeast(1)
        }.getOrElse {
            AppLog.e("ping failed ${node.name}", it)
            null
        }
    }
}
