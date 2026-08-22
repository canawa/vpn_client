package ru.coffeemaniavpn.app.data

import org.json.JSONArray
import org.json.JSONObject

object XrayConfigBuilder {
    const val MIXED_PORT = 10808

    fun build(node: ProxyNode): String {
        return buildBaseConfig(node).toString(2)
    }

    fun buildProxyOutbound(node: ProxyNode): JSONObject {
        node.rawOutboundJson?.let { raw ->
            return JSONObject(raw).apply { put("tag", "proxy") }
        }
        return when {
            node.isHysteria2 -> buildHysteria2Outbound(node)
            node.isTrojan -> buildTrojanOutbound(node)
            else -> buildVlessOutbound(node)
        }
    }

    private fun buildBaseConfig(node: ProxyNode): JSONObject {
        val proxyOutbound = buildProxyOutbound(node)
        return JSONObject().apply {
            put("log", JSONObject().put("loglevel", "warning"))
            put("stats", JSONObject())
            put(
                "policy",
                JSONObject().put(
                    "system",
                    JSONObject()
                        .put("statsOutboundUplink", true)
                        .put("statsOutboundDownlink", true),
                ),
            )
            put("dns", buildDns(node))
            put("inbounds", JSONArray().put(buildMixedInbound()))
            put(
                "outbounds",
                JSONArray()
                    .put(proxyOutbound)
                    .put(freedomOutbound("direct"))
                    .put(blackholeOutbound("block")),
            )
            put("routing", buildBaseRouting())
        }
    }

