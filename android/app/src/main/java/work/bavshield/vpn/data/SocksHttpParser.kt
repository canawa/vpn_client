package work.bavshield.vpn.data

import java.util.UUID

object SocksHttpParser {
    fun parseSocks(link: String): ProxyNode? {
        val trimmed = link.trim()
        if (!trimmed.startsWith("socks://", ignoreCase = true) &&
            !trimmed.startsWith("socks5://", ignoreCase = true)
        ) {
            return null
        }
        return parseUserHost(trimmed, protocol = "socks")
    }

    fun parseHttp(link: String): ProxyNode? {
        val trimmed = link.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true)) return null
        if (!trimmed.substringAfter("://").contains('@')) return null
        return parseUserHost(trimmed, protocol = "http")
    }

    private fun parseUserHost(link: String, protocol: String): ProxyNode? = runCatching {
        val payload = link.substringAfter("://")
        val namePart = payload.substringAfter("#", "")
        val mainPart = payload.substringBefore("#")
        val split = ShareLinkCodec.splitUserinfo(mainPart)
        val host: String
        val port: Int
        val username: String?
        val password: String?
        if (split != null) {
            val (userinfo, hostPort, _) = split
            val parsed = ShareLinkCodec.parseHostPort(hostPort) ?: return@runCatching null
            host = parsed.first
            port = parsed.second
            val decoded = ShareLinkCodec.decode(userinfo)
            val colon = decoded.indexOf(':')
            if (colon >= 0) {
                username = decoded.substring(0, colon).ifBlank { null }
                password = decoded.substring(colon + 1).ifBlank { null }
            } else {
                username = decoded.ifBlank { null }
                password = null
            }
        } else {
            val parsed = ShareLinkCodec.parseHostPort(mainPart.substringBefore('?')) ?: return@runCatching null
            host = parsed.first
            port = parsed.second
            username = null
            password = null
        }
        val name = ShareLinkCodec.fragmentName(namePart, "$host:$port")
        ProxyNode(
            id = UUID.nameUUIDFromBytes(link.toByteArray()).toString(),
            name = name,
            protocol = protocol,
            host = host,
            port = port,
            username = username,
            password = password,
            uuid = password.orEmpty(),
            security = "none",
            transport = "tcp",
        )
    }.getOrNull()
}
