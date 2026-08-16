package ru.coffeemaniavpn.app.data

import ru.coffeemaniavpn.app.ui.FlagUtils
import ru.coffeemaniavpn.app.ui.ServerCategory
import ru.coffeemaniavpn.app.ui.ServerDisplayMapper

object LoadBalancer {
    const val AUTO_NODE_ID = "__auto__"

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

    fun hasBypassServers(nodes: List<ProxyNode>): Boolean =
        nodes.any { ServerCategory.BYPASS.matches(it.name) }

    /** Лучший пинг среди серверов подписки (без remote-auto и без России). */
    fun pickBest(nodes: List<ProxyNode>, pings: Map<String, PingState>): ProxyNode? {
        val pool = connectableNodes(nodes)
        val candidates = pool.filterNot(::isRussianServer)
        val source = candidates.ifEmpty { pool }
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
