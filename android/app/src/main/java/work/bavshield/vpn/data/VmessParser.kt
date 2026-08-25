package work.bavshield.vpn.data

import org.json.JSONObject
import java.util.UUID

object VmessParser {
    fun parse(link: String): ProxyNode? {
        val trimmed = link.trim()
        if (!trimmed.startsWith("vmess://", ignoreCase = true)) return null
        val payload = trimmed.substringAfter("://")
        return if (payload.contains('@')) {
            parseUri(trimmed, payload)
        } else {
            parseLegacyJson(trimmed, payload)
        }
    }

    private fun parseUri(original: String, payload: String): ProxyNode? = runCatching {
        val namePart = payload.substringAfter("#", "")
        val mainPart = payload.substringBefore("#")
        val (uuid, hostPort, query) = ShareLinkCodec.splitUserinfo(mainPart) ?: return@runCatching null
        val (host, port) = ShareLinkCodec.parseHostPort(hostPort) ?: return@runCatching null
        val params = ShareLinkCodec.parseQuery(query)
        val stream = StreamSettingsCodec.fromUriParams(params)
        val name = ShareLinkCodec.fragmentName(namePart, "$host:$port")
        ProxyNode(
            id = UUID.nameUUIDFromBytes(original.toByteArray()).toString(),
            name = name,
            protocol = "vmess",
            uuid = ShareLinkCodec.decode(uuid),
            host = host,
            port = port,
            encryption = params["encryption"] ?: params["scy"] ?: "auto",
            alterId = params["aid"]?.toIntOrNull() ?: 0,
        ).withStream(stream)
    }.getOrNull()

    private fun parseLegacyJson(original: String, payload: String): ProxyNode? = runCatching {
        val json = ShareLinkCodec.decodeBase64(payload.substringBefore("#")) ?: return@runCatching null
        val obj = JSONObject(json)
        val host = obj.optString("add").ifBlank { obj.optString("host") }
        val port = obj.optInt("port").takeIf { it > 0 }
            ?: obj.optString("port").toIntOrNull()
            ?: return@runCatching null
        if (host.isBlank()) return@runCatching null
        val network = StreamSettingsCodec.normalizeNetwork(obj.optString("net", "tcp"))
        val tls = obj.optString("tls")
        val security = when {
            tls.equals("reality", ignoreCase = true) -> "reality"
            tls.equals("tls", ignoreCase = true) || tls == "1" -> "tls"
            else -> "none"
        }
        val name = obj.optString("ps").ifBlank { obj.optString("remarks") }.ifBlank { "$host:$port" }
        val path = obj.optString("path").takeIf { it.isNotBlank() }
            ?: obj.optString("serviceName").takeIf { it.isNotBlank() }
        ProxyNode(
            id = UUID.nameUUIDFromBytes(original.toByteArray()).toString(),
            name = name,
            protocol = "vmess",
            uuid = obj.optString("id"),
            host = host,
            port = port,
            encryption = obj.optString("scy").ifBlank { obj.optString("security") }.ifBlank { "auto" },
            alterId = obj.optInt("aid", 0),
            security = security,
            sni = obj.optString("sni").takeIf { it.isNotBlank() },
            fingerprint = obj.optString("fp").takeIf { it.isNotBlank() },
            publicKey = obj.optString("pbk").takeIf { it.isNotBlank() },
            shortId = obj.optString("sid").takeIf { it.isNotBlank() },
            alpn = ShareLinkCodec.parseAlpn(obj.optString("alpn").takeIf { it.isNotBlank() }),
            insecureTls = obj.optBoolean("allowInsecure") || obj.optString("allowInsecure") == "1",
            transport = network,
            xhttpHost = obj.optString("host").takeIf { it.isNotBlank() },
            xhttpPath = path.takeIf { network == "xhttp" },
            xhttpMode = obj.optString("mode").takeIf { it.isNotBlank() && network == "xhttp" },
            path = path,
            headerType = obj.optString("type").takeIf { it.isNotBlank() && it.lowercase() !in NETWORK_AS_TYPE },
            kcpSeed = obj.optString("seed").takeIf { it.isNotBlank() },
            grpcMultiMode = obj.optString("type").equals("multi", ignoreCase = true) ||
                obj.optBoolean("multiMode"),
        )
    }.getOrNull()

    private val NETWORK_AS_TYPE = setOf("ws", "grpc", "tcp", "kcp", "quic", "http", "h2", "httpupgrade", "xhttp", "splithttp")
}
