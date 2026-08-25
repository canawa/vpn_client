package work.bavshield.vpn.data

import java.util.UUID

object TrojanParser {
    fun parse(link: String): ProxyNode? {
        val trimmed = link.trim()
        if (!trimmed.startsWith("trojan://", ignoreCase = true)) return null
        return runCatching {
            val payload = trimmed.substringAfter("://")
            val namePart = payload.substringAfter("#", "")
            val mainPart = payload.substringBefore("#")
            val (password, hostPort, query) = ShareLinkCodec.splitUserinfo(mainPart) ?: return@runCatching null
            val (host, port) = ShareLinkCodec.parseHostPort(hostPort) ?: return@runCatching null
            val params = ShareLinkCodec.parseQuery(query)
            val stream = StreamSettingsCodec.fromUriParams(params).let { parsed ->
                if (parsed.security == "none") parsed.copy(security = "tls") else parsed
            }
            val name = ShareLinkCodec.fragmentName(namePart, "$host:$port")
            ProxyNode(
                id = UUID.nameUUIDFromBytes(trimmed.toByteArray()).toString(),
                name = name,
                protocol = "trojan",
                host = host,
                port = port,
                password = ShareLinkCodec.decode(password),
                uuid = ShareLinkCodec.decode(password),
            ).withStream(stream)
        }.getOrNull()
    }
}
