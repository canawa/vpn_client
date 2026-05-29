package online.coffemaniavpn.client.ui

import online.coffemaniavpn.client.data.ProxyNode

data class ServerDisplay(
    val flag: String,
    val title: String,
    val subtitle: String,
    val protocolLabel: String,
    val pingMs: Int,
)

object ServerDisplayMapper {
    private val flagRegex = Regex("^(\\p{Extended_Pictographic}+)")

    fun map(node: ProxyNode, pingMs: Int? = null): ServerDisplay {
        val flag = flagRegex.find(node.name.trim())?.value ?: "🌐"
        val withoutFlag = node.name.trim().removePrefix(flag).trim()
        val title = withoutFlag.substringBefore("|").trim().ifBlank { node.host }
        val subtitle = withoutFlag.substringAfter("|", "").trim()
            .ifBlank { "${node.host}:${node.port}" }

        return ServerDisplay(
            flag = flag,
            title = title,
            subtitle = subtitle,
            protocolLabel = if (node.isHysteria2) "HY2" else "VLESS",
            pingMs = pingMs ?: fakePingMs(node.id),
        )
    }

    fun fakePingMs(nodeId: String): Int =
        (nodeId.hashCode().and(0x7FFFFFFF) % 200) + 30
}
