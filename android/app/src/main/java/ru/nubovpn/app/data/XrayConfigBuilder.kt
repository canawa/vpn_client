package ru.nubovpn.app.data

import org.json.JSONArray
import org.json.JSONObject

object XrayConfigBuilder {
    private const val TUN_ADDRESS = "172.19.0.1"
    private const val TUN_PREFIX = 30
    private const val TUN_DNS = "8.8.8.8"
    private val IPv4_REGEX = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

    fun build(node: ProxyNode): String {
        val config = buildBaseConfig(node)
        XrayRoutingApplier.applyRoutingProfile(config)
        XrayRoutingApplier.applyConnectionSettings(config)
        XrayRoutingApplier.ensureDefaultProxyRule(config)
        ensureProxyServerDirectRoute(config, node)
        return config.toString(2)
    }

    /** Соединение с VPN-сервером иначе уходит в proxy и зависает (read timeout). */
    private fun ensureProxyServerDirectRoute(config: JSONObject, node: ProxyNode) {
        val routing = config.optJSONObject("routing") ?: return
        val rules = routing.optJSONArray("rules") ?: return

        val domains = JSONArray()
        val ips = JSONArray()
        proxyEndpointHosts(node).forEach { host ->
            if (IPv4_REGEX.matches(host)) {
                ips.put(host)
            } else {
                domains.put("full:$host")
                val parent = host.substringAfter('.', "")
                if (parent.contains('.')) domains.put("domain:.$parent")
            }
        }
        if (domains.length() == 0 && ips.length() == 0) return

        val bypass = JSONObject().apply {
            put("type", "field")
            put("outboundTag", "direct")
            domains.takeIf { it.length() > 0 }?.let { put("domain", it) }
            ips.takeIf { it.length() > 0 }?.let { put("ip", it) }
            put("port", node.port.toString())
        }

        val merged = JSONArray()
        val privateRuleCount = 1.coerceAtMost(rules.length())
        for (i in 0 until privateRuleCount) merged.put(rules.get(i))
        merged.put(bypass)
        for (i in privateRuleCount until rules.length()) merged.put(rules.get(i))
        routing.put("rules", merged)
    }

    private fun proxyEndpointHosts(node: ProxyNode): Set<String> = buildSet {
        listOfNotNull(node.host, node.sni, node.wsHost, node.xhttpHost, node.grpcAuthority)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { add(it) }
    }

    private fun buildBaseConfig(node: ProxyNode): JSONObject {
        val proxyOutbound = resolveProxyOutbound(node)

        return JSONObject().apply {
            put("log", JSONObject().put("loglevel", "warning"))
            put("dns", buildDns(node))
            put("inbounds", JSONArray().apply {
                put(buildTunInbound())
            })
            put("outbounds", JSONArray().apply {
                put(proxyOutbound)
                put(freedomOutbound("direct"))
                put(blackholeOutbound("block"))
                put(dnsOutbound("dns-out"))
            })
            put("routing", buildBaseRouting())
        }
    }

    private fun buildDns(node: ProxyNode): JSONObject {
        val isIpHost = IPv4_REGEX.matches(node.host)
        val parentDomain = if (!isIpHost) {
            node.host.substringAfter('.', missingDelimiterValue = "")
                .takeIf { it.contains('.') }
        } else {
            null
        }

        return JSONObject().apply {
            put("queryStrategy", "UseIPv4")
            put("servers", JSONArray().apply {
                put(TUN_DNS)
                // Fallback: DNS-over-TCP — работает и там, где сервер не пропускает UDP
                put("tcp://$TUN_DNS")
                put(JSONObject().apply {
                    put("address", TUN_DNS)
                    put("domains", JSONArray().apply {
                        put("full:${node.host}")
                        parentDomain?.let { put("domain:.$it") }
                    })
                    put("skipFallback", true)
                })
            })
        }
    }

    private fun buildTunInbound(): JSONObject {
        return JSONObject().apply {
            put("tag", "tun-in")
            put("port", 0)
            put("protocol", "tun")
            put("settings", JSONObject().apply {
                put("name", "xray0")
                put("mtu", 1500)
                put("gateway", JSONArray().put("$TUN_ADDRESS/$TUN_PREFIX"))
                put("dns", JSONArray().put(TUN_DNS))
            })
            put("sniffing", JSONObject().apply {
                put("enabled", true)
                put("routeOnly", true)
                put("destOverride", JSONArray().apply {
                    put("http")
                    put("tls")
                    put("quic")
                })
            })
        }
    }

    private fun buildBaseRouting(): JSONObject {
        return JSONObject().apply {
            put("domainStrategy", "IPIfNonMatch")
            put("rules", JSONArray().apply {
                // Перехват DNS: запросы приложений (порт 53) обрабатывает DNS-модуль xray.
                // Без этого DNS уходит сырым UDP через прокси и часто теряется —
                // туннель «подключён», а сайты не открываются.
                put(JSONObject().apply {
                    put("type", "field")
                    put("inboundTag", JSONArray().put("tun-in"))
                    put("port", "53")
                    put("network", "tcp,udp")
                    put("outboundTag", "dns-out")
                })
                put(fieldRule(
                    ip = JSONArray().apply {
                        put("0.0.0.0/8")
                        put("10.0.0.0/8")
                        put("127.0.0.0/8")
                        put("169.254.0.0/16")
                        put("172.16.0.0/12")
                        put("192.168.0.0/16")
                        put("224.0.0.0/4")
                        put("240.0.0.0/4")
                    },
                    outboundTag = "direct",
                ))
            })
            put("balancers", JSONArray())
        }
    }

