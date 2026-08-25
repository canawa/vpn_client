package work.bavshield.vpn.data

import kotlinx.serialization.Serializable

@Serializable
data class ProxyNode(
    val id: String,
    val name: String,
    val protocol: String = "vless",
    val host: String,
    val port: Int,
    val uuid: String = "",
    val password: String? = null,
    val encryption: String = "none",
    val flow: String? = null,
    val security: String = "reality",
    val sni: String? = null,
    val fingerprint: String? = null,
    val publicKey: String? = null,
    val shortId: String? = null,
    val spiderX: String? = null,
    val obfsType: String? = null,
    val obfsPassword: String? = null,
    val insecureTls: Boolean = false,
    val upMbps: Int? = null,
    val downMbps: Int? = null,
    val alpn: List<String>? = null,
    val transport: String = "tcp",
    val xhttpHost: String? = null,
    val xhttpPath: String? = null,
    val xhttpMode: String? = null,
    val xhttpExtra: String? = null,
    val path: String? = null,
    val headerType: String? = null,
    val kcpSeed: String? = null,
    val quicSecurity: String? = null,
    val quicKey: String? = null,
    val grpcMultiMode: Boolean = false,
    val alterId: Int = 0,
    val username: String? = null,
) {
    val isHysteria2: Boolean
        get() = protocol.equals("hysteria2", ignoreCase = true) ||
            protocol.equals("hysteria", ignoreCase = true)

    val isXhttp: Boolean
        get() = transport.equals("xhttp", ignoreCase = true) ||
            transport.equals("splithttp", ignoreCase = true)

    val resolvedPath: String?
        get() = path?.takeIf { it.isNotBlank() } ?: xhttpPath?.takeIf { it.isNotBlank() }

    val resolvedHostHeader: String?
        get() = xhttpHost?.takeIf { it.isNotBlank() }

    fun protocolLabel(): String = when (protocol.lowercase()) {
        "hysteria2", "hysteria" -> "HY2"
        "vmess" -> "VMESS"
        "trojan" -> "TROJAN"
        "shadowsocks", "ss" -> "SS"
        "socks", "socks5" -> "SOCKS"
        "http" -> "HTTP"
        else -> "VLESS"
    }
}
