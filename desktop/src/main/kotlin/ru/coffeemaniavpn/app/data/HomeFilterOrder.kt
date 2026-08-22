package ru.coffeemaniavpn.app.data

/**
 * Порядок чипов фильтров на Home.
 * `"ALL"` — «Все», остальное — имена категорий без AUTO.
 */
object HomeFilterOrder {
    const val ALL_ID = "ALL"

    val DEFAULT: List<String> = listOf(
        ALL_ID,
        "BYPASS",
        "SPEED",
        "YOUTUBE",
        "GAMING",
    )

    fun normalize(stored: List<String>): List<String> {
        val known = DEFAULT.toSet()
        val cleaned = stored.filter { it in known }.distinct()
        return cleaned + DEFAULT.filter { it !in cleaned.toSet() }
    }
}
