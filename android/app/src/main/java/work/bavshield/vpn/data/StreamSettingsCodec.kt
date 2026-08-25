package work.bavshield.vpn.data

import org.json.JSONArray
import org.json.JSONObject

internal data class ParsedStream(
    val network: String = "tcp",
    val security: String = "none",
    val sni: String? = null,
    val fingerprint: String? = null,
    val publicKey: String? = null,
    val shortId: String? = null,
    val spiderX: String? = null,
    val alpn: List<String>? = null,
    val insecureTls: Boolean = false,
    val path: String? = null,
    val host: String? = null,
    val xhttpMode: String? = null,
    val xhttpExtra: String? = null,
    val headerType: String? = null,
    val kcpSeed: String? = null,
    val quicSecurity: String? = null,
    val quicKey: String? = null,
    val grpcMultiMode: Boolean = false,
)

internal object StreamSettingsCodec {
    fun normalizeNetwork(raw: String?): String {
        val value = raw?.lowercase()?.trim().orEmpty()
        return when (value) {
            "", "tcp", "raw", "none" -> "tcp"
            "ws", "websocket" -> "ws"
            "grpc" -> "grpc"
            "httpupgrade", "http-upgrade" -> "httpupgrade"
            "xhttp", "splithttp" -> "xhttp"
            "kcp", "mkcp" -> "kcp"
            "quic" -> "quic"
            "h2", "http" -> "http"
            else -> value
        }
    }

    fun networkFromUriParams(params: Map<String, String>): String =
        normalizeNetwork(params["type"] ?: params["network"] ?: params["net"])

    fun fromUriParams(params: Map<String, String>): ParsedStream {
        val network = networkFromUriParams(params)
        val security = params["security"]?.lowercase()?.ifBlank { null }
            ?: params["tls"]?.lowercase()?.ifBlank { null }
            ?: "none"
        val path = when (network) {
            "grpc" -> params["serviceName"] ?: params["path"]
            else -> params["path"]
        }
        return ParsedStream(
            network = network,
            security = when (security) {
                "1", "true", "tls" -> "tls"
                "reality" -> "reality"
                else -> if (security == "none" || security.isBlank()) "none" else security
            },
            sni = params["sni"] ?: params["peer"],
            fingerprint = params["fp"] ?: params["fingerprint"],
            publicKey = params["pbk"] ?: params["publicKey"],
            shortId = params["sid"] ?: params["shortId"],
            spiderX = params["spx"] ?: params["spiderX"],
            alpn = ShareLinkCodec.parseAlpn(params["alpn"]),
            insecureTls = params["allowInsecure"].equals("1", true) ||
                params["allowInsecure"].equals("true", true) ||
                params["insecure"].equals("1", true) ||
                params["insecure"].equals("true", true),
            path = path,
            host = params["host"] ?: params["authority"],
            xhttpMode = params["mode"]?.takeIf { network == "xhttp" },
            xhttpExtra = XhttpParseHelper.extraFromParams(params),
            headerType = params["headerType"] ?: params["header"],
            kcpSeed = params["seed"],
            quicSecurity = params["quicSecurity"],
            quicKey = params["key"],
            grpcMultiMode = params["mode"].equals("multi", ignoreCase = true),
        )
    }

