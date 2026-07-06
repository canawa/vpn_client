package ru.nubovpn.app.data

import android.util.Base64
import org.json.JSONObject

internal object XhttpExtraHelper {
    fun fromSettings(settings: JSONObject?): String? {
        if (settings == null) return null
        settings.optJSONObject("extra")?.takeIf { it.length() > 0 }?.let {
            return migrateSessionKeys(JSONObject(it.toString())).toString()
        }
        val extra = JSONObject()
        settings.keys().forEach { key ->
            when (key) {
                "host", "path", "mode", "extra" -> Unit
                else -> extra.put(key, settings.get(key))
            }
        }
        return extra.takeIf { it.length() > 0 }?.let { migrateSessionKeys(it).toString() }
    }

    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val json = when {
            trimmed.startsWith("{") -> runCatching { JSONObject(trimmed) }.getOrNull()
            else -> decodeBase64Json(trimmed)?.let { runCatching { JSONObject(it) }.getOrNull() }
        }
        return json?.let { migrateSessionKeys(it).toString() }
            ?: trimmed.takeIf { it.startsWith("{") }
            ?: decodeBase64Json(trimmed)
            ?: trimmed
    }

    /** Xray-core v26.6.22+ renamed session* → sessionID* without fallback for legacy keys. */
    fun migrateSessionKeys(extra: JSONObject): JSONObject {
        renameKey(extra, "sessionPlacement", "sessionIDPlacement")
        renameKey(extra, "sessionKey", "sessionIDKey")
        return extra
    }

    private fun renameKey(obj: JSONObject, from: String, to: String) {
        if (!obj.has(from) || obj.has(to)) return
        obj.put(to, obj.remove(from))
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
