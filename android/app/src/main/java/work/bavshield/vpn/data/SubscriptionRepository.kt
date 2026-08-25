package work.bavshield.vpn.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import work.bavshield.vpn.util.AppLog
import java.util.concurrent.TimeUnit

class SubscriptionRepository(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    private fun loc() = LocaleHelper.strings(context)

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
            if (body.isBlank()) error(loc().getString(work.bavshield.vpn.R.string.error_subscription_blank))

            val nodes = runCatching {
                SubscriptionParser.parse(body)
            }.getOrElse { parseError ->
                if (looksLikePlainTextError(body)) {
                    error(body.lineSequence().first { it.isNotBlank() }.trim())
                }
                throw parseError
            }

            if (nodes.isEmpty()) error(loc().getString(work.bavshield.vpn.R.string.error_no_supported_servers))

            val info = SubscriptionInfoParser.parseFromResponse(
                userInfoHeader = response.header("subscription-userinfo"),
                profileTitleHeader = response.header("profile-title"),
                body = body,
            )

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
            limitReached -> error(loc().getString(work.bavshield.vpn.R.string.error_device_limit))
            notSupported -> error(loc().getString(work.bavshield.vpn.R.string.error_hwid_missing))
            code == 404 && body.contains("не поддерживается", ignoreCase = true) ->
                error(body.lineSequence().first { it.isNotBlank() }.trim())
            code == 403 && body.contains("device", ignoreCase = true) ->
                error(body.ifBlank { loc().getString(work.bavshield.vpn.R.string.error_access_denied) })
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
        if (ShareLinkParser.looksLikeShareLink(trimmed)) return false
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) return false
        return trimmed.lines().all { line ->
            val value = line.trim()
            value.isEmpty() ||
                value.startsWith("#") ||
                !value.contains("://")
        }
    }
}
