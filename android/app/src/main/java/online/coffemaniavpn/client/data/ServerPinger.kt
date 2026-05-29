package online.coffemaniavpn.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import online.coffemaniavpn.client.util.AppLog
import java.net.InetSocketAddress
import java.net.Socket

sealed interface PingState {
    data object Loading : PingState
    data class Result(val latencyMs: Int) : PingState
    data object Unreachable : PingState
}

object ServerPinger {
    private const val TIMEOUT_MS = 4_000
    private const val MAX_CONCURRENT = 4

    suspend fun pingAll(
        nodes: List<ProxyNode>,
        onUpdate: suspend (nodeId: String, state: PingState) -> Unit,
    ) = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT)
        nodes.map { node ->
            async {
                semaphore.withPermit {
                    onUpdate(node.id, PingState.Loading)
                    val latencyMs = tcpConnectLatency(node.host, node.port)
                    val state = latencyMs?.let(PingState::Result) ?: PingState.Unreachable
                    AppLog.i("ping ${node.name} ${node.host}:${node.port} -> $state")
                    onUpdate(node.id, state)
                }
            }
        }.awaitAll()
    }

    private suspend fun tcpConnectLatency(host: String, port: Int): Int? = withContext(Dispatchers.IO) {
        val startedAt = System.nanoTime()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), TIMEOUT_MS)
            }
            ((System.nanoTime() - startedAt) / 1_000_000L).toInt().coerceAtLeast(1)
        } catch (_: Exception) {
            null
        }
    }
}
