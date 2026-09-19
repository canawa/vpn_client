package ru.coffeemaniavpn.app.ui

import ru.coffeemaniavpn.app.data.ProxyNode

/**
 * Фиксированные категории слайдера на Home.
 * Классификация — по тексту имени сервера из подписки.
 */
enum class ServerCategory {
    BYPASS,
    AUTO,
    ;

    fun matches(rawName: String): Boolean {
        val name = rawName.lowercase()
        return when (this) {
            BYPASS -> BYPASS_MARKERS.any { it in name }
            AUTO -> AUTO_REGEX.containsMatchIn(name) && BYPASS_MARKERS.none { it in name }
        }
    }

    companion object {
        val ALL: List<ServerCategory> = entries.toList()

        private val BYPASS_MARKERS = listOf(
            "обход",
            "bypass",
            "белых ip",
            "белый ip",
            "white ip",
            "white-list",
            "whitelist",
        )

        /** «Авто» как отдельная метка (🇪🇺 Авто | …), не часть других слов. */
        private val AUTO_REGEX = Regex("""(^|[^\p{L}])авто([^\p{L}]|$)""")

        fun categoriesOf(node: ProxyNode): Set<ServerCategory> =
            ALL.filterTo(mutableSetOf()) { it.matches(node.name) }
    }
}
