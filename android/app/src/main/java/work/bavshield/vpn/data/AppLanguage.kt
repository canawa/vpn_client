package work.bavshield.vpn.data

import java.util.Locale

enum class AppLanguage(val tag: String) {
    RU("ru"),
    EN("en"),
    ;

    val locale: Locale
        get() = Locale.forLanguageTag(tag)

    companion object {
        val DEFAULT = RU

        fun fromStored(value: String?): AppLanguage {
            if (value.isNullOrBlank()) return DEFAULT
            return entries.find { it.tag.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
                ?: DEFAULT
        }
    }
}
