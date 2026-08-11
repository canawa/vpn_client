package ru.coffeemaniavpn.app.data

import ru.coffeemaniavpn.app.ui.FlagUtils
import ru.coffeemaniavpn.app.ui.ServerDisplayMapper

object LoadBalancer {
    const val AUTO_NODE_ID = "__auto__"

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
        nodes.any { ru.coffeemaniavpn.app.ui.ServerCategory.BYPASS.matches(it.name) }

    /** Лучший пинг среди серверов подписки, кроме России. */
    fun pickBest(nodes: List<ProxyNode>, pings: Map<String, PingState>): ProxyNode? {
        val candidates = nodes.filterNot(::isRussianServer)
        if (candidates.isEmpty()) return nodes.firstOrNull()

        val withPing = candidates.mapNotNull { node ->
            when (val ping = pings[node.id]) {
                is PingState.Result -> node to ping.latencyMs
                else -> null
            }
        }
        if (withPing.isNotEmpty()) {
            return withPing.minByOrNull { it.second }?.first
        }
        return candidates.firstOrNull()
    }
}
