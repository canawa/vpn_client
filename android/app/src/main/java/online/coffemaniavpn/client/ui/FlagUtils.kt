package online.coffemaniavpn.client.ui

object FlagUtils {
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

    fun flagImageUrl(flag: String): String {
        emojiToCountryCode(flag)?.let { code ->
            return "https://flagcdn.com/w160/$code.png"
        }

        val hex = buildList {
            flag.trim().codePoints().forEach { add(String.format("%x", it)) }
        }.joinToString("-")

        return "https://cdn.jsdelivr.net/gh/twitter/tweet-emoji@v14.0.2/assets/72x72/$hex.png"
    }
}