    fun parse(stream: JSONObject?): ParsedStream {
        if (stream == null) return ParsedStream()
        val network = normalizeNetwork(stream.optString("network"))
        val security = stream.optString("security").ifBlank { "none" }.lowercase()
        val tls = stream.optJSONObject("tlsSettings") ?: JSONObject()
        val reality = stream.optJSONObject("realitySettings") ?: JSONObject()
        val ws = stream.optJSONObject("wsSettings") ?: JSONObject()
        val grpc = stream.optJSONObject("grpcSettings") ?: JSONObject()
        val httpupgrade = stream.optJSONObject("httpupgradeSettings") ?: JSONObject()
        val xhttp = stream.optJSONObject("xhttpSettings")
            ?: stream.optJSONObject("splithttpSettings")
        val kcp = stream.optJSONObject("kcpSettings") ?: JSONObject()
        val quic = stream.optJSONObject("quicSettings") ?: JSONObject()
        val http = stream.optJSONObject("httpSettings") ?: JSONObject()
        val tcp = stream.optJSONObject("tcpSettings") ?: JSONObject()
        val parsedXhttp = XhttpParseHelper.parseFromSettings(xhttp)

        val path = when (network) {
            "ws" -> ws.optString("path").takeIf { it.isNotBlank() }
            "grpc" -> grpc.optString("serviceName").takeIf { it.isNotBlank() }
            "httpupgrade" -> httpupgrade.optString("path").takeIf { it.isNotBlank() }
            "xhttp" -> parsedXhttp?.path
            "http" -> http.optString("path").takeIf { it.isNotBlank() }
            "tcp" -> tcp.optJSONObject("header")?.optJSONObject("request")
                ?.optJSONArray("path")?.optString(0)?.takeIf { it.isNotBlank() }
            else -> null
        }
        val host = when (network) {
            "ws" -> ws.optString("host").ifBlank {
                ws.optJSONObject("headers")?.optString("Host").orEmpty()
            }.takeIf { it.isNotBlank() }
            "httpupgrade" -> httpupgrade.optString("host").takeIf { it.isNotBlank() }
            "xhttp" -> parsedXhttp?.host
            "http" -> http.optJSONArray("host")?.optString(0)?.takeIf { it.isNotBlank() }
            "grpc" -> grpc.optString("authority").takeIf { it.isNotBlank() }
            else -> null
        }
        val headerType = when (network) {
            "kcp" -> kcp.optJSONObject("header")?.optString("type")
            "quic" -> quic.optJSONObject("header")?.optString("type")
            "tcp" -> tcp.optJSONObject("header")?.optString("type")
            else -> null
        }?.takeIf { it.isNotBlank() }

        return ParsedStream(
            network = if (xhttp != null) "xhttp" else network,
            security = security,
            sni = reality.optString("serverName").takeIf { it.isNotBlank() }
                ?: tls.optString("serverName").takeIf { it.isNotBlank() },
            fingerprint = reality.optString("fingerprint").takeIf { it.isNotBlank() }
                ?: tls.optString("fingerprint").takeIf { it.isNotBlank() },
            publicKey = reality.optString("publicKey").takeIf { it.isNotBlank() },
            shortId = reality.optString("shortId").takeIf { it.isNotBlank() },
            spiderX = reality.optString("spiderX").takeIf { it.isNotBlank() },
            alpn = readStringList(tls.optJSONArray("alpn")),
            insecureTls = tls.optBoolean("allowInsecure") || tls.optBoolean("insecure"),
            path = path,
            host = host,
            xhttpMode = parsedXhttp?.mode,
            xhttpExtra = parsedXhttp?.extra,
            headerType = headerType,
            kcpSeed = kcp.optString("seed").takeIf { it.isNotBlank() },
            quicSecurity = quic.optString("security").takeIf { it.isNotBlank() },
            quicKey = quic.optString("key").takeIf { it.isNotBlank() },
            grpcMultiMode = grpc.optBoolean("multiMode"),
        )
    }

