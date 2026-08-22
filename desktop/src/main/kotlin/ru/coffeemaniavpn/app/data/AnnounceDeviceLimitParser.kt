package ru.coffeemaniavpn.app.data

import java.nio.charset.StandardCharsets
import java.util.Base64

/** Парсинг лимита устройств из header Announce (base64/текст). Без Android-зависимостей. */
object AnnounceDeviceLimitParser {
    /**
     * «Лимит устройств 📱 0» / «Device limit: 3» — число после подписи, эмодзи допускаются.
     */
    private val limitPatterns = listOf(
        Regex("""лимит\s*устройств[^\d]{0,40}(\d+)""", RegexOption.IGNORE_CASE),
        Regex("""device\s*limit[^\d]{0,40}(\d+)""", RegexOption.IGNORE_CASE),
        Regex("""max\s*devices?[^\d]{0,40}(\d+)""", RegexOption.IGNORE_CASE),
        Regex("""hwid[^\d]{0,16}limit[^\d]{0,16}(\d+)""", RegexOption.IGNORE_CASE),
    )

    fun parse(announceHeader: String?): Int? {
        val text = decodeAnnounce(announceHeader)
        if (text.isBlank()) return null
        return extractDeviceLimit(text)
    }

    fun parseAll(headers: List<String>): Int? {
        for (header in headers) {
            parse(header)?.let { return it }
        }
        return null
    }

    fun decodeAnnounce(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val trimmed = raw.trim().removeSurrounding("\"").trim()
        if (trimmed.startsWith("base64:", ignoreCase = true)) {
            return decodeBase64(trimmed.substringAfter(':')) ?: ""
        }
        return trimmed
    }

    fun extractDeviceLimit(text: String): Int? {
        if (text.isBlank()) return null
        text.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            for (pattern in limitPatterns) {
                pattern.find(trimmed)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun decodeBase64(encoded: String): String? {
        val normalized = encoded.trim()
            .replace('-', '+')
            .replace('_', '/')
            .replace("\\s".toRegex(), "")
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return runCatching {
            String(Base64.getDecoder().decode(padded), StandardCharsets.UTF_8).trim()
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}
