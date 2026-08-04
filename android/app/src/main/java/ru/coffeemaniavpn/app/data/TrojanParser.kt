package ru.coffeemaniavpn.app.data

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

object TrojanParser {
    fun parse(link: String): ProxyNode? {
        val trimmed = link.trim()
        if (!trimmed.startsWith("trojan://", ignoreCase = true)) return null

        return runCatching {
            val withoutScheme = trimmed.substringAfter("trojan://")
            val namePart = withoutScheme.substringAfter("#", "")
            val mainPart = withoutScheme.substringBefore("#")

            val atIndex = mainPart.lastIndexOf('@')
            if (atIndex <= 0) return@runCatching null

            val password = URLDecoder.decode(mainPart.substring(0, atIndex), StandardCharsets.UTF_8.name())
            val hostPortQuery = mainPart.substring(atIndex + 1)
            val queryStart = hostPortQuery.indexOf('?')
            val hostPort = if (queryStart >= 0) hostPortQuery.substring(0, queryStart) else hostPortQuery
            val query = if (queryStart >= 0) hostPortQuery.substring(queryStart + 1) else ""

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
            val typeParam = params["type"]?.lowercase().orEmpty()
            val transport = when (typeParam) {
                "grpc", "gun" -> "grpc"
                "ws", "websocket" -> "ws"
                else -> "tcp"
            }
            val alpn = params["alpn"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            val security = params["security"]?.takeIf { it.isNotBlank() } ?: "tls"

            ProxyNode(
                id = UUID.nameUUIDFromBytes(trimmed.toByteArray()).toString(),
                name = name,
                protocol = "trojan",
                host = host,
                port = port,
                password = password,
                uuid = password,
                security = security,
                sni = params["sni"],
                fingerprint = params["fp"],
                insecureTls = params["allowInsecure"] == "1" || params["insecure"] == "1",
                alpn = alpn?.takeIf { it.isNotEmpty() },
                transport = transport,
                grpcServiceName = params["serviceName"]?.takeIf { it.isNotBlank() },
                wsPath = params["path"]?.takeIf { transport == "ws" || transport == "websocket" },
                wsHost = params["host"]?.takeIf { transport == "ws" || transport == "websocket" },
            ).withBuiltOutbound()
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
