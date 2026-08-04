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
 * TCP-пинг мимо VPN — через [PingNetworkBypass].
 */
object ServerPinger {
    /** Общий лимит на один сервер (DNS + TCP). */
    const val PER_NODE_TIMEOUT_MS = 4_000L

    private const val TCP_CONNECT_TIMEOUT_MS = 2_000
    private const val DNS_TIMEOUT_MS = 2_000L
    private const val MAX_CONCURRENT = 12

    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "tcp-ping").apply { isDaemon = true }
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
                    AppLog.i("tcp-ping ${node.name} ${node.host}:${node.port} -> $state")
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
        tcpConnectTime(target, node.port)?.let { return it }
        if (node.isHysteria2) {
            for (fallbackPort in listOf(443, 80)) {
                if (fallbackPort != node.port) {
                    tcpConnectTime(target, fallbackPort)?.let { return it }
                }
            }
        }
        return null
    }

    private fun tcpConnectTime(host: String, port: Int): Int? =
        runWithHardTimeout(TCP_CONNECT_TIMEOUT_MS.toLong()) {
            PingNetworkBypass.tcpConnect(host, port, TCP_CONNECT_TIMEOUT_MS)
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
