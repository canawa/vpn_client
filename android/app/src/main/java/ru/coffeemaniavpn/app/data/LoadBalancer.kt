package ru.coffeemaniavpn.app.data

import android.net.NetworkCapabilities
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.ui.FlagUtils
import ru.coffeemaniavpn.app.ui.ServerCategory
import ru.coffeemaniavpn.app.ui.ServerDisplayMapper

object LoadBalancer {
    const val AUTO_NODE_ID = "__auto__"

    private val LTE_TOKEN = Regex("""(^|[^a-zа-я0-9])(lte|4g|5g)([^a-zа-я0-9]|$)""")
    private val LTE_MARKERS = listOf(
        "📶",
        "мобильн",
        "cellular",
        "mobile internet",
        "для мобиль",
    )

    /** Узлы «Авто / автовыбор» из подписки — скрываем, вместо них свой балансировщик. */
    fun isRemoteAutoNode(node: ProxyNode): Boolean {
        val name = node.name
        if (ServerCategory.AUTO.matches(name)) return true
        val lower = name.lowercase()
        return "автовыбор" in lower || "auto select" in lower || "autoselect" in lower
    }

    fun connectableNodes(nodes: List<ProxyNode>): List<ProxyNode> =
        nodes.filterNot(::isRemoteAutoNode)

    fun isRussianServer(node: ProxyNode): Boolean {
        val display = ServerDisplayMapper.map(node)
        if (FlagUtils.resolveCountryCode(display.flag) == "ru") return true
        val name = node.name.lowercase()
        return "россия" in name ||
            "russia" in name ||
            " ru " in " $name " ||
            name.startsWith("ru ") ||
            name.contains("🇷🇺")
    }

    /** Серверы «для мобильного интернета» (LTE / 📶). */
    fun isLteServer(node: ProxyNode): Boolean {
        val raw = node.name
        val lower = raw.lowercase()
        if (LTE_MARKERS.any { marker -> marker in raw || marker in lower }) return true
        return LTE_TOKEN.containsMatchIn(lower)
    }

    /** Физический Wi‑Fi / Ethernet (не VPN). */
    fun isOnWifi(): Boolean {
        val cm = runCatching { App.connectivity }.getOrNull() ?: return false
        return cm.allNetworks.any { net ->
            val caps = cm.getNetworkCapabilities(net) ?: return@any false
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return@any false
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return@any false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
    }

    fun hasBypassServers(nodes: List<ProxyNode>): Boolean =
        nodes.any { ServerCategory.BYPASS.matches(it.name) }

    /** Лучший пинг среди серверов подписки (без remote-auto, без России; на Wi‑Fi без LTE). */
    fun pickBest(nodes: List<ProxyNode>, pings: Map<String, PingState>): ProxyNode? {
        val pool = connectableNodes(nodes)
        val eligible = if (isOnWifi()) pool.filterNot(::isLteServer) else pool
        val candidates = eligible.filterNot(::isRussianServer)
        val source = candidates.ifEmpty { eligible }
        if (source.isEmpty()) return null

        val withPing = source.mapNotNull { node ->
            when (val ping = pings[node.id]) {
                is PingState.Result -> node to ping.latencyMs
                else -> null
            }
        }
        if (withPing.isNotEmpty()) {
            return withPing.minByOrNull { it.second }?.first
        }
        return source.firstOrNull()
    }

    fun bestPingMs(nodes: List<ProxyNode>, pings: Map<String, PingState>): Int? {
        val best = pickBest(nodes, pings) ?: return null
        return (pings[best.id] as? PingState.Result)?.latencyMs
    }
}
