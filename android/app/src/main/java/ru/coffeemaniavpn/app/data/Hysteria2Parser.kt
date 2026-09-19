package ru.coffeemaniavpn.app.data

import org.json.JSONObject
import ru.coffeemaniavpn.app.util.AppLog
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

object Hysteria2Parser {
    fun parseUri(link: String): ProxyNode? {
        val trimmed = link.trim()
        val scheme = when {
            trimmed.startsWith("hy2://", ignoreCase = true) -> "hy2://"
            trimmed.startsWith("hysteria2://", ignoreCase = true) -> "hysteria2://"
            else -> return null
        }

        return runCatching {
            val withoutScheme = trimmed.substringAfter(scheme)
            val namePart = withoutScheme.substringAfter("#", "")
            val mainPart = withoutScheme.substringBefore("#")
            val atIndex = mainPart.lastIndexOf('@')

            val passwordRaw: String
            val hostPortQuery: String
            if (atIndex > 0) {
                passwordRaw = mainPart.substring(0, atIndex)
                hostPortQuery = mainPart.substring(atIndex + 1)
            } else {
                passwordRaw = ""
                hostPortQuery = mainPart
            }

            val queryStart = hostPortQuery.indexOf('?')
            val hostPort = if (queryStart >= 0) hostPortQuery.substring(0, queryStart) else hostPortQuery
            val query = if (queryStart >= 0) hostPortQuery.substring(queryStart + 1) else ""
            val params = parseQuery(query)

            val password = URLDecoder.decode(passwordRaw, StandardCharsets.UTF_8.name())
                .ifBlank { params["auth"].orEmpty() }
            if (password.isBlank()) {
                AppLog.w("Hysteria2Parser missing auth uri=${trimmed.take(48)}")
                return@runCatching null
            }

            val (host, portRaw) = splitHostPort(hostPort)
            if (host.isBlank()) {
                AppLog.w("Hysteria2Parser missing host uri=${trimmed.take(48)}")
                return@runCatching null
            }
            val port = PortSpec.firstPortFromText(portRaw, default = 443)

            val name = URLDecoder.decode(namePart, StandardCharsets.UTF_8.name())
                .ifBlank { "$host:$port" }

            val (obfsType, obfsPassword) = parseObfsFromParams(params)

            ProxyNode(
                id = UUID.nameUUIDFromBytes(trimmed.toByteArray()).toString(),
                name = name,
                protocol = "hysteria2",
                host = host,
                port = port,
                password = password,
                uuid = password,
                sni = params["sni"] ?: params["peer"],
                fingerprint = params["fp"] ?: params["fingerprint"],
                obfsType = obfsType ?: params["obfs"],
                obfsPassword = obfsPassword ?: params["obfs-password"],
                insecureTls = params["insecure"] == "1" || params["allowInsecure"] == "1",
                upMbps = params["upmbps"]?.toIntOrNull()?.takeIf { it > 0 }
                    ?: params["up"]?.toIntOrNull()?.takeIf { it > 0 },
                downMbps = params["downmbps"]?.toIntOrNull()?.takeIf { it > 0 }
                    ?: params["down"]?.toIntOrNull()?.takeIf { it > 0 },
            ).withBuiltOutbound()
        }.onFailure { e ->
            AppLog.w("Hysteria2Parser failed uri=${trimmed.take(64)}", e)
        }.getOrNull()
    }

    private fun splitHostPort(hostPort: String): Pair<String, String> {
        val trimmed = hostPort.trim()
        if (trimmed.isEmpty()) return "" to ""
        if (trimmed.startsWith("[")) {
            val end = trimmed.indexOf(']')
            if (end <= 1) return "" to ""
            val host = trimmed.substring(1, end)
            val rest = trimmed.substring(end + 1).removePrefix(":").trim()
            return host to rest
        }
        val colon = trimmed.lastIndexOf(':')
        if (colon <= 0) return trimmed to ""
        return trimmed.substring(0, colon) to trimmed.substring(colon + 1)
    }

    private fun parseObfsFromParams(params: Map<String, String>): Pair<String?, String?> {
        val fmRaw = params["fm"] ?: return null to null
        val fm = runCatching { JSONObject(fmRaw) }.getOrNull() ?: return null to null
        val quicParam = fm.optJSONArray("quicParams")?.optJSONObject(0) ?: return null to null
        val type = quicParam.optString("type").takeIf { it.isNotBlank() }
        val password = quicParam.optJSONObject("settings")
            ?.optString("password")
            ?.takeIf { it.isNotBlank() }
        return type to password
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