    private fun resolveProxyOutbound(node: ProxyNode): JSONObject {
        node.rawOutboundJson?.let { raw ->
            runCatching { JSONObject(raw) }.getOrNull()?.takeIf { it.has("protocol") }?.let { outbound ->
                outbound.put("tag", "proxy")
                return outbound
            }
        }
        return when {
            node.isHysteria2 -> buildHysteria2Outbound(node)
            else -> buildVlessOutbound(node)
        }
    }

    private fun buildVlessOutbound(node: ProxyNode): JSONObject {
        val user = JSONObject().apply {
            put("id", node.uuid)
            put("encryption", node.encryption.ifBlank { "none" })
            if (shouldUseFlow(node)) {
                put("flow", node.flow)
            }
        }

        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vless")
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", node.host)
                        put("port", node.port)
                        put("users", JSONArray().apply { put(user) })
                    })
                })
            })
            put("streamSettings", buildVlessStreamSettings(node))
        }
    }

    private fun buildVlessStreamSettings(node: ProxyNode): JSONObject {
        return JSONObject().apply {
            when {
                node.isXhttp && node.transport.equals("splithttp", ignoreCase = true) -> {
                    put("network", "splithttp")
                    put("splithttpSettings", buildXhttpSettings(node))
                }
                node.isXhttp -> {
                    put("network", "xhttp")
                    put("xhttpSettings", buildXhttpSettings(node))
                }
                node.isGrpc -> {
                    put("network", "grpc")
                    put("grpcSettings", JSONObject().apply {
                        put("serviceName", node.grpcServiceName.orEmpty())
                        node.grpcAuthority?.let { put("authority", it) }
                        node.grpcMultiMode?.let { put("multiMode", it) }
                    })
                }
                node.transport.equals("ws", ignoreCase = true) -> {
                    put("network", "ws")
                    put("wsSettings", JSONObject().apply {
                        put("path", node.wsPath ?: "/")
                        node.wsHost?.let { host ->
                            put("host", host)
                            put("headers", JSONObject().put("Host", host))
                        }
                    })
                }
                node.transport.equals("httpupgrade", ignoreCase = true) -> {
                    put("network", "httpupgrade")
                    put("httpupgradeSettings", JSONObject().apply {
                        put("path", node.wsPath ?: "/")
                        node.wsHost?.let { host ->
                            put("host", host)
                            put("headers", JSONObject().put("Host", host))
                        }
                    })
                }
                else -> {
                    put("network", "tcp")
                    put("tcpSettings", JSONObject().apply {
                        put("header", JSONObject().put("type", "none"))
                    })
                }
            }
            applyTls(this, node)
        }
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
                stream.put("realitySettings", JSONObject().apply {
                    put("serverName", node.sni ?: node.host)
                    put("publicKey", node.publicKey.orEmpty())
                    put("shortId", node.shortId.orEmpty())
                    put("fingerprint", node.fingerprint?.takeIf { it.isNotBlank() } ?: "chrome")
                    node.spiderX?.takeIf { it.isNotBlank() }?.let { put("spiderX", it) }
                })
            }
            "tls" -> {
                stream.put("security", "tls")
                stream.put("tlsSettings", JSONObject().apply {
                    put("serverName", node.sni ?: node.host)
                    node.fingerprint?.takeIf { it.isNotBlank() }?.let { put("fingerprint", it) }
                    if (node.insecureTls) put("allowInsecure", true)
                })
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
            put("tlsSettings", JSONObject().apply {
                put("serverName", node.sni ?: node.host)
                put("alpn", JSONArray().put("h3"))
                if (node.insecureTls) put("allowInsecure", true)
                node.fingerprint?.takeIf { it.isNotBlank() }?.let { put("fingerprint", it) }
            })
            put("hysteriaSettings", hysteriaSettings)

            if (!node.obfsType.isNullOrBlank() && !node.obfsPassword.isNullOrBlank()) {
                put("finalmask", JSONObject().apply {
                    put("udp", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", node.obfsType)
                            put("settings", JSONObject().put("password", node.obfsPassword))
                        })
                    })
                })
            }
        }

        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "hysteria")
            put("settings", JSONObject().apply {
                put("version", 2)
                put("address", node.host)
                put("port", node.port)
            })
            put("streamSettings", stream)
        }
    }

    private fun dnsOutbound(tag: String): JSONObject {
        return JSONObject().apply {
            put("tag", tag)
            put("protocol", "dns")
            put("settings", JSONObject().apply {
                put("nonIPQuery", "skip")
            })
        }
    }

    private fun freedomOutbound(tag: String): JSONObject {
        return JSONObject().apply {
            put("tag", tag)
            put("protocol", "freedom")
            put("settings", JSONObject().put("domainStrategy", "UseIPv4"))
        }
    }

    private fun shouldUseFlow(node: ProxyNode): Boolean {
        if (node.flow.isNullOrBlank()) return false
        val transport = node.transport.lowercase()
        return transport !in setOf("xhttp", "splithttp", "grpc", "ws", "httpupgrade")
    }

    private fun blackholeOutbound(tag: String): JSONObject {
        return JSONObject().apply {
            put("tag", tag)
            put("protocol", "blackhole")
            put("settings", JSONObject().apply {
                put("response", JSONObject().put("type", "http"))
            })
        }
    }

    internal fun fieldRule(
        domain: JSONArray? = null,
        ip: JSONArray? = null,
        outboundTag: String,
    ): JSONObject {
        return JSONObject().apply {
            put("type", "field")
            domain?.let { put("domain", it) }
            ip?.let { put("ip", it) }
            put("outboundTag", outboundTag)
        }
    }
}
