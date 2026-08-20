package ru.coffeemaniavpn.app.deeplink

import android.net.Uri

object SubscriptionQrParser {
    fun parseSubscriptionUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }

        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return null
        return when (val action = DeepLinkParser.parse(uri)) {
            is DeepLinkAction.Add -> action.url
            else -> null
        }
    }
}
