package ru.coffeemaniavpn.app.data

enum class AppLanguage {
    RU,
    EN,
    ;

    companion object {
        val DEFAULT = RU

        val selectable: List<AppLanguage> = listOf(RU, EN)

        fun fromStored(value: String?): AppLanguage {
            if (value == null) return DEFAULT
            return when (value) {
                EN.name -> EN
                "SYSTEM" -> RU
                else -> entries.find { it.name == value } ?: DEFAULT
            }
        }
    }
}

enum class TrafficRoutingMode {
    GLOBAL,
    CUSTOM,
    ;

    companion object {
        val DEFAULT = GLOBAL

        val selectable: List<TrafficRoutingMode> = listOf(GLOBAL, CUSTOM)

        fun fromStored(value: String?): TrafficRoutingMode {
            if (value == null) return DEFAULT
            if (value == "SMART") return GLOBAL
            return entries.find { it.name == value } ?: DEFAULT
        }
    }
}
