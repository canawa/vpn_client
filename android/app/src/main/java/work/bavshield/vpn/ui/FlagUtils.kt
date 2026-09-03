package work.bavshield.vpn.ui

object FlagUtils {
    /** Fallback for auto-select / unknown country. */
    const val PIRATE_FLAG = "🏴‍☠️"

    fun emojiToCountryCode(flag: String): String? {
        if (flag.trim() == PIRATE_FLAG) return null

        val codePoints = flag.trim().codePoints().toArray()
        if (codePoints.size != 2) return null

        val base = 0x1F1E6
        val first = codePoints[0] - base
        val second = codePoints[1] - base
        if (first !in 0..25 || second !in 0..25) return null

        return buildString {
            append(('A'.code + first).toChar())
            append(('A'.code + second).toChar())
        }.lowercase()
    }

    fun resolveCountryCode(flag: String): String? = emojiToCountryCode(flag)

    fun flagImageUrl(flag: String): String? {
        val code = resolveCountryCode(flag) ?: return null
        return "https://flagcdn.com/w160/$code.png"
    }
}