    fun parseSingBoxTransport(transport: JSONObject?, tls: JSONObject?): ParsedStream {
        val network = normalizeNetwork(transport?.optString("type"))
        val reality = tls?.optJSONObject("reality") ?: JSONObject()
        val utls = tls?.optJSONObject("utls") ?: JSONObject()
        val headers = transport?.optJSONObject("headers") ?: JSONObject()
        val xhttp = XhttpParseHelper.parseFromTransport(transport)
        return ParsedStream(
            network = if (xhttp != null) "xhttp" else network,
            security = when {
                reality.optBoolean("enabled") -> "reality"
                tls?.optBoolean("enabled") == true -> "tls"
                else -> "none"
            },
            sni = tls?.optString("server_name")?.takeIf { it.isNotBlank() },
            fingerprint = utls.optString("fingerprint").takeIf { it.isNotBlank() },
            publicKey = reality.optString("public_key").takeIf { it.isNotBlank() },
            shortId = reality.optString("short_id").takeIf { it.isNotBlank() },
            alpn = readStringList(tls?.optJSONArray("alpn")),
            insecureTls = tls?.optBoolean("insecure") == true,
            path = transport?.optString("path")?.takeIf { it.isNotBlank() }
                ?: transport?.optString("service_name")?.takeIf { it.isNotBlank() }
                ?: xhttp?.path,
            host = transport?.optString("host")?.takeIf { it.isNotBlank() }
                ?: headers.optString("Host").takeIf { it.isNotBlank() }
                ?: xhttp?.host,
            xhttpMode = xhttp?.mode,
            xhttpExtra = xhttp?.extra,
            headerType = transport?.optJSONObject("header")?.optString("type")
                ?.takeIf { it.isNotBlank() },
            kcpSeed = transport?.optString("seed")?.takeIf { it.isNotBlank() },
            grpcMultiMode = transport?.optBoolean("multi_mode") == true,
        )
    }

    fun build(node: ProxyNode): JSONObject {
        val network = normalizeNetwork(node.transport)
        return JSONObject().apply {
            put("network", if (network == "xhttp") "xhttp" else network)
            when (network) {
                "xhttp" -> put("xhttpSettings", buildXhttpSettings(node))
                "ws" -> put("wsSettings", buildWsSettings(node))
                "grpc" -> put("grpcSettings", buildGrpcSettings(node))
                "httpupgrade" -> put("httpupgradeSettings", buildHttpUpgradeSettings(node))
                "kcp" -> put("kcpSettings", buildKcpSettings(node))
                "quic" -> put("quicSettings", buildQuicSettings(node))
                "http" -> put("httpSettings", buildHttpSettings(node))
                else -> put("tcpSettings", buildTcpSettings(node))
            }
            applyTls(this, node)
        }
    }

    private fun buildXhttpSettings(node: ProxyNode): JSONObject {
        val host = node.resolvedHostHeader ?: node.sni ?: node.host
        val path = node.resolvedPath?.ifBlank { null } ?: XhttpDefaults.DEFAULT_PATH
        val mode = node.xhttpMode?.takeIf { it.isNotBlank() } ?: XhttpDefaults.DEFAULT_MODE
        return JSONObject().apply {
            put("host", host)
            put("path", path)
            put("mode", mode)
            node.xhttpExtra?.takeIf { it.isNotBlank() }?.let { raw ->
                runCatching { JSONObject(raw) }.getOrNull()
                    ?.let { XhttpExtraHelper.migrateSessionKeys(it) }
                    ?.let { extra -> put("extra", extra) }
            }
        }
    }

    private fun buildWsSettings(node: ProxyNode): JSONObject {
        val path = node.resolvedPath ?: "/"
        val host = node.resolvedHostHeader
        return JSONObject().apply {
            put("path", path)
            if (!host.isNullOrBlank()) {
                put("host", host)
                put("headers", JSONObject().put("Host", host))
            }
        }
    }

    private fun buildGrpcSettings(node: ProxyNode): JSONObject {
        return JSONObject().apply {
            put("serviceName", node.resolvedPath.orEmpty())
            put("multiMode", node.grpcMultiMode)
            node.resolvedHostHeader?.let { put("authority", it) }
        }
    }

    private fun buildHttpUpgradeSettings(node: ProxyNode): JSONObject {
        return JSONObject().apply {
            put("path", node.resolvedPath ?: "/")
            node.resolvedHostHeader?.let { put("host", it) }
        }
    }

    private fun buildKcpSettings(node: ProxyNode): JSONObject {
        return JSONObject().apply {
            put("mtu", 1350)
            put("tti", 50)
            put("uplinkCapacity", 12)
            put("downlinkCapacity", 100)
            put("congestion", false)
            put("readBufferSize", 2)
            put("writeBufferSize", 2)
            put("header", JSONObject().put("type", node.headerType?.ifBlank { null } ?: "none"))
            node.kcpSeed?.takeIf { it.isNotBlank() }?.let { put("seed", it) }
        }
    }

