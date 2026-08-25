package work.bavshield.vpn.data

import org.json.JSONArray
import org.json.JSONObject

object XrayConfigBuilder {
    private const val TUN_ADDRESS = "172.19.0.1"
    private const val TUN_PREFIX = 30
    private const val TUN_DNS = "8.8.8.8"

    fun build(node: ProxyNode): String {
        val config = buildBaseConfig(node)
        XrayRoutingApplier.applyRoutingProfile(config)
        XrayRoutingApplier.applyConnectionSettings(config)
        return config.toString(2)
    }

    private fun buildBaseConfig(node: ProxyNode): JSONObject {
        val proxyOutbound = when (node.protocol.lowercase()) {
            "hysteria2", "hysteria" -> buildHysteria2Outbound(node)
            "vmess" -> buildVmessOutbound(node)
            "trojan" -> buildTrojanOutbound(node)
            "shadowsocks", "ss" -> buildShadowsocksOutbound(node)
            "socks", "socks5" -> buildSocksOutbound(node)
            "http" -> buildHttpOutbound(node)
            else -> buildVlessOutbound(node)
        }

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
            })
            put("routing", buildBaseRouting())
        }
    }

    private fun buildDns(node: ProxyNode): JSONObject {
        val parentDomain = node.host.substringAfter('.', missingDelimiterValue = "")
            .takeIf { it.contains('.') }

        return JSONObject().apply {
            put("servers", JSONArray().apply {
                // Российские домены — через DNS напрямую (не через VPN).
                put(JSONObject().apply {
                    put("address", "77.88.8.8")
                    put("domains", JSONArray().apply {
                        put("geosite:CATEGORY-RU")
                        put("geosite:TLD-RU")
                        put("geosite:CATEGORY-GOV-RU")
                    })
                    put("skipFallback", true)
                })
                put(TUN_DNS)
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
                put(fieldRule(
                    ip = JSONArray().apply {
                        put("geoip:PRIVATE")
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
                // Российские сайты и IP — мимо VPN (geo-файлы из libv2ray).
                put(fieldRule(
                    domain = JSONArray().apply {
                        put("geosite:CATEGORY-RU")
                        put("geosite:TLD-RU")
                        put("geosite:CATEGORY-GOV-RU")
                    },
                    outboundTag = "direct",
                ))
                put(fieldRule(
                    ip = JSONArray().put("geoip:RU"),
                    outboundTag = "direct",
                ))
            })
            put("balancers", JSONArray())
        }
    }

    private fun buildVlessOutbound(node: ProxyNode): JSONObject {
        val user = JSONObject().apply {
            put("id", node.uuid)
            put("encryption", node.encryption.ifBlank { "none" })
            if (!node.isXhttp && !node.flow.isNullOrBlank()) {
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
            put("streamSettings", StreamSettingsCodec.build(node))
        }
    }

    private fun buildVmessOutbound(node: ProxyNode): JSONObject {
        val user = JSONObject().apply {
            put("id", node.uuid)
            put("alterId", node.alterId)
            put("security", node.encryption.ifBlank { "auto" })
        }
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vmess")
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", node.host)
                        put("port", node.port)
                        put("users", JSONArray().apply { put(user) })
                    })
                })
            })
            put("streamSettings", StreamSettingsCodec.build(node))
        }
    }

    private fun buildTrojanOutbound(node: ProxyNode): JSONObject {
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "trojan")
            put("settings", JSONObject().apply {
                put("servers", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", node.host)
                        put("port", node.port)
                        put("password", node.password ?: node.uuid)
                    })
                })
            })
            put("streamSettings", StreamSettingsCodec.build(node))
        }
    }

    private fun buildShadowsocksOutbound(node: ProxyNode): JSONObject {
        val server = JSONObject().apply {
            put("address", node.host)
            put("port", node.port)
            put("method", node.encryption.ifBlank { "aes-256-gcm" })
            put("password", node.password ?: node.uuid)
        }
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "shadowsocks")
            put("settings", JSONObject().apply {
                put("servers", JSONArray().apply { put(server) })
            })
            val stream = StreamSettingsCodec.build(node)
            if (node.transport != "tcp" || node.security != "none") {
                put("streamSettings", stream)
            }
        }
    }

    private fun buildSocksOutbound(node: ProxyNode): JSONObject =
        buildUsernamePasswordOutbound(node, protocol = "socks")

    private fun buildHttpOutbound(node: ProxyNode): JSONObject =
        buildUsernamePasswordOutbound(node, protocol = "http")

    private fun buildUsernamePasswordOutbound(node: ProxyNode, protocol: String): JSONObject {
        val server = JSONObject().apply {
            put("address", node.host)
            put("port", node.port)
            val user = node.username?.takeIf { it.isNotBlank() }
            val pass = node.password?.takeIf { it.isNotBlank() }
            if (user != null || pass != null) {
                put(
                    "users",
                    JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("user", user.orEmpty())
                                put("pass", pass.orEmpty())
                            },
                        )
                    },
                )
            }
        }
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", protocol)
            put("settings", JSONObject().apply {
                put("servers", JSONArray().apply { put(server) })
            })
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

    private fun freedomOutbound(tag: String): JSONObject {
        return JSONObject().apply {
            put("tag", tag)
            put("protocol", "freedom")
            put("settings", JSONObject())
        }
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
