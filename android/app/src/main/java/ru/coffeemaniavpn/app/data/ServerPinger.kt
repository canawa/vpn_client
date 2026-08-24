package ru.coffeemaniavpn.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import libv2ray.Libv2ray
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.util.AppLog
import ru.coffeemaniavpn.app.vpn.XrayCoreManager
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
 * Реальный URL-тест через outbound (как в v2rayNG), а не TCP/ICMP до host:port.
 *
 * TCP до моста врёт на bridge/relay: США может показать ~50 мс до входа,
 * хотя end-to-end через прокси — 1000+ мс. Балансир должен выбирать по этому RTT.
 */
object ServerPinger {
    /** DNS + handshake + HTTP generate_204 через прокси. */
    const val PER_NODE_TIMEOUT_MS = 12_000L

    private const val TEST_URL = "https://www.gstatic.com/generate_204"
    /** Параллельные measureOutboundDelay тяжёлые (временный core на ноду). */
    private const val MAX_CONCURRENT = 3

    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "proxy-delay").apply { isDaemon = true }
    }

    suspend fun pingAll(
        nodes: List<ProxyNode>,
        onUpdate: suspend (nodeId: String, state: PingState) -> Unit,
    ) = coroutineScope {
        ensureCoreReady()
        val concurrency = nodes.size.coerceAtMost(MAX_CONCURRENT).coerceAtLeast(1)
        val semaphore = Semaphore(concurrency)
        nodes.map { node ->
            async {
                semaphore.withPermit {
                    onUpdate(node.id, PingState.Loading)
                    val state = runCatching { measurePing(node) }
                        .getOrElse { e ->
                            if (e is CancellationException) throw e
                            AppLog.w("proxy-delay ${node.name} failed", e)
                            PingState.Unreachable
                        }
                    AppLog.i("proxy-delay ${node.name} ${node.host}:${node.port} -> $state")
                    onUpdate(node.id, state)
                }
            }
        }.awaitAll()
    }

    private fun ensureCoreReady() {
        if (App.xrayReady.get()) return
        runCatching {
            XrayCoreManager.init(App.instance)
            App.xrayReady.set(true)
        }.onFailure {
            AppLog.e("ServerPinger: Xray init failed", it)
        }
    }

    private fun measurePing(node: ProxyNode): PingState {
        if (!App.xrayReady.get()) return PingState.Unreachable
        val latencyMs = runWithHardTimeout(PER_NODE_TIMEOUT_MS) { probe(node) }
        return when {
            latencyMs == null -> PingState.Timeout
            latencyMs <= 0 -> PingState.Unreachable
            else -> PingState.Result(latencyMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        }
    }

    private fun probe(node: ProxyNode): Long {
        val config = XrayConfigBuilder.buildForDelayTest(node)
        return Libv2ray.measureOutboundDelay(config, TEST_URL)
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
