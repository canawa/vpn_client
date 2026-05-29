package online.coffemaniavpn.client.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import online.coffemaniavpn.client.util.AppLog
import java.util.concurrent.TimeUnit

class SubscriptionRepository(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    fun fetchSubscription(url: String): SubscriptionFetchResult {
        val requestBuilder = Request.Builder().url(url.trim())
        DeviceIdentity.subscriptionHeaders(context).forEach { (name, value) ->
            requestBuilder.header(name, value)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val body = response.body?.string()?.trim().orEmpty()
            validateHwidResponse(response.code, response.headers, body)

            if (!response.isSuccessful) {
                error(formatHttpError(response.code, body))
            }
            if (body.isBlank()) error("Пустой ответ подписки")

            val nodes = runCatching {
                SubscriptionParser.parse(body)
            }.getOrElse { parseError ->
                if (looksLikePlainTextError(body)) {
                    error(body.lineSequence().first { it.isNotBlank() }.trim())
                }
                throw parseError
            }

            if (nodes.isEmpty()) error("В подписке нет поддерживаемых серверов")

            val info = response.header("subscription-userinfo")
                ?.let(SubscriptionInfoParser::parseHeader)
                ?: SubscriptionInfoParser.parseFromBody(body)

            AppLog.i("fetchSubscription ok nodes=${nodes.size} hwid=${DeviceIdentity.hwid(context).take(8)}…")
            return SubscriptionFetchResult(nodes = nodes, info = info)
        }
    }

    fun fetchNodes(url: String): List<ProxyNode> = fetchSubscription(url).nodes

    private fun validateHwidResponse(code: Int, headers: okhttp3.Headers, body: String) {
        val notSupported = headers["x-hwid-not-supported"]?.equals("true", ignoreCase = true) == true
        val limitReached = headers["x-hwid-max-devices-reached"]?.equals("true", ignoreCase = true) == true ||
            headers["x-hwid-limit"]?.equals("true", ignoreCase = true) == true

        when {
            limitReached -> error("Достигнут лимит устройств для этой подписки")
            notSupported -> error("Устройство не поддерживается: клиент не отправил HWID")
            code == 404 && body.contains("не поддерживается", ignoreCase = true) ->
                error(body.lineSequence().first { it.isNotBlank() }.trim())
            code == 403 && body.contains("device", ignoreCase = true) ->
                error(body.ifBlank { "Доступ запрещён (лимит устройств)" })
        }
    }

    private fun formatHttpError(code: Int, body: String): String {
        if (looksLikePlainTextError(body)) {
            return body.lineSequence().first { it.isNotBlank() }.trim()
        }
        return "HTTP $code"
    }

    private fun looksLikePlainTextError(body: String): Boolean {
        if (body.isBlank()) return false
        val trimmed = body.trim()
        if (trimmed.startsWith("vless://", ignoreCase = true)) return false
        if (trimmed.startsWith("hy2://", ignoreCase = true)) return false
        if (trimmed.startsWith("hysteria2://", ignoreCase = true)) return false
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) return false
        return trimmed.lines().all { line ->
            val value = line.trim()
            value.isEmpty() ||
                value.startsWith("#") ||
                !value.contains("://")
        }
    }
}
