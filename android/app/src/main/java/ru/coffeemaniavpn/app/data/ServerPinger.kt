package ru.coffeemaniavpn.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.util.AppLog
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

sealed interface PingState {
    data object Loading : PingState
    data class Result(val latencyMs: Int) : PingState
    data object Unreachable : PingState
    data object Timeout : PingState
}

/**
 * TCP-пинг: время рукопожатия до host:port (как PingService.swift на macOS).
 * Hysteria2 (UDP) меряется по TCP 443/80 того же хоста.
 */
object ServerPinger {
    private const val PING_TIMEOUT_MS = 5_000L
    private const val TCP_CONNECT_TIMEOUT_MS = 3_000
    private const val MAX_CONCURRENT = 12
    private const val RETRY_THRESHOLD_MS = 600

    private val ipCache = ConcurrentHashMap<String, String>()
    private var cachedNetwork: Network? = null
    private var networkCachedAtMs = 0L

    suspend fun pingAll(
        nodes: List<ProxyNode>,
        onUpdate: suspend (nodeId: String, state: PingState) -> Unit,
    ) = coroutineScope {
        val concurrency = nodes.size.coerceAtMost(MAX_CONCURRENT).coerceAtLeast(1)
        val semaphore = Semaphore(concurrency)
        nodes.map { node ->
            async {
                semaphore.withPermit {
                    onUpdate(node.id, PingState.Loading)
                    val state = measurePing(node)
                    AppLog.i("tcp-ping ${node.name} ${node.host}:${node.port} -> $state")
                    onUpdate(node.id, state)
                }
            }
        }.awaitAll()
    }

    private suspend fun measurePing(node: ProxyNode): PingState {
        return try {
            withTimeout(PING_TIMEOUT_MS) {
                probe(node)?.let { PingState.Result(it) } ?: PingState.Unreachable
            }
        } catch (_: TimeoutCancellationException) {
            PingState.Timeout
        } catch (e: CancellationException) {
            throw e
        }
    }

    /** Если первое значение аномально большое (SYN-ретрансмит), перепроверяем. */
    private suspend fun probe(node: ProxyNode): Int? {
        val first = probeOnce(node)
        if (first != null && first > RETRY_THRESHOLD_MS) {
            val second = probeOnce(node)
            if (second != null) return minOf(first, second)
        }
        return first
    }

    private suspend fun probeOnce(node: ProxyNode): Int? {
        val target = resolveHost(node.host) ?: node.host
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

    private suspend fun tcpConnectTime(host: String, port: Int): Int? = withContext(Dispatchers.IO) {
        if (port !in 1..65535) return@withContext null
        val socket = Socket()
        try {
            physicalNetwork(App.instance)?.bindSocket(socket)
            val start = System.nanoTime()
            socket.connect(InetSocketAddress(host, port), TCP_CONNECT_TIMEOUT_MS)
            val elapsedMs = ((System.nanoTime() - start) / 1_000_000L).toInt().coerceAtLeast(1)
            elapsedMs
        } catch (_: Exception) {
            null
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun resolveHost(host: String): String? {
        val trimmed = host.trim()
        if (trimmed.isEmpty()) return null
        if (isIpAddress(trimmed)) return trimmed
        ipCache[trimmed]?.let { return it }
        return runCatching {
            InetAddress.getByName(trimmed).hostAddress?.also { ipCache[trimmed] = it }
        }.getOrNull()
    }

    private fun isIpAddress(host: String): Boolean =
        host.all { it.isDigit() || it == '.' || it == ':' || it == '[' || it == ']' }

    /** Wi‑Fi / мобильная сеть — мимо VPN-туннеля. */
    private fun physicalNetwork(context: Context): Network? {
        val now = System.currentTimeMillis()
        if (cachedNetwork != null && now - networkCachedAtMs < 30_000L) {
            return cachedNetwork
        }
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null
        val network = cm.allNetworks.firstOrNull { net ->
            val caps = cm.getNetworkCapabilities(net) ?: return@firstOrNull false
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                (
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
        }
        cachedNetwork = network
        networkCachedAtMs = now
        return network
    }
}
