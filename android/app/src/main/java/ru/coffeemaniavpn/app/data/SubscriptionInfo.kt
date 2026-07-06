package ru.coffeemaniavpn.app.data

import androidx.core.text.HtmlCompat
import kotlinx.serialization.Serializable
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale
import java.util.concurrent.TimeUnit

@Serializable
data class SubscriptionInfo(
    val upload: Long = 0,
    val download: Long = 0,
    val total: Long = 0,
    val expire: Long = 0,
    val title: String = "",
    val subInfoText: String = "",
    val subInfoButtonText: String = "",
    val subInfoButtonLink: String = "",
) {
    val used: Long get() = (upload + download).coerceAtLeast(0)
    val isUnlimitedTraffic: Boolean get() = total <= 0
    val usageFraction: Float
        get() = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    val hasTitle: Boolean get() = title.isNotBlank() && !title.startsWith("base64:", ignoreCase = true)
    val hasSubInfoText: Boolean get() = subInfoText.isNotBlank()
    val hasSubInfoButton: Boolean get() = subInfoButtonText.isNotBlank()

    fun trafficLabel(): String {
        val usedText = formatTrafficBytes(used)
        val totalText = if (isUnlimitedTraffic) "∞" else formatTrafficBytes(total)
        return "$usedText / $totalText"
    }

    fun expireLabel(nowMs: Long = System.currentTimeMillis()): String? {
        if (expire <= 0) return "Бессрочная подписка"
        val nowSec = TimeUnit.MILLISECONDS.toSeconds(nowMs)
        val remainingSec = expire - nowSec
        if (remainingSec <= 0) return "Подписка истекла"

        return SubscriptionExpireFormatter.formatRemaining(remainingSec)
    }

    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (expire <= 0) return false
        return expire * 1_000L <= nowMs
    }
}

data class SubscriptionFetchResult(
    val nodes: List<ProxyNode>,
    val info: SubscriptionInfo?,
)

object SubscriptionInfoParser {
    fun parseFromResponse(
        userInfoHeader: String?,
        profileTitleHeader: String?,
        subInfoTextHeader: String?,
        subInfoButtonTextHeader: String?,
        subInfoButtonLinkHeader: String?,
        body: String,
    ): SubscriptionInfo? {
        val parsedUserInfo = userInfoHeader?.let(::parseUserInfoHeader)
            ?: parseUserInfoFromBody(body)
        val title = parseTitle(profileTitleHeader, body)
        val subInfoText = decodeHeaderText(subInfoTextHeader)
        val subInfoButtonText = decodeHeaderText(subInfoButtonTextHeader)
        val subInfoButtonLink = decodeHeaderLink(subInfoButtonLinkHeader)

        val extras = { info: SubscriptionInfo ->
            info.copy(
                title = title.ifBlank { info.title },
                subInfoText = subInfoText,
                subInfoButtonText = subInfoButtonText,
                subInfoButtonLink = subInfoButtonLink,
            )
        }

        return when {
            parsedUserInfo != null -> extras(parsedUserInfo)
            title.isNotBlank() || subInfoText.isNotBlank() || subInfoButtonText.isNotBlank() ->
                extras(SubscriptionInfo(title = title))
            else -> null
        }
    }

    fun parseHeader(raw: String): SubscriptionInfo? = parseUserInfoHeader(raw)

    private fun parseUserInfoHeader(raw: String): SubscriptionInfo? {
        val values = raw.split(';', ',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .associate { part ->
                val key = part.substringBefore('=').trim().lowercase(Locale.US)
                val value = part.substringAfter('=', "0").trim().toLongOrNull() ?: 0L
                key to value
            }

        if (values.isEmpty()) return null

        return SubscriptionInfo(
            upload = values["upload"] ?: 0L,
            download = values["download"] ?: 0L,
            total = values["total"] ?: 0L,
            expire = values["expire"] ?: 0L,
        )
    }

    fun parseFromBody(body: String): SubscriptionInfo? = parseUserInfoFromBody(body)

    private fun parseUserInfoFromBody(body: String): SubscriptionInfo? {
        return body.lineSequence()
            .map { it.trim().removePrefix("#").trim() }
            .firstOrNull { line ->
                line.contains("upload=", ignoreCase = true) &&
                    line.contains("download=", ignoreCase = true)
            }
            ?.let(::parseUserInfoHeader)
    }

    private fun parseTitle(profileTitleHeader: String?, body: String): String {
        profileTitleHeader?.trim()?.takeIf { it.isNotBlank() }?.let(::decodeText)?.let { return it }
        return parseTitleFromBody(body).orEmpty()
    }

    private fun parseTitleFromBody(body: String): String? {
        return body.lineSequence()
            .map { it.trim().removePrefix("#").trim() }
            .firstNotNullOfOrNull { line ->
                when {
                    line.startsWith("profile-title:", ignoreCase = true) ->
                        line.substringAfter(':').trim()
                    line.startsWith("title:", ignoreCase = true) ->
                        line.substringAfter(':').trim()
                    else -> null
                }
            }
            ?.let(::decodeText)
            ?.takeIf { it.isNotBlank() }
    }

    private fun decodeHeaderText(raw: String?): String {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return ""
        return decodeText(value)
    }

    private fun decodeHeaderLink(raw: String?): String {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return ""
        return if (value.startsWith("base64:", ignoreCase = true)) {
            decodeText(value)
        } else {
            value
        }
    }

    private fun decodeText(raw: String): String {
        val urlDecoded = runCatching {
            URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
        }.getOrDefault(raw).trim()

        val base64Prefix = "base64:"
        if (urlDecoded.startsWith(base64Prefix, ignoreCase = true)) {
            decodeBase64Text(urlDecoded.substring(base64Prefix.length))?.let {
                return normalizeSubscriptionTitle(it)
            }
            return ""
        }

        return normalizeSubscriptionTitle(urlDecoded)
    }

    private fun normalizeSubscriptionTitle(raw: String): String {
        val text = if (raw.contains("&#")) {
            HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
        } else {
            raw.trim()
        }
        if (text.isBlank() || text.contains("&#")) return ""
        return text
    }

    private fun decodeBase64Text(encoded: String): String? {
        val normalized = encoded.trim()
            .replace('-', '+')
            .replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return runCatching {
            String(Base64.getDecoder().decode(padded), StandardCharsets.UTF_8).trim()
        }.getOrNull()?.takeIf { it.isNotBlank() && !it.looksLikeEncodedTitle() }
    }

    private fun String.looksLikeEncodedTitle(): Boolean =
        startsWith("base64:", ignoreCase = true)
}

fun formatTrafficBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}
