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
    /** Флаг по умолчанию для серверов без emoji в названии. */
    private const val DEFAULT_FLAG = "🇪🇺"

    private val ipv4Regex = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

    private data class EmojiMatch(val emoji: String, val startIndex: Int, val endIndex: Int)

    private fun isRegionalIndicator(cp: Int): Boolean = cp in 0x1F1E6..0x1F1FF

    private fun isEmojiCodePoint(cp: Int): Boolean = when {
        isRegionalIndicator(cp) -> true
        cp in 0x1F300..0x1FAFF -> true
        cp in 0x2600..0x26FF -> true
        cp in 0x2700..0x27BF -> true
        else -> false
    }

    /** Первый emoji в названии: флаг страны или любой символ (👑, ⚡ и т.д.). */
    private fun findFirstEmoji(text: String): EmojiMatch? {
        var index = 0
        while (index < text.length) {
            val cp = text.codePointAt(index)
            if (isRegionalIndicator(cp)) {
                val nextIndex = index + Character.charCount(cp)
                if (nextIndex < text.length) {
                    val cp2 = text.codePointAt(nextIndex)
                    if (isRegionalIndicator(cp2)) {
                        val end = nextIndex + Character.charCount(cp2)
                        return EmojiMatch(text.substring(index, end), index, end)
                    }
                }
            }
            if (isEmojiCodePoint(cp)) {
                var end = index + Character.charCount(cp)
                if (end < text.length && text.codePointAt(end) == 0xFE0F) {
                    end += Character.charCount(0xFE0F)
                }
                return EmojiMatch(text.substring(index, end), index, end)
            }
            index += Character.charCount(cp)
        }
        return null
    }

    private fun stripEmoji(text: String, match: EmojiMatch): String =
        (text.substring(0, match.startIndex) + text.substring(match.endIndex)).trim()

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
        val emojiMatch = findFirstEmoji(trimmed)
        val flag = emojiMatch?.emoji ?: DEFAULT_FLAG
        val withoutFlag = emojiMatch?.let { stripEmoji(trimmed, it) } ?: trimmed
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
