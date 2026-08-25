package work.bavshield.vpn.data

import java.util.UUID

object ShadowsocksParser {
    fun parse(link: String): ProxyNode? {
        val trimmed = link.trim()
        if (!trimmed.startsWith("ss://", ignoreCase = true)) return null
        return runCatching {
            val payload = trimmed.substringAfter("://")
            val namePart = payload.substringAfter("#", "")
            val withoutName = payload.substringBefore("#")
            val query = withoutName.substringAfter("?", "")
            val mainPart = withoutName.substringBefore("?")

            val methodPassword: String
            val host: String
            val port: Int

            if (mainPart.contains('@')) {
                val (userinfo, hostPort, _) = ShareLinkCodec.splitUserinfo(mainPart) ?: return@runCatching null
                val parsedHostPort = ShareLinkCodec.parseHostPort(hostPort) ?: return@runCatching null
                host = parsedHostPort.first
                port = parsedHostPort.second
                methodPassword = decodeMethodPassword(userinfo) ?: return@runCatching null
            } else {
                val decoded = ShareLinkCodec.decodeBase64(mainPart) ?: return@runCatching null
                val at = decoded.lastIndexOf('@')
                if (at <= 0) return@runCatching null
                methodPassword = decoded.substring(0, at)
                val parsedHostPort = ShareLinkCodec.parseHostPort(decoded.substring(at + 1)) ?: return@runCatching null
                host = parsedHostPort.first
                port = parsedHostPort.second
            }

            val split = splitMethodPassword(methodPassword) ?: return@runCatching null
            val params = ShareLinkCodec.parseQuery(query)
            val stream = StreamSettingsCodec.fromUriParams(params)
            val name = ShareLinkCodec.fragmentName(namePart, "$host:$port")
            ProxyNode(
                id = UUID.nameUUIDFromBytes(trimmed.toByteArray()).toString(),
                name = name,
                protocol = "shadowsocks",
                host = host,
                port = port,
                encryption = split.first,
                password = split.second,
                uuid = split.second,
            ).withStream(stream)
        }.getOrNull()
    }

    private fun decodeMethodPassword(userinfo: String): String? {
        val decoded = ShareLinkCodec.decode(userinfo)
        if (decoded.contains(':')) return decoded
        return ShareLinkCodec.decodeBase64(userinfo)?.takeIf { it.contains(':') } ?: decoded.takeIf { it.contains(':') }
    }

    private fun splitMethodPassword(raw: String): Pair<String, String>? {
        val idx = raw.indexOf(':')
        if (idx <= 0) return null
        val method = raw.substring(0, idx).trim()
        val password = raw.substring(idx + 1)
        if (method.isBlank() || password.isBlank()) return null
        return method to password
    }
}