    private fun buildQuicSettings(node: ProxyNode): JSONObject {
        return JSONObject().apply {
            put("security", node.quicSecurity?.ifBlank { null } ?: "none")
            put("key", node.quicKey.orEmpty())
            put("header", JSONObject().put("type", node.headerType?.ifBlank { null } ?: "none"))
        }
    }

    private fun buildHttpSettings(node: ProxyNode): JSONObject {
        val host = node.resolvedHostHeader ?: node.sni ?: node.host
        return JSONObject().apply {
            put("host", JSONArray().put(host))
            put("path", node.resolvedPath ?: "/")
        }
    }

    private fun buildTcpSettings(node: ProxyNode): JSONObject {
        val headerType = node.headerType?.lowercase().orEmpty()
        return JSONObject().apply {
            if (headerType == "http") {
                put(
                    "header",
                    JSONObject().apply {
                        put("type", "http")
                        put(
                            "request",
                            JSONObject().apply {
                                put("path", JSONArray().put(node.resolvedPath ?: "/"))
                                val host = node.resolvedHostHeader ?: node.sni ?: node.host
                                put("headers", JSONObject().put("Host", JSONArray().put(host)))
                            },
                        )
                    },
                )
            } else {
                put("header", JSONObject().put("type", "none"))
            }
        }
    }

    fun applyTls(stream: JSONObject, node: ProxyNode) {
        when (node.security.lowercase()) {
            "reality" -> {
                stream.put("security", "reality")
                stream.put(
                    "realitySettings",
                    JSONObject().apply {
                        put("serverName", node.sni ?: node.host)
                        put("publicKey", node.publicKey.orEmpty())
                        put("shortId", node.shortId.orEmpty())
                        put("fingerprint", node.fingerprint?.takeIf { it.isNotBlank() } ?: "chrome")
                        node.spiderX?.takeIf { it.isNotBlank() }?.let { put("spiderX", it) }
                    },
                )
            }
            "tls" -> {
                stream.put("security", "tls")
                stream.put(
                    "tlsSettings",
                    JSONObject().apply {
                        put("serverName", node.sni ?: node.host)
                        node.fingerprint?.takeIf { it.isNotBlank() }?.let { put("fingerprint", it) }
                        node.alpn?.takeIf { it.isNotEmpty() }?.let { values ->
                            put("alpn", JSONArray().apply { values.forEach { put(it) } })
                        }
                        if (node.insecureTls) put("allowInsecure", true)
                    },
                )
            }
            else -> stream.put("security", "none")
        }
    }

    private fun readStringList(array: JSONArray?): List<String>? {
        if (array == null || array.length() == 0) return null
        return buildList {
            for (i in 0 until array.length()) {
                array.optString(i).takeIf { it.isNotBlank() }?.let(::add)
            }
        }.takeIf { it.isNotEmpty() }
    }
}

internal fun ProxyNode.withStream(stream: ParsedStream): ProxyNode = copy(
    security = stream.security.ifBlank { security },
    sni = stream.sni ?: sni,
    fingerprint = stream.fingerprint ?: fingerprint,
    publicKey = stream.publicKey ?: publicKey,
    shortId = stream.shortId ?: shortId,
    spiderX = stream.spiderX ?: spiderX,
    alpn = stream.alpn ?: alpn,
    insecureTls = insecureTls || stream.insecureTls,
    transport = stream.network,
    xhttpHost = stream.host ?: xhttpHost,
    xhttpPath = if (stream.network == "xhttp") stream.path ?: xhttpPath else xhttpPath,
    xhttpMode = stream.xhttpMode ?: xhttpMode,
    xhttpExtra = stream.xhttpExtra ?: xhttpExtra,
    path = stream.path ?: path,
    headerType = stream.headerType ?: headerType,
    kcpSeed = stream.kcpSeed ?: kcpSeed,
    quicSecurity = stream.quicSecurity ?: quicSecurity,
    quicKey = stream.quicKey ?: quicKey,
    grpcMultiMode = grpcMultiMode || stream.grpcMultiMode,
)
