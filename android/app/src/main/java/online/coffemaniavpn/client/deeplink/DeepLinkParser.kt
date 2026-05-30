package online.coffemaniavpn.client.deeplink

import android.net.Uri
import android.util.Base64
import online.coffemaniavpn.client.util.AppLog
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object DeepLinkParser {
    val supportedSchemes = setOf("coffemaniavpn", "cmvpn", "cmv")

    fun parse(uri: Uri): DeepLinkAction? {
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme !in supportedSchemes) return null

        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path.orEmpty().trimStart('/')
        val queryUrl = uri.getQueryParameter("url")?.trim().orEmpty()

        AppLog.i("DeepLinkParser uri=$uri host=$host pathLen=${path.length}")

        return when (host) {
            "open" -> DeepLinkAction.Open
            "connect" -> DeepLinkAction.Connect
            "disconnect" -> DeepLinkAction.Disconnect
            "close" -> DeepLinkAction.Close
            "add" -> {
                val url = queryUrl.ifBlank { decodePathPayload(path) }
                if (url.isBlank()) {
                    AppLog.w("DeepLinkParser add: empty url")
                    null
                } else {
                    DeepLinkAction.Add(url, connectAfter = uri.connectAfterRequested())
                }
            }
            "import" -> {
                val encoded = queryUrl.ifBlank { path }
                decodeImportPayload(encoded)?.let { DeepLinkAction.Import(it) }
            }
            "routing" -> parseRouting(path)
            "" -> parseLegacyPath(path)
            else -> parseLegacyPath(listOf(host, path).filter { it.isNotBlank() }.joinToString("/"))
        }
    }

    private fun parseLegacyPath(path: String): DeepLinkAction? = when {
        path.equals("open", ignoreCase = true) -> DeepLinkAction.Open
        path.equals("connect", ignoreCase = true) -> DeepLinkAction.Connect
        path.equals("disconnect", ignoreCase = true) -> DeepLinkAction.Disconnect
        path.equals("close", ignoreCase = true) -> DeepLinkAction.Close
        path.startsWith("add/", ignoreCase = true) -> {
            val url = decodePathPayload(path.removePrefix("add/").removePrefix("add"))
            if (url.isBlank()) null else DeepLinkAction.Add(url, connectAfter = false)
        }
        path.startsWith("import/", ignoreCase = true) -> {
            decodeImportPayload(path.removePrefix("import/").removePrefix("import"))
                ?.let { DeepLinkAction.Import(it) }
        }
        path.startsWith("routing/", ignoreCase = true) -> parseRouting(path.removePrefix("routing/"))
        else -> null
    }

    private fun parseRouting(path: String): DeepLinkAction? {
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.size < 2) {
            AppLog.w("DeepLinkParser routing: invalid path=$path")
            return null
        }
        val action = segments[0].lowercase()
        val encoded = segments.drop(1).joinToString("/")
        val profileJson = decodeImportPayload(encoded) ?: return null
        val enable = action == "onadd"
        if (action != "add" && action != "onadd") {
            AppLog.w("DeepLinkParser routing: unknown action=$action")
            return null
        }
        return DeepLinkAction.Routing(profileJson = profileJson, enable = enable)
    }

    private fun decodePathPayload(raw: String): String {
        if (raw.isBlank()) return ""
        return decodeUri(raw)
    }

    private fun decodeImportPayload(raw: String): String? {
        if (raw.isBlank()) return null

        decodeUri(raw).takeIf { it.isNotBlank() }?.let { decoded ->
            if (looksLikeUrl(decoded) || looksLikeSubscriptionBody(decoded)) {
                return decoded
            }
        }

        return decodeBase64(raw)?.let { decoded ->
            decoded.takeIf { it.isNotBlank() }
        }
    }

    private fun decodeBase64(raw: String): String? {
        val normalized = raw.trim()
            .replace('-', '+')
            .replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return runCatching {
            val bytes = Base64.decode(padded, Base64.DEFAULT)
            String(bytes, StandardCharsets.UTF_8).trim()
        }.getOrNull()
    }

    private fun decodeUri(value: String): String = runCatching {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }.getOrElse { value }

    private fun looksLikeUrl(value: String): Boolean =
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)

    private fun looksLikeSubscriptionBody(value: String): Boolean =
        value.startsWith("vless://", ignoreCase = true) ||
            value.startsWith("hy2://", ignoreCase = true) ||
            value.startsWith("hysteria2://", ignoreCase = true) ||
            value.startsWith("[") ||
            value.startsWith("{")

    private fun Uri.connectAfterRequested(): Boolean {
        val connect = getQueryParameter("connect")?.trim().orEmpty()
        if (connect.equals("1", ignoreCase = true) ||
            connect.equals("true", ignoreCase = true) ||
            connect.equals("yes", ignoreCase = true)
        ) {
            return true
        }
        return getBooleanQueryParameter("connect", false)
    }
}
