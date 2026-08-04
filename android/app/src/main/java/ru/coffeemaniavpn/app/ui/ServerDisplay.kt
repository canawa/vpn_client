package ru.coffeemaniavpn.app.ui

import androidx.compose.runtime.Immutable
import ru.coffeemaniavpn.app.data.PingState
import ru.coffeemaniavpn.app.data.ProxyNode

@Immutable
data class ServerDisplay(
    val flag: String,
    val title: String,
    val subtitle: String,
    val protocolLabel: String,
    val pingText: String,
    val pingMs: Int?,
    val group: String? = null,
)

object ServerDisplayMapper {
    private val ipv4Regex = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

    /** Группа из имени `Premium | 🇩🇪 Frankfurt`. */
    fun adminGroup(rawName: String): String? {
        val pipe = rawName.indexOf('|')
        if (pipe <= 0) return null
        val group = rawName.substring(0, pipe).trim()
        if (group.isBlank()) return null
        if (group.contains("автовыбор", ignoreCase = true)) return null
        return group
    }

    fun nameWithoutGroup(rawName: String): String {
        val pipe = rawName.indexOf('|')
        return if (pipe >= 0) rawName.substring(pipe + 1).trim() else rawName.trim()
    }
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

    /**
     * Снимает ведущие flag emoji / regional indicators с имени,
     * чтобы в title не оставались эмодзи.
     */
    private fun splitFlag(name: String): Pair<String, String> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return FlagUtils.DEFAULT_FLAG_EMOJI to ""

        val cps = trimmed.codePoints().toArray().toMutableList()
        // Drop variation selectors / ZWJ at start noise
        while (cps.isNotEmpty() && (cps[0] == 0xFE0F || cps[0] == 0x200D)) {
            cps.removeAt(0)
        }

        if (cps.size >= 2) {
            val base = 0x1F1E6
            val a = cps[0] - base
            val b = cps[1] - base
            if (a in 0..25 && b in 0..25) {
                val flag = String(intArrayOf(cps[0], cps[1]), 0, 2)
                var restIdx = 2
                // skip VS16 after flag
                if (restIdx < cps.size && cps[restIdx] == 0xFE0F) restIdx++
                val rest = if (restIdx < cps.size) {
                    String(cps.subList(restIdx, cps.size).toIntArray(), 0, cps.size - restIdx).trim()
                } else {
                    ""
                }
                return flag to rest
            }
        }

        // Globe / generic pictograph at start
        if (cps.isNotEmpty()) {
            val first = cps[0]
            val isGlobe = first == 0x1F310 || first == 0x1F30D || first == 0x1F30E ||
                first == 0x1F30F || first == 0x1F5FA
            if (isGlobe) {
                var restIdx = 1
                if (restIdx < cps.size && cps[restIdx] == 0xFE0F) restIdx++
                val flag = String(intArrayOf(first), 0, 1)
                val rest = if (restIdx < cps.size) {
                    String(cps.subList(restIdx, cps.size).toIntArray(), 0, cps.size - restIdx).trim()
                } else {
                    ""
                }
                return flag to rest
            }
        }

        // ISO code as first token: "NL Server" / "nl-ams-1"
        val firstToken = trimmed.substringBefore(' ').trim()
        if (firstToken.length == 2 && firstToken.all { it.isLetter() }) {
            val rest = trimmed.removePrefix(firstToken).trimStart(' ', '-', '_', '|')
            return firstToken.lowercase() to rest
        }

        return FlagUtils.DEFAULT_FLAG_EMOJI to trimmed
    }

    fun map(node: ProxyNode, ping: PingState? = null): ServerDisplay {
        val group = adminGroup(node.name)
        val trimmed = nameWithoutGroup(node.name)
        val (flag, withoutFlag) = splitFlag(trimmed)
        val autoSelect = isAutoSelect(trimmed, withoutFlag.substringBefore("|").trim())

        val parts = withoutFlag.split("|").map { it.trim() }.filter { it.isNotBlank() }
        val title = when {
            autoSelect -> "Автовыбор"
            parts.isEmpty() -> when {
                isIpv4Address(node.host) -> "Сервер"
                node.host.isNotBlank() -> node.host
                else -> "Сервер"
            }
            parts.size == 1 -> parts[0].let { candidate ->
                candidate.dropWhile { ch ->
                    val cp = ch.code
                    cp in 0x1F1E6..0x1F1FF || cp == 0xFE0F
                }.trim().ifBlank { candidate }
            }
            else -> parts.dropLast(1).joinToString(" | ") { part ->
                part.dropWhile { ch ->
                    val cp = ch.code
                    cp in 0x1F1E6..0x1F1FF || cp == 0xFE0F
                }.trim().ifBlank { part }
            }
        }

        val subtitle = when {
            autoSelect -> ""
            parts.size >= 2 -> parts.last().let { raw ->
                if (isHiddenSubtitle(raw, node)) "" else raw
            }
            else -> ""
        }

        val (pingText, pingMs) = when (ping) {
            null -> "—" to null
            PingState.Loading -> "…" to null
            is PingState.Result -> "${ping.latencyMs} мс" to ping.latencyMs
            PingState.Unreachable -> "нет" to null
        }

        return ServerDisplay(
            flag = flag,
            title = title,
            subtitle = subtitle,
            protocolLabel = if (node.isHysteria2) "Hysteria2" else "VLESS",
            pingText = pingText,
            pingMs = pingMs,
            group = group,
        )
    }
}
