package ru.coffeemaniavpn.app.data

import android.net.Uri
import ru.coffeemaniavpn.app.BuildConfig

object SubscriptionUrlValidator {
    private val ourHosts = setOf(
        "xenovpn.top",
        "www.xenovpn.top",
        "cl.xenovpn.top",
        "sub.xenovpn.top",
        "panel.xenovpn.top",
    )

    fun looksLikeSubscriptionUrl(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return true
        }
        return trimmed.startsWith("vless://", ignoreCase = true) ||
            trimmed.startsWith("hy2://", ignoreCase = true) ||
            trimmed.startsWith("hysteria2://", ignoreCase = true)
    }

    fun isOurSubscription(url: String): Boolean {
        val trimmed = url.trim()
        if (!looksLikeSubscriptionUrl(trimmed)) return false
        if (!trimmed.startsWith("http", ignoreCase = true)) return true

        val host = runCatching { Uri.parse(trimmed).host?.lowercase() }.getOrNull()
            ?: return false
        if (host in ourHosts) return true
        return host.endsWith(".xenovpn.top") || host.endsWith(".titi.su")
    }

    fun websiteUrl(utmCampaign: String = "app"): String =
        appendUtm(BuildConfig.SUBSCRIPTION_STORE_URL, utmCampaign)

    fun telegramBotUrl(utmCampaign: String = "app"): String =
        appendUtm(BuildConfig.TELEGRAM_BOT_URL, utmCampaign)

    private fun appendUtm(base: String, campaign: String): String {
        val separator = if ('?' in base) '&' else '?'
        return "${base}${separator}utm_source=app&utm_medium=android&utm_campaign=$campaign"
    }
}
