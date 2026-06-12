package online.coffemaniavpn.client.data

import android.util.Base64
import org.json.JSONObject

internal object XhttpExtraHelper {
    fun fromSettings(settings: JSONObject?): String? {
        if (settings == null) return null
        settings.optJSONObject("extra")?.takeIf { it.length() > 0 }?.let { return it.toString() }
        val extra = JSONObject()
        settings.keys().forEach { key ->
            when (key) {
                "host", "path", "mode", "extra" -> Unit
                else -> extra.put(key, settings.get(key))
            }
        }
        return extra.takeIf { it.length() > 0 }?.toString()
    }

    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        if (trimmed.startsWith("{")) return trimmed
        decodeBase64Json(trimmed)?.let { return it }
        return trimmed
    }

    private fun decodeBase64Json(value: String): String? {
        val flags = intArrayOf(
            Base64.URL_SAFE or Base64.NO_WRAP,
            Base64.DEFAULT,
        )
        for (flag in flags) {
            runCatching {
                val decoded = String(Base64.decode(value, flag), Charsets.UTF_8).trim()
                if (decoded.startsWith("{")) return decoded
            }
        }
        return null
    }
}
