package ru.coffeemaniavpn.app.data

enum class AppThemeMode(val label: String) {
    LIGHT("Светлая"),
    DARK("Тёмная"),
    SYSTEM("Как в системе"),
    ;

    companion object {
        val DEFAULT = SYSTEM

        fun fromStored(value: String?): AppThemeMode {
            if (value == null) return DEFAULT
            return entries.find { it.name == value } ?: DEFAULT
        }
    }
}
