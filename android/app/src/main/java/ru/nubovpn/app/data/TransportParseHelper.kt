package ru.nubovpn.app.data

import org.json.JSONObject

internal object TransportParseHelper {
    private val XHTTP_TYPES = setOf("xhttp", "splithttp")

    data class TransportFields(
        val type: String = "tcp",
        val grpcServiceName: String? = null,
        val grpcAuthority: String? = null,
        val grpcMultiMode: Boolean? = null,
        val wsPath: String? = null,
        val wsHost: String? = null,
    )

    fun fromXrayStream(stream: JSONObject): TransportFields {
        val network = stream.optString("network").lowercase()
        val xhttpSettings = stream.optJSONObject("xhttpSettings")
            ?: stream.optJSONObject("splithttpSettings")
        if (network in XHTTP_TYPES || xhttpSettings != null) {
            return TransportFields(type = "xhttp")
        }

        return when (network) {
            "grpc" -> {
                val grpc = stream.optJSONObject("grpcSettings") ?: JSONObject()
                TransportFields(
                    type = "grpc",
                    grpcServiceName = grpc.optString("serviceName").takeIf { it.isNotBlank() },
                    grpcAuthority = grpc.optString("authority").takeIf { it.isNotBlank() },
                    grpcMultiMode = grpc.optBoolean("multiMode").takeIf { grpc.has("multiMode") },
                )
            }
            "ws" -> {
                val ws = stream.optJSONObject("wsSettings") ?: JSONObject()
                val headers = ws.optJSONObject("headers")
                val host = ws.optString("host").takeIf { it.isNotBlank() }
                    ?: headers?.optString("Host")?.takeIf { it.isNotBlank() }
                TransportFields(
                    type = "ws",
                    wsPath = ws.optString("path").takeIf { it.isNotBlank() },
                    wsHost = host,
                )
            }
            "httpupgrade" -> {
                val http = stream.optJSONObject("httpupgradeSettings") ?: JSONObject()
                TransportFields(
                    type = "httpupgrade",
                    wsPath = http.optString("path").takeIf { it.isNotBlank() },
                    wsHost = http.optString("host").takeIf { it.isNotBlank() },
                )
            }
            else -> TransportFields(type = network.ifBlank { "tcp" })
        }
    }

    fun fromSingBoxTransport(transport: JSONObject?): TransportFields {
        if (transport == null) return TransportFields()
        return when (transport.optString("type").lowercase()) {
            in XHTTP_TYPES -> TransportFields(type = "xhttp")
            "grpc" -> TransportFields(
                type = "grpc",
                grpcServiceName = transport.optString("service_name")
                    .ifBlank { transport.optString("serviceName") }
                    .takeIf { it.isNotBlank() },
                grpcAuthority = transport.optString("authority").takeIf { it.isNotBlank() },
            )
            "ws" -> TransportFields(
                type = "ws",
                wsPath = transport.optString("path").takeIf { it.isNotBlank() },
                wsHost = transport.optString("host").takeIf { it.isNotBlank() },
            )
            else -> TransportFields(type = transport.optString("type").ifBlank { "tcp" })
        }
    }

    fun fromVlessParams(params: Map<String, String>): TransportFields {
        val type = params["type"]?.lowercase()
            ?: params["network"]?.lowercase()
            ?: "tcp"
        return when (type) {
            in XHTTP_TYPES -> TransportFields(type = type)
            "grpc" -> TransportFields(
                type = "grpc",
                grpcServiceName = params["serviceName"]?.takeIf { it.isNotBlank() },
                grpcAuthority = params["authority"]?.takeIf { it.isNotBlank() },
                grpcMultiMode = params["mode"]?.equals("multi", ignoreCase = true),
            )
            "ws" -> TransportFields(
                type = "ws",
                wsPath = params["path"]?.takeIf { it.isNotBlank() },
                wsHost = params["host"]?.takeIf { it.isNotBlank() },
            )
            else -> TransportFields(type = type)
        }
    }
}
