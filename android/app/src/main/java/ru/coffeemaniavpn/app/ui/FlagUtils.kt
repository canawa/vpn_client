package ru.coffeemaniavpn.app.ui

object FlagUtils {
    private val regionalIndicatorBase = 0x1F1E6

    /** ISO 3166-1 alpha-2 из emoji-флага (🇳🇱 → nl). */
    fun emojiToCountryCode(flag: String): String? {
        val codePoints = flag.trim()
            .codePoints()
            .toArray()
            .filter { it != 0xFE0F && it != 0x200D }
        if (codePoints.size < 2) return null

        // Берём первую пару regional indicators (на случай лишних символов).
        var i = 0
        while (i <= codePoints.size - 2) {
            val first = codePoints[i] - regionalIndicatorBase
            val second = codePoints[i + 1] - regionalIndicatorBase
            if (first in 0..25 && second in 0..25) {
                return buildString {
                    append(('a'.code + first).toChar())
                    append(('a'.code + second).toChar())
                }
            }
            i++
        }
        return null
    }

    /** Нормализует emoji / ISO-код / неизвестное → lowercase ISO или null. */
    fun resolveCountryCode(flagOrCode: String): String? {
        val trimmed = flagOrCode.trim()
        if (trimmed.isEmpty()) return null

        emojiToCountryCode(trimmed)?.let { return it }

        val ascii = trimmed.lowercase()
        if (ascii.length == 2 && ascii.all { it in 'a'..'z' }) {
            return ascii
        }
        return null
    }

    /** Локальный asset; null если файла нет / код неизвестен. */
    fun flagAssetPath(flagOrCode: String): String? {
        val code = resolveCountryCode(flagOrCode) ?: return null
        return "file:///android_asset/flags/$code.png"
    }

    fun flagCdnUrl(flagOrCode: String, widthPx: Int = 160): String? {
        val code = resolveCountryCode(flagOrCode) ?: return null
        val width = when {
            widthPx <= 40 -> 40
            widthPx <= 80 -> 80
            widthPx <= 160 -> 160
            widthPx <= 320 -> 320
            else -> 640
        }
        return "https://flagcdn.com/w$width/$code.png"
    }

    fun isGlobeOrUnknown(flagOrCode: String): Boolean {
        val t = flagOrCode.trim()
        if (t == "🌐" || t == "🌍" || t == "🌎" || t == "🌏" || t == "🗺") return true
        return resolveCountryCode(t) == null
    }
}
