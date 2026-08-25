package work.bavshield.vpn.data

import java.util.UUID

object VlessParser {
    fun parse(link: String): ProxyNode? {
        val trimmed = link.trim()
        if (!trimmed.startsWith("vless://", ignoreCase = true)) return null

        return runCatching {
            val payload = trimmed.substringAfter("://")
            val namePart = payload.substringAfter("#", "")
            val mainPart = payload.substringBefore("#")
            val (uuid, hostPort, query) = ShareLinkCodec.splitUserinfo(mainPart) ?: return@runCatching null
            val (host, port) = ShareLinkCodec.parseHostPort(hostPort) ?: return@runCatching null
            val params = ShareLinkCodec.parseQuery(query)
            val stream = StreamSettingsCodec.fromUriParams(params)
            val name = ShareLinkCodec.fragmentName(namePart, "$host:$port")
            val flow = params["flow"].takeIf {
                stream.network != "xhttp" && stream.network != "splithttp" && !it.isNullOrBlank()
            }

            ProxyNode(
                id = UUID.nameUUIDFromBytes(trimmed.toByteArray()).toString(),
                name = name,
                protocol = "vless",
                uuid = ShareLinkCodec.decode(uuid),
                host = host,
                port = port,
                encryption = params["encryption"] ?: "none",
                flow = flow,
            ).withStream(stream)
        }.getOrNull()
    }
}
