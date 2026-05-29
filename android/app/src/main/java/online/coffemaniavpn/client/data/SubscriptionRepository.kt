package online.coffemaniavpn.client.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class SubscriptionRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    fun fetchSubscription(url: String): SubscriptionFetchResult {
        val request = Request.Builder()
            .url(url.trim())
            .header("User-Agent", "v2rayNG/1.8.29")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code}")
            }
            val body = response.body?.string()?.trim().orEmpty()
            if (body.isBlank()) error("Пустой ответ подписки")

            val nodes = SubscriptionParser.parse(body).also { parsed ->
                if (parsed.isEmpty()) error("В подписке нет поддерживаемых серверов")
            }

            val info = response.header("subscription-userinfo")
                ?.let(SubscriptionInfoParser::parseHeader)
                ?: SubscriptionInfoParser.parseFromBody(body)

            return SubscriptionFetchResult(nodes = nodes, info = info)
        }
    }

    fun fetchNodes(url: String): List<ProxyNode> = fetchSubscription(url).nodes
}
