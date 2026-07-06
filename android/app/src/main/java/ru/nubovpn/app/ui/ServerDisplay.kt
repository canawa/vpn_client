package ru.nubovpn.app.ui

import androidx.compose.runtime.Immutable
import ru.nubovpn.app.data.PingState
import ru.nubovpn.app.data.ProxyNode

@Immutable
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
    private val ipv4Regex = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

    private fun isAutoSelect(name: String, title: String): Boolean =
        name.contains("автовыбор", ignoreCase = true) ||
            title.contains("автовыбор", ignoreCase = true)

    private fun isIpv4Address(value: String): Boolean {
        val host = value.substringBefore(':').trim()
        if (!ipv4Regex.matches(host)) return false
        return host.split('.').all { octet ->
            val n = octet.toIntOrNull() ?: return false
            n in 0..255
        }
    }

    /** Не показываем технический endpoint (IP или host:port узла) под названием сервера. */
    private fun isHiddenSubtitle(text: String, node: ProxyNode): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return true
        if (trimmed == node.host || trimmed == "${node.host}:${node.port}") return true
        if (isIpv4Address(trimmed)) return true
        return false
    }

    fun map(node: ProxyNode, ping: PingState? = null): ServerDisplay {
        val trimmed = node.name.trim()
        val flag = firstFlagRegex.find(trimmed)?.value
            ?: trimmed.substringBefore(' ').trim().takeIf { it.isNotEmpty() }
            ?: "🌐"
        val withoutFlag = trimmed.removePrefix(flag).trim()
        val autoSelect = isAutoSelect(trimmed, withoutFlag.substringBefore("|").trim())

        val title = withoutFlag.substringBefore("|").trim().let { candidate ->
            when {
                autoSelect -> "Автовыбор"
                candidate.isNotBlank() &&
                    !isIpv4Address(candidate) &&
                    candidate != "${node.host}:${node.port}" -> candidate
                isIpv4Address(node.host) -> "Сервер"
                node.host.isNotBlank() -> node.host
                else -> "Сервер"
            }
        }

        val subtitle = when {
            autoSelect -> ""
            else -> withoutFlag.substringAfter("|", "").trim().let { raw ->
                if (isHiddenSubtitle(raw, node)) "" else raw
            }
        }

        val (pingText, pingMs) = when (ping) {
            null -> "—" to null
            PingState.Loading -> "…" to null
            is PingState.Result -> "${ping.latencyMs} ms" to ping.latencyMs
            PingState.Unreachable -> "N/A" to null
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
