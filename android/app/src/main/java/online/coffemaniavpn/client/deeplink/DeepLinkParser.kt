package online.coffemaniavpn.client.deeplink

import android.net.Uri
import android.util.Base64
import online.coffemaniavpn.client.BuildConfig
import online.coffemaniavpn.client.util.AppLog
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object DeepLinkParser {
    val supportedSchemes = setOf("coffemaniavpn", "cmvpn", "cmv")

    fun parse(uri: Uri): DeepLinkAction? {
        val scheme = uri.scheme?.lowercase() ?: return null
        AppLog.i("DeepLinkParser uri=$uri")

        return when (scheme) {
            in supportedSchemes -> parseCustomScheme(uri)
            "http", "https" -> parseHttpsAppLink(uri)
            else -> null
        }
    }

    private fun parseCustomScheme(uri: Uri): DeepLinkAction? {
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path.orEmpty().trimStart('/')
        val queryUrl = uri.getQueryParameter("url")?.trim().orEmpty()

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

    private fun parseHttpsAppLink(uri: Uri): DeepLinkAction? {
        val host = uri.host?.lowercase().orEmpty()
        if (host != BuildConfig.APP_LINK_HOST.lowercase()) {
            AppLog.w("DeepLinkParser https: unsupported host=$host")
            return null
        }

        val segments = uri.path.orEmpty().trim('/').split('/').filter { it.isNotBlank() }
        if (segments.size < 2 || segments[0] != "app") {
            AppLog.w("DeepLinkParser https: unsupported path=${uri.path}")
            return null
        }

        return when (segments[1].lowercase()) {
            "add" -> parseHttpsAdd(uri)
            "connect" -> DeepLinkAction.Connect
            "disconnect" -> DeepLinkAction.Disconnect
            else -> null
        }
    }

    private fun parseHttpsAdd(uri: Uri): DeepLinkAction? {
        val subscriptionUrl = resolveSubscriptionUrl(uri) ?: run {
            AppLog.w("DeepLinkParser https add: empty subscription url")
            return null
        }
        return DeepLinkAction.Add(
            url = subscriptionUrl,
            connectAfter = uri.connectAfterRequested(default = true),
        )
    }

    private fun resolveSubscriptionUrl(uri: Uri): String? {
        uri.getQueryParameter("url")?.trim()?.takeIf { it.isNotBlank() }?.let { return decodeUri(it) }

        uri.getQueryParameter("token")?.trim()?.takeIf { it.isNotBlank() }?.let { token ->
            val base = BuildConfig.SUBSCRIPTION_BASE_URL.trimEnd('/')
            return "$base/$token"
        }

        val segments = uri.path.orEmpty().trim('/').split('/').filter { it.isNotBlank() }
        if (segments.size >= 3 && segments[0] == "app" && segments[1] == "add") {
            val token = segments[2].trim()
            if (token.isNotBlank()) {
                val base = BuildConfig.SUBSCRIPTION_BASE_URL.trimEnd('/')
                return "$base/$token"
            }
        }

        return null
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

    private fun Uri.connectAfterRequested(default: Boolean = false): Boolean {
        val connect = getQueryParameter("connect")?.trim().orEmpty()
        if (connect.isEmpty()) return default
        if (connect.equals("1", ignoreCase = true) ||
            connect.equals("true", ignoreCase = true) ||
            connect.equals("yes", ignoreCase = true)
        ) {
            return true
        }
        if (connect.equals("0", ignoreCase = true) ||
            connect.equals("false", ignoreCase = true) ||
            connect.equals("no", ignoreCase = true)
        ) {
            return false
        }
        return getBooleanQueryParameter("connect", default)
    }
}
