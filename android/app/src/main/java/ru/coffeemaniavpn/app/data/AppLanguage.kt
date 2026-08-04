package ru.coffeemaniavpn.app.data

enum class AppLanguage(val label: String) {
    SYSTEM("Системный"),
    RU("Русский"),
    EN("English"),
    ;

    companion object {
        val DEFAULT = RU

        fun fromStored(value: String?): AppLanguage {
            if (value == null) return DEFAULT
            return entries.find { it.name == value } ?: DEFAULT
        }
    }
}

enum class TrafficRoutingMode(val label: String) {
    GLOBAL("Весь трафик через VPN"),
    SMART("Умный режим"),
    CUSTOM("Только свои правила"),
    ;

    companion object {
        val DEFAULT = GLOBAL

        fun fromStored(value: String?): TrafficRoutingMode {
            if (value == null) return DEFAULT
            return entries.find { it.name == value } ?: DEFAULT
        }
    }
}
