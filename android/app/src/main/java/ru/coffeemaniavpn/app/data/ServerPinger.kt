package ru.coffeemaniavpn.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import ru.coffeemaniavpn.app.util.AppLog
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

sealed interface PingState {
    data object Loading : PingState
    data class Result(val latencyMs: Int) : PingState
    data object Unreachable : PingState
    data object Timeout : PingState
}

/**
 * TCP/UDP-пинг мимо VPN — через [PingNetworkBypass].
 * Hysteria2 — UDP/QUIC: TCP к порту часто молчит, поэтому короткий probe + fallback 443/80.
 */
object ServerPinger {
    /** Общий лимит на один сервер (DNS + probe), после захвата слота. */
    const val PER_NODE_TIMEOUT_MS = 5_000L
    const val MAX_CONCURRENT = 12

    private const val TCP_CONNECT_TIMEOUT_MS = 2_000
    private const val HY2_PORT_TCP_TIMEOUT_MS = 500
    private const val UDP_PROBE_TIMEOUT_MS = 400
    private const val DNS_TIMEOUT_MS = 2_000L

    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "tcp-ping").apply { isDaemon = true }
    }

    /** Оценка полного прогона списка с учётом очереди (для UI-watchdog). */
    fun estimatedBatchTimeoutMs(nodeCount: Int): Long {
        if (nodeCount <= 0) return PER_NODE_TIMEOUT_MS
        val waves = (nodeCount + MAX_CONCURRENT - 1) / MAX_CONCURRENT
        return waves * PER_NODE_TIMEOUT_MS + 2_000L
    }

    suspend fun pingAll(
        nodes: List<ProxyNode>,
        onUpdate: suspend (nodeId: String, state: PingState) -> Unit,
    ) = coroutineScope {
        PingNetworkBypass.ensureListening()
        val concurrency = nodes.size.coerceAtMost(MAX_CONCURRENT).coerceAtLeast(1)
        val semaphore = Semaphore(concurrency)
        nodes.map { node ->
            async {
                semaphore.withPermit {
                    onUpdate(node.id, PingState.Loading)
                    val state = runCatching { measurePing(node) }
                        .getOrElse { e ->
                            if (e is CancellationException) throw e
                            PingState.Unreachable
                        }
                    AppLog.i("tcp-ping ${node.name} ${node.host}:${node.port} hy2=${node.isHysteria2} -> $state")
                    onUpdate(node.id, state)
                }
            }
        }.awaitAll()
    }

    private fun measurePing(node: ProxyNode): PingState {
        val latencyMs = runWithHardTimeout(PER_NODE_TIMEOUT_MS) { probe(node) }
        return latencyMs?.let { PingState.Result(it) } ?: PingState.Unreachable
    }

    private fun probe(node: ProxyNode): Int? {
        val target = runWithHardTimeout(DNS_TIMEOUT_MS) {
            PingNetworkBypass.resolveHost(node.host)
        } ?: node.host

        if (node.isHysteria2) {
            // UDP/QUIC: полный TCP-таймаут на hy2-порт съедает бюджет и не даёт дойти до 443/80.
            PingNetworkBypass.udpProbe(target, node.port, UDP_PROBE_TIMEOUT_MS)?.let { return it }
            tcpConnectTime(target, node.port, HY2_PORT_TCP_TIMEOUT_MS)?.let { return it }
            for (fallbackPort in listOf(443, 80)) {
                if (fallbackPort != node.port) {
                    tcpConnectTime(target, fallbackPort)?.let { return it }
                }
            }
            return null
        }

        return tcpConnectTime(target, node.port)
    }

    private fun tcpConnectTime(
        host: String,
        port: Int,
        timeoutMs: Int = TCP_CONNECT_TIMEOUT_MS,
    ): Int? =
        runWithHardTimeout(timeoutMs.toLong()) {
            PingNetworkBypass.tcpConnect(host, port, timeoutMs)
        }

    private fun <T> runWithHardTimeout(timeoutMs: Long, block: () -> T): T? {
        val future = executor.submit(Callable { block() })
        return try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            future.cancel(true)
            null
        } catch (_: Exception) {
            future.cancel(true)
            null
        }
    }
}
