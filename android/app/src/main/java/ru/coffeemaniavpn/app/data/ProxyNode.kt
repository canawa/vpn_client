package ru.coffeemaniavpn.app.data

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
    val grpcServiceName: String? = null,
    val wsPath: String? = null,
    val wsHost: String? = null,
    val xhttpHost: String? = null,
    val xhttpPath: String? = null,
    val xhttpMode: String? = null,
    val xhttpExtra: String? = null,
    /** Исходный Xray-outbound из подписки — используется при сборке конфига без потери параметров. */
    val rawOutboundJson: String? = null,
) {
    val isHysteria2: Boolean
        get() = protocol.equals("hysteria2", ignoreCase = true) ||
            protocol.equals("hysteria", ignoreCase = true)

    val isTrojan: Boolean
        get() = protocol.equals("trojan", ignoreCase = true)

    val isXhttp: Boolean
        get() = transport.equals("xhttp", ignoreCase = true) ||
            transport.equals("splithttp", ignoreCase = true)

    val isGrpc: Boolean
        get() = transport.equals("grpc", ignoreCase = true) ||
            transport.equals("gun", ignoreCase = true)

    val isWebSocket: Boolean
        get() = transport.equals("ws", ignoreCase = true) ||
            transport.equals("websocket", ignoreCase = true)
}
