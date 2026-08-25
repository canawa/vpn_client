package work.bavshield.vpn.data

import android.util.Base64
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal object ShareLinkCodec {
    fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            decode(part.substring(0, idx)) to decode(part.substring(idx + 1))
        }.toMap()
    }

    fun parseHostPort(hostPort: String): Pair<String, Int>? {
        if (hostPort.isBlank()) return null
        return if (hostPort.startsWith("[")) {
            val end = hostPort.indexOf(']')
            if (end <= 1) return null
            val host = hostPort.substring(1, end)
            val port = hostPort.substring(end + 1).removePrefix(":").toIntOrNull() ?: return null
            host to port
        } else {
            val colon = hostPort.lastIndexOf(':')
            if (colon <= 0) return null
            val host = hostPort.substring(0, colon)
            val port = hostPort.substring(colon + 1).toIntOrNull() ?: return null
            host to port
        }
    }

    fun splitUserinfo(mainPart: String): Triple<String, String, String>? {
        val atIndex = mainPart.lastIndexOf('@')
        if (atIndex <= 0) return null
        val userinfo = mainPart.substring(0, atIndex)
        val rest = mainPart.substring(atIndex + 1)
        val queryStart = rest.indexOf('?')
        val hostPort = if (queryStart >= 0) rest.substring(0, queryStart) else rest
        val query = if (queryStart >= 0) rest.substring(queryStart + 1) else ""
        return Triple(userinfo, hostPort, query)
    }

    fun fragmentName(encoded: String, fallback: String): String =
        decode(encoded).ifBlank { fallback }

    fun decode(value: String): String = runCatching {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }.getOrDefault(value)

    fun parseAlpn(raw: String?): List<String>? =
        raw?.split(',', ';')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }

    fun decodeBase64(input: String): String? {
        val normalized = input.replace(Regex("\\s"), "")
        if (normalized.isEmpty()) return null
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        val flags = intArrayOf(
            Base64.DEFAULT,
            Base64.NO_WRAP,
            Base64.URL_SAFE or Base64.NO_WRAP,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        for (flag in flags) {
            runCatching {
                val decoded = String(Base64.decode(padded, flag), Charsets.UTF_8).trim()
                if (decoded.isNotEmpty()) return decoded
            }
        }
        return null
    }
}
