package work.bavshield.vpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import work.bavshield.vpn.util.AppLog
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

sealed interface PingState {
    data object Loading : PingState
    data class Result(val latencyMs: Int) : PingState
    data object Unreachable : PingState
}

object ServerPinger {
    private const val TIMEOUT_MS = 4_000
    private const val MAX_CONCURRENT = 4

    @Volatile
    var method: PingMethod = PingMethod.DEFAULT

    suspend fun pingAll(
        nodes: List<ProxyNode>,
        method: PingMethod = this.method,
        onUpdate: suspend (nodeId: String, state: PingState) -> Unit,
    ) = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT)
        nodes.map { node ->
            async {
                semaphore.withPermit {
                    onUpdate(node.id, PingState.Loading)
                    val state = pingHost(node.host, node.port, method)
                    AppLog.i("ping/${method.logLabel} ${node.name} ${node.host}:${node.port} -> $state")
                    onUpdate(node.id, state)
                }
            }
        }.awaitAll()
    }

    suspend fun pingHost(
        host: String,
        port: Int,
        method: PingMethod = this.method,
    ): PingState {
        val latencyMs = when (method) {
            PingMethod.TCP -> tcpConnectLatency(host, port)
            PingMethod.HTTP_GET -> httpGetLatency(host, port)
        }
        return latencyMs?.let(PingState::Result) ?: PingState.Unreachable
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

    private suspend fun httpGetLatency(host: String, port: Int): Int? = withContext(Dispatchers.IO) {
        val startedAt = System.nanoTime()
        var connection: HttpURLConnection? = null
        try {
            val scheme = if (port == 443) "https" else "http"
            val url = URL("$scheme://${host.trim()}:$port/")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = false
                useCaches = false
                setRequestProperty("Connection", "close")
                setRequestProperty("User-Agent", "BAVShieldVPN-Ping/1.0")
            }
            // Any HTTP response (incl. 4xx/5xx) means the host answered.
            connection.responseCode
            ((System.nanoTime() - startedAt) / 1_000_000L).toInt().coerceAtLeast(1)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { connection?.disconnect() }
        }
    }
}
