package work.bavshield.vpn.data

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import work.bavshield.vpn.R

enum class AppColorScheme(
    @StringRes val labelRes: Int,
    val swatch: Color,
) {
    FOREST(R.string.color_forest, Color(0xFF7C9D8A)),
    OCEAN(R.string.color_ocean, Color(0xFF3D6B8A)),
    SLATE(R.string.color_slate, Color(0xFF6B7280)),
    TEAL(R.string.color_teal, Color(0xFF2A9B8F)),
    INDIGO(R.string.color_indigo, Color(0xFF5B5A8C)),
    AMBER(R.string.color_amber, Color(0xFFC4A46A)),
    CRIMSON(R.string.color_crimson, Color(0xFFB07078)),
    ;

    companion object {
        val DEFAULT = FOREST

        fun fromStored(value: String?): AppColorScheme {
            if (value.isNullOrBlank()) return DEFAULT
            return entries.find { it.name == value } ?: DEFAULT
        }
    }
}
