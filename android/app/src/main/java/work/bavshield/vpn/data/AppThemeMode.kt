package work.bavshield.vpn.data

import androidx.annotation.StringRes
import work.bavshield.vpn.R

enum class AppThemeMode(@StringRes val labelRes: Int) {
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
    SYSTEM(R.string.theme_system),
    ;

    companion object {
        val DEFAULT = SYSTEM

        fun fromStored(value: String?): AppThemeMode {
            if (value == null) return DEFAULT
            return entries.find { it.name == value } ?: DEFAULT
        }
    }
}
