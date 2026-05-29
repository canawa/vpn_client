package online.coffemaniavpn.client.data

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
) {
    val isHysteria2: Boolean
        get() = protocol.equals("hysteria2", ignoreCase = true)
}
