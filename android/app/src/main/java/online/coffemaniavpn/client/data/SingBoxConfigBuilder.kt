package online.coffemaniavpn.client.data

import org.json.JSONArray
import org.json.JSONObject

object SingBoxConfigBuilder {
    fun build(node: ProxyNode): String {
        val outbound = when {
            node.isHysteria2 -> buildHysteria2Outbound(node)
            else -> buildVlessOutbound(node)
        }

        return buildBaseConfig(outbound, node).toString(2)
    }

    private fun buildVlessOutbound(node: ProxyNode): JSONObject {
        return JSONObject().apply {
            put("type", "vless")
            put("tag", "proxy")
            put("server", node.host)
            put("server_port", node.port)
            put("uuid", node.uuid)
            put("domain_resolver", "dns-local")
            if (!node.flow.isNullOrBlank()) {
                put("flow", node.flow)
            }

            when (node.security.lowercase()) {
                "reality" -> {
                    put("tls", JSONObject().apply {
                        put("enabled", true)
                        put("server_name", node.sni ?: node.host)
                        put("reality", JSONObject().apply {
                            put("enabled", true)
                            put("public_key", node.publicKey.orEmpty())
                            put("short_id", node.shortId.orEmpty())
                        })
                        if (!node.fingerprint.isNullOrBlank()) {
                            put("utls", JSONObject().apply {
                                put("enabled", true)
                                put("fingerprint", node.fingerprint)
                            })
                        }
                    })
                }
                "tls" -> {
                    put("tls", JSONObject().apply {
                        put("enabled", true)
                        put("server_name", node.sni ?: node.host)
                    })
                }
            }
        }
    }

    private fun buildHysteria2Outbound(node: ProxyNode): JSONObject {
        val password = node.password ?: node.uuid
        return JSONObject().apply {
            put("type", "hysteria2")
            put("tag", "proxy")
            put("server", node.host)
            put("server_port", node.port)
            put("password", password)
            put("connect_timeout", "10s")
            put("domain_resolver", "dns-local")

            if (node.upMbps != null && node.downMbps != null) {
                put("up_mbps", node.upMbps)
                put("down_mbps", node.downMbps)
            }

            if (!node.obfsType.isNullOrBlank() && !node.obfsPassword.isNullOrBlank()) {
                put("obfs", JSONObject().apply {
                    put("type", node.obfsType)
                    put("password", node.obfsPassword)
                })
            }

            put("tls", JSONObject().apply {
                put("enabled", true)
                put("server_name", node.sni ?: node.host)
                if (node.insecureTls) {
                    put("insecure", true)
                }
                // uTLS не поддерживается для Hysteria2/QUIC
                node.alpn?.takeIf { it.isNotEmpty() }?.let { values ->
                    put("alpn", JSONArray().apply { values.forEach(::put) })
                }
            })
        }
    }

    private fun buildBaseConfig(outbound: JSONObject, node: ProxyNode): JSONObject {
        val parentDomain = node.host.substringAfter('.', missingDelimiterValue = "")
            .takeIf { it.contains('.') }

        return JSONObject().apply {
            put("log", JSONObject().put("level", "info"))
            put("dns", JSONObject().apply {
                put("servers", JSONArray().apply {
                    put(JSONObject().apply {
                        put("tag", "dns-local")
                        put("type", "local")
                    })
                    put(JSONObject().apply {
                        put("tag", "dns-remote")
                        put("type", "udp")
                        put("server", "8.8.8.8")
                        put("detour", "proxy")
                    })
                })
                put("rules", JSONArray().apply {
                    put(JSONObject().apply {
                        put("domain", JSONArray().apply {
                            put(node.host)
                            parentDomain?.let { put(".$it") }
                        })
                        put("server", "dns-local")
                    })
                })
                put("final", "dns-local")
                put("strategy", "prefer_ipv4")
            })
            put("inbounds", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "tun")
                    put("tag", "tun-in")
                    put("address", JSONArray().apply { put("172.19.0.1/30") })
                    put("auto_route", true)
                    put("strict_route", true)
                    put("stack", "mixed")
                })
            })
            put("outbounds", JSONArray().apply {
                put(outbound)
                put(JSONObject().apply {
                    put("type", "direct")
                    put("tag", "direct")
                })
                put(JSONObject().apply {
                    put("type", "block")
                    put("tag", "block")
                })
            })
            put("route", JSONObject().apply {
                put("rules", JSONArray().apply {
                    put(JSONObject().apply {
                        put("ip_is_private", true)
                        put("outbound", "direct")
                    })
                    put(JSONObject().apply {
                        put("action", "sniff")
                    })
                    put(JSONObject().apply {
                        put("protocol", "dns")
                        put("action", "hijack-dns")
                    })
                })
                put("final", "proxy")
                put("auto_detect_interface", true)
                put("default_domain_resolver", "dns-local")
            })
        }
    }
}