    private fun buildDns(node: ProxyNode): JSONObject {
        val parentDomain = node.host.substringAfter('.', missingDelimiterValue = "")
            .takeIf { it.contains('.') }
        return JSONObject().apply {
            put(
                "servers",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("address", "77.88.8.8")
                            put(
                                "domains",
                                JSONArray()
                                    .put("domain:ru")
                                    .put("domain:su")
                                    .put("domain:xn--p1ai"),
                            )
                            put("skipFallback", true)
                        },
                    )
                    put("8.8.8.8")
                    put(
                        JSONObject().apply {
                            put("address", "8.8.8.8")
                            put(
                                "domains",
                                JSONArray().apply {
                                    put("full:${node.host}")
                                    parentDomain?.let { put("domain:$it") }
                                },
                            )
                            put("skipFallback", true)
                        },
                    )
                },
            )
        }
    }

    private fun buildMixedInbound(): JSONObject = JSONObject().apply {
        put("tag", "mixed-in")
        put("listen", "127.0.0.1")
        put("port", MIXED_PORT)
        put("protocol", "mixed")
        put("settings", JSONObject().put("udp", true))
        put(
            "sniffing",
            JSONObject()
                .put("enabled", true)
                .put("destOverride", JSONArray().put("http").put("tls").put("quic")),
        )
    }

    private fun buildBaseRouting(): JSONObject = JSONObject().apply {
        put("domainStrategy", "IPIfNonMatch")
        put(
            "rules",
            JSONArray().apply {
                put(
                    fieldRule(
                        ip = JSONArray()
                            .put("geoip:private")
                            .put("127.0.0.0/8")
                            .put("10.0.0.0/8")
                            .put("169.254.0.0/16")
                            .put("172.16.0.0/12")
                            .put("192.168.0.0/16"),
                        outboundTag = "direct",
                    ),
                )
                put(
                    fieldRule(
                        domain = JSONArray()
                            .put("domain:ru")
                            .put("domain:su")
                            .put("domain:xn--p1ai"),
                        outboundTag = "direct",
                    ),
                )
                put(fieldRule(ip = JSONArray().put("geoip:ru"), outboundTag = "direct"))
            },
        )
    }

    private fun buildTrojanOutbound(node: ProxyNode): JSONObject {
        val password = node.password ?: node.uuid
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "trojan")
            put(
                "settings",
                JSONObject().put(
                    "servers",
                    JSONArray().put(
                        JSONObject()
                            .put("address", node.host)
                            .put("port", node.port)
                            .put("password", password),
                    ),
                ),
            )
            put(
                "streamSettings",
                buildVlessStreamSettings(node).apply {
                    if (!has("security") || optString("security") == "none") {
                        put("security", "tls")
                    }
                },
            )
        }
    }

    private fun buildVlessOutbound(node: ProxyNode): JSONObject {
        val user = JSONObject().apply {
            put("id", node.uuid)
            put("encryption", node.encryption.ifBlank { "none" })
            if (!node.isXhttp && !node.isGrpc && !node.flow.isNullOrBlank()) {
                put("flow", node.flow)
            }
        }
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vless")
            put(
                "settings",
                JSONObject().put(
                    "vnext",
                    JSONArray().put(
                        JSONObject()
                            .put("address", node.host)
                            .put("port", node.port)
                            .put("users", JSONArray().put(user)),
                    ),
                ),
            )
            put("streamSettings", buildVlessStreamSettings(node))
        }
    }

    private fun buildVlessStreamSettings(node: ProxyNode): JSONObject = JSONObject().apply {
        when {
            node.isXhttp -> {
                put("network", "xhttp")
                put("xhttpSettings", buildXhttpSettings(node))
            }
            node.isGrpc -> {
                put("network", "grpc")
                put(
                    "grpcSettings",
                    JSONObject()
                        .put("serviceName", node.grpcServiceName?.takeIf { it.isNotBlank() } ?: "grpc")
                        .put("multiMode", false)
                        .put("idleTimeout", 120),
                )
            }
            node.isWebSocket -> {
                put("network", "ws")
                put(
                    "wsSettings",
                    JSONObject().apply {
                        put("path", node.wsPath?.takeIf { it.isNotBlank() } ?: "/")
                        node.wsHost?.takeIf { it.isNotBlank() }?.let { host ->
                            put("headers", JSONObject().put("Host", host))
                        }
                    },
                )
            }
            else -> {
                put("network", "tcp")
                put("tcpSettings", JSONObject().put("header", JSONObject().put("type", "none")))
            }
        }
        applyTls(this, node)
        applySockopt(this)
    }

    private fun applySockopt(stream: JSONObject) {
        stream.put(
            "sockopt",
            JSONObject()
                .put("tcpKeepAliveIdle", 60)
                .put("tcpKeepAliveInterval", 30),
        )
    }

    private fun buildXhttpSettings(node: ProxyNode): JSONObject {
        val host = node.xhttpHost?.takeIf { it.isNotBlank() }
            ?: node.sni?.takeIf { it.isNotBlank() }
            ?: node.host
        val path = node.xhttpPath?.takeIf { it.isNotBlank() } ?: "/"
        val mode = node.xhttpMode?.takeIf { it.isNotBlank() } ?: "auto"
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

    private fun applyTls(stream: JSONObject, node: ProxyNode) {
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
                        put("fingerprint", node.fingerprint?.takeIf { it.isNotBlank() } ?: "chrome")
                        node.alpn?.takeIf { it.isNotEmpty() }?.let { alpn ->
                            put("alpn", JSONArray().apply { alpn.forEach { put(it) } })
                        }
                        if (node.insecureTls) put("allowInsecure", true)
                    },
                )
            }
            else -> stream.put("security", "none")
        }
    }

    private fun buildHysteria2Outbound(node: ProxyNode): JSONObject {
        val password = node.password ?: node.uuid
        val hysteriaSettings = JSONObject().apply {
            put("version", 2)
            put("auth", password)
            if (node.upMbps != null && node.downMbps != null) {
                put("up", "${node.upMbps}mbps")
                put("down", "${node.downMbps}mbps")
                put("congestion", "brutal")
            }
        }
        val stream = JSONObject().apply {
            put("network", "hysteria")
            put("security", "tls")
            put(
                "tlsSettings",
                JSONObject().apply {
                    put("serverName", node.sni ?: node.host)
                    put("alpn", JSONArray().put("h3"))
                    if (node.insecureTls) put("allowInsecure", true)
                    node.fingerprint?.takeIf { it.isNotBlank() }?.let { put("fingerprint", it) }
                },
            )
            put("hysteriaSettings", hysteriaSettings)
            if (!node.obfsType.isNullOrBlank() && !node.obfsPassword.isNullOrBlank()) {
                put(
                    "finalmask",
                    JSONObject().put(
                        "udp",
                        JSONArray().put(
                            JSONObject()
                                .put("type", node.obfsType)
                                .put("settings", JSONObject().put("password", node.obfsPassword)),
                        ),
                    ),
                )
            }
        }
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "hysteria")
            put(
                "settings",
                JSONObject()
                    .put("version", 2)
                    .put("address", node.host)
                    .put("port", node.port),
            )
            put("streamSettings", stream)
        }
    }

    private fun freedomOutbound(tag: String): JSONObject = JSONObject()
        .put("tag", tag)
        .put("protocol", "freedom")
        .put("settings", JSONObject())

    private fun blackholeOutbound(tag: String): JSONObject = JSONObject()
        .put("tag", tag)
        .put("protocol", "blackhole")
        .put("settings", JSONObject().put("response", JSONObject().put("type", "http")))

    private fun fieldRule(
        domain: JSONArray? = null,
        ip: JSONArray? = null,
        outboundTag: String,
    ): JSONObject = JSONObject().apply {
        put("type", "field")
        domain?.let { put("domain", it) }
        ip?.let { put("ip", it) }
        put("outboundTag", outboundTag)
    }
}
