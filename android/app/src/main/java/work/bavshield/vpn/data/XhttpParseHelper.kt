package work.bavshield.vpn.data

import org.json.JSONObject

internal object XhttpParseHelper {
    fun transportFromParams(params: Map<String, String>): String =
        StreamSettingsCodec.networkFromUriParams(params)

    fun parseFromSettings(settings: JSONObject?): XhttpFields? {
        if (settings == null) return null
        return XhttpFields(
            host = settings.optString("host").takeIf { it.isNotBlank() },
            path = settings.optString("path").takeIf { it.isNotBlank() },
            mode = settings.optString("mode").takeIf { it.isNotBlank() },
            extra = XhttpExtraHelper.fromSettings(settings),
        )
    }

    fun parseFromTransport(transport: JSONObject?): XhttpFields? {
        if (transport == null) return null
        if (transport.optString("type").lowercase() !in XHTTP_TYPES) return null
        return XhttpFields(
            host = transport.optString("host").takeIf { it.isNotBlank() },
            path = transport.optString("path").takeIf { it.isNotBlank() },
            mode = transport.optString("mode").takeIf { it.isNotBlank() },
            extra = XhttpExtraHelper.fromSettings(transport),
        )
    }

    fun extraFromParams(params: Map<String, String>): String? {
        return XhttpExtraHelper.normalize(params["extra"])
    }

    data class XhttpFields(
        val host: String? = null,
        val path: String? = null,
        val mode: String? = null,
        val extra: String? = null,
    )

    private val XHTTP_TYPES = setOf("xhttp", "splithttp")
}
