package ru.coffeemaniavpn.app.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.coffeemaniavpn.app.util.AppLog
import java.util.concurrent.TimeUnit

class SubscriptionRepository(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    fun fetchSubscription(url: String): SubscriptionFetchResult {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            error("Вставьте верную ссылку подписки")
        }

        val requestBuilder = runCatching {
            Request.Builder().url(trimmed)
        }.getOrElse {
            error("Вставьте верную ссылку подписки")
        }
        DeviceIdentity.subscriptionHeaders(context).forEach { (name, value) ->
            requestBuilder.header(name, value)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val body = response.body?.string()?.trim().orEmpty()
            validateHwidResponse(response.code, response.headers, body)

            if (!response.isSuccessful) {
                error(formatHttpError(response.code, body))
            }
            if (body.isBlank()) error("Вставьте верную ссылку подписки")

            val nodes = runCatching {
                SubscriptionParser.parse(body)
            }.getOrElse { parseError ->
                if (looksLikePlainTextError(body)) {
                    error(body.lineSequence().first { it.isNotBlank() }.trim())
                }
                error("Вставьте верную ссылку подписки")
            }

            if (nodes.isEmpty()) error("Вставьте верную ссылку подписки")

            val announceCandidates = buildList {
                response.header("announce")?.let(::add)
                response.header("Announce")?.let(::add)
                for (name in response.headers.names()) {
                    if (name.equals("announce", ignoreCase = true)) {
                        response.headers.values(name).forEach(::add)
                    } else {
                        // Иногда панель кладёт base64-announce в другой header.
                        response.headers.values(name).forEach { value ->
                            if (value.startsWith("base64:", ignoreCase = true)) add(value)
                        }
                    }
                }
            }.distinct()

            val info = SubscriptionInfoParser.parseFromResponse(
                userInfoHeader = response.header("subscription-userinfo"),
                profileTitleHeader = response.header("profile-title"),
                announceHeader = announceCandidates.firstOrNull(),
                announceHeaders = announceCandidates,
                body = body,
            )
            AppLog.i(
                "fetchSubscription ok nodes=${nodes.size} " +
                    "announceCandidates=${announceCandidates.size} " +
                    "deviceLimit=${info?.deviceLimit} " +
                    "announceLen=${info?.announce?.length ?: 0} " +
                    "hwid=${DeviceIdentity.hwid(context).take(8)}…",
            )
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
        return "Вставьте верную ссылку подписки"
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
