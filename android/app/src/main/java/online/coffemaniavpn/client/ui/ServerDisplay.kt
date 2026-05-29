package online.coffemaniavpn.client.ui

import online.coffemaniavpn.client.data.PingState
import online.coffemaniavpn.client.data.ProxyNode

data class ServerDisplay(
    val flag: String,
    val title: String,
    val subtitle: String,
    val protocolLabel: String,
    val pingText: String,
    val pingMs: Int?,
)

object ServerDisplayMapper {
    /** Первый emoji-флаг в названии (нулевая позиция / первый токен до пробела). */
    private val firstFlagRegex = Regex("^(\\p{Regional_Indicator}{2}|\\p{Extended_Pictographic})")

    fun map(node: ProxyNode, ping: PingState? = null): ServerDisplay {
        val trimmed = node.name.trim()
        val flag = firstFlagRegex.find(trimmed)?.value
            ?: trimmed.substringBefore(' ').trim().takeIf { it.isNotEmpty() }
            ?: "🌐"
        val withoutFlag = trimmed.removePrefix(flag).trim()
        val title = withoutFlag.substringBefore("|").trim().ifBlank { node.host }
        val subtitle = withoutFlag.substringAfter("|", "").trim()
            .ifBlank { "${node.host}:${node.port}" }

        val (pingText, pingMs) = when (ping) {
            null -> "—" to null
            PingState.Loading -> "…" to null
            is PingState.Result -> "${ping.latencyMs} ms" to ping.latencyMs
            PingState.Unreachable -> "—" to null
        }

        return ServerDisplay(
            flag = flag,
            title = title,
            subtitle = subtitle,
            protocolLabel = if (node.isHysteria2) "HY2" else "VLESS",
            pingText = pingText,
            pingMs = pingMs,
        )
    }
}
