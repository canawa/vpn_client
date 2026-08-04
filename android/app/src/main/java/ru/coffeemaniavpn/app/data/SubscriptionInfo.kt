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
    /** Лимит устройств; null — неизвестно, 0 — без лимита. */
    val deviceLimit: Int? = null,
    /** Текст из header Announce (описание подписки). */
    val announce: String = "",
) {
    val used: Long get() = (upload + download).coerceAtLeast(0)
    val isUnlimitedTraffic: Boolean get() = total <= 0
    val usageFraction: Float
        get() = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    val hasTitle: Boolean get() = title.isNotBlank() && !title.startsWith("base64:", ignoreCase = true)
    val hasAnnounce: Boolean get() = announce.isNotBlank()

    /** Лимит из поля или из строки Announce «Лимит устройств … N». */
    fun resolvedDeviceLimit(): Int? =
        deviceLimit ?: AnnounceDeviceLimitParser.extractDeviceLimit(announce)

    fun trafficLabel(): String {
        val usedText = formatTrafficBytes(used)
        val totalText = if (isUnlimitedTraffic) "∞" else formatTrafficBytes(total)
        return "$usedText / $totalText"
    }

    fun devicesLabel(): String = when (val limit = resolvedDeviceLimit()) {
        null -> "Устройств: —"
        in Int.MIN_VALUE..0 -> "Устройств: ∞"
        else -> "Устройств: $limit"
    }

    fun expireLabel(nowMs: Long = System.currentTimeMillis()): String? {
        if (expire <= 0) return "Бессрочная подписка"
        val nowSec = TimeUnit.MILLISECONDS.toSeconds(nowMs)
        val remainingSec = expire - nowSec
        if (remainingSec <= 0) return "Подписка истекла"

        return SubscriptionExpireFormatter.formatRemaining(remainingSec)
    }

    /** Дата окончания для info-bar: «26 октября 2027 г.» */
    fun expireCalendarLabel(): String? {
        if (expire <= 0) return null
        return SubscriptionExpireFormatter.formatCalendarDate(expire)
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
        announceHeader: String? = null,
        announceHeaders: List<String> = emptyList(),
        body: String,
    ): SubscriptionInfo? {
        val parsedUserInfo = userInfoHeader?.let(::parseUserInfoHeader)
            ?: parseUserInfoFromBody(body)
        val title = parseTitle(profileTitleHeader, body)
        val mergedAnnounceHeaders = buildList {
            if (!announceHeader.isNullOrBlank()) add(announceHeader)
            addAll(announceHeaders)
        }.distinct()
        val announce = parseAnnounce(mergedAnnounceHeaders, body)
        val deviceLimit = parseDeviceLimit(
            userInfoHeader = userInfoHeader,
            announceHeaders = mergedAnnounceHeaders,
            body = body,
        ) ?: AnnounceDeviceLimitParser.extractDeviceLimit(announce)

        return when {
            parsedUserInfo != null -> parsedUserInfo.copy(
                title = title,
                deviceLimit = deviceLimit,
                announce = announce,
            )
            title.isNotBlank() || deviceLimit != null || announce.isNotBlank() -> SubscriptionInfo(
                title = title,
                deviceLimit = deviceLimit,
                announce = announce,
            )
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

        val deviceLimit = sequenceOf(
            "device_limit",
            "devicelimit",
            "max_devices",
            "maxdevices",
            "devices",
            "hwid_limit",
            "hwidlimit",
        ).firstNotNullOfOrNull { key ->
            values[key]?.toInt()?.takeIf { it >= 0 }
        }

        return SubscriptionInfo(
            upload = values["upload"] ?: 0L,
            download = values["download"] ?: 0L,
            total = values["total"] ?: 0L,
            expire = values["expire"] ?: 0L,
            deviceLimit = deviceLimit,
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

    private fun parseDeviceLimit(
        userInfoHeader: String?,
        announceHeaders: List<String>,
        body: String,
    ): Int? {
        userInfoHeader?.let(::parseUserInfoHeader)?.deviceLimit?.let { return it }

        AnnounceDeviceLimitParser.parseAll(announceHeaders)?.let { return it }

        body.lineSequence()
            .map { it.trim().removePrefix("#").trim() }
            .forEach { line ->
                if (line.startsWith("base64:", ignoreCase = true)) {
                    AnnounceDeviceLimitParser.parse(line)?.let { return it }
                } else if (line.contains("устройств", ignoreCase = true)) {
                    AnnounceDeviceLimitParser.extractDeviceLimit(line)?.let { return it }
                }
            }
        return null
    }

    private fun parseAnnounce(announceHeaders: List<String>, body: String): String {
        for (header in announceHeaders) {
            normalizeAnnounceText(AnnounceDeviceLimitParser.decodeAnnounce(header))
                ?.let { return it }
        }

        body.lineSequence()
            .map { it.trim().removePrefix("#").trim() }
            .forEach { line ->
                when {
                    line.startsWith("announce:", ignoreCase = true) -> {
                        val raw = line.substringAfter(':').trim()
                        normalizeAnnounceText(AnnounceDeviceLimitParser.decodeAnnounce(raw))
                            ?.let { return it }
                    }
                    line.startsWith("base64:", ignoreCase = true) -> {
                        normalizeAnnounceText(AnnounceDeviceLimitParser.decodeAnnounce(line))
                            ?.let { return it }
                    }
                }
            }
        return ""
    }

    private fun normalizeAnnounceText(raw: String): String? {
        val text = raw
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .trim()
        return text.takeIf { it.isNotBlank() }
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

    private fun decodeText(raw: String): String {
        val trimmed = raw.trim()
        val base64Prefix = "base64:"

        // Нельзя гонять base64 через URLDecoder: '+' превращается в пробел и ломает декод.
        if (trimmed.startsWith(base64Prefix, ignoreCase = true)) {
            decodeBase64Text(trimmed.substring(base64Prefix.length))?.let {
                return normalizeSubscriptionTitle(it)
            }
            return ""
        }

        val urlDecoded = runCatching {
            URLDecoder.decode(trimmed, StandardCharsets.UTF_8.name())
        }.getOrDefault(trimmed).trim()

        if (urlDecoded.startsWith(base64Prefix, ignoreCase = true)) {
            decodeBase64Text(urlDecoded.substring(base64Prefix.length))?.let {
                return normalizeSubscriptionTitle(it)
            }
            return ""
        }

        return normalizeSubscriptionTitle(urlDecoded)
    }

    /** Для announce сохраняем многострочный текст; для title — как есть. */
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

/** Скорость: Б/с, КБ/с или МБ/с в зависимости от величины. */
fun formatTrafficRate(bytesPerSec: Long): String {
    val rate = bytesPerSec.coerceAtLeast(0).toDouble()
    val kb = 1024.0
    val mb = kb * 1024
    return when {
        rate >= mb -> String.format(Locale.US, "%.1f МБ/с", rate / mb)
        rate >= kb -> String.format(Locale.US, "%.1f КБ/с", rate / kb)
        else -> String.format(Locale.US, "%.0f Б/с", rate)
    }
}

fun formatTrafficSpeedLine(downlinkBytesPerSec: Long, uplinkBytesPerSec: Long): String =
    "↓ ${formatTrafficRate(downlinkBytesPerSec)}  ↑ ${formatTrafficRate(uplinkBytesPerSec)}"

