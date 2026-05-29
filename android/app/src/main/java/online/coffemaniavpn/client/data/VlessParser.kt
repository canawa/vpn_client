package online.coffemaniavpn.client.data

import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

object VlessParser {
    fun parse(link: String): ProxyNode? {
        val trimmed = link.trim()
        if (!trimmed.startsWith("vless://", ignoreCase = true)) return null

        return runCatching {
            val withoutScheme = trimmed.substringAfter("vless://")
            val namePart = withoutScheme.substringAfter("#", "")
            val mainPart = withoutScheme.substringBefore("#")

            val atIndex = mainPart.lastIndexOf('@')
            if (atIndex <= 0) return@runCatching null

            val uuid = URLDecoder.decode(mainPart.substring(0, atIndex), StandardCharsets.UTF_8.name())
            val hostPortQuery = mainPart.substring(atIndex + 1)
            val queryStart = hostPortQuery.indexOf('?')
            val hostPort = if (queryStart >= 0) {
                hostPortQuery.substring(0, queryStart)
            } else {
                hostPortQuery
            }
            val query = if (queryStart >= 0) {
                hostPortQuery.substring(queryStart + 1)
            } else {
                ""
            }

            val host: String
            val port: Int
            if (hostPort.startsWith("[")) {
                val end = hostPort.indexOf(']')
                host = hostPort.substring(1, end)
                port = hostPort.substring(end + 1).removePrefix(":").toInt()
            } else {
                val colon = hostPort.lastIndexOf(':')
                host = hostPort.substring(0, colon)
                port = hostPort.substring(colon + 1).toInt()
            }

            val params = parseQuery(query)
            val name = URLDecoder.decode(namePart, StandardCharsets.UTF_8.name())
                .ifBlank { "$host:$port" }

            ProxyNode(
                id = UUID.nameUUIDFromBytes(trimmed.toByteArray()).toString(),
                name = name,
                protocol = "vless",
                uuid = uuid,
                host = host,
                port = port,
                encryption = params["encryption"] ?: "none",
                flow = params["flow"],
                security = params["security"] ?: "none",
                sni = params["sni"],
                fingerprint = params["fp"],
                publicKey = params["pbk"],
                shortId = params["sid"],
                spiderX = params["spx"],
            )
        }.getOrNull()
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            val key = URLDecoder.decode(part.substring(0, idx), StandardCharsets.UTF_8.name())
            val value = URLDecoder.decode(part.substring(idx + 1), StandardCharsets.UTF_8.name())
            key to value
        }.toMap()
    }
}
