package work.bavshield.vpn.ui

object FlagUtils {
    const val EU_FLAG = "🇪🇺"
    private const val EU_CODE = "eu"

    fun emojiToCountryCode(flag: String): String? {
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

    fun resolveCountryCode(flag: String): String = emojiToCountryCode(flag) ?: EU_CODE

    fun flagImageUrl(flag: String): String = "https://flagcdn.com/w160/${resolveCountryCode(flag)}.png"
}
