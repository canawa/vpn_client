package ru.coffeemaniavpn.app.widget

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import ru.coffeemaniavpn.app.R

/** SF Pro — те же начертания, что [ru.coffeemaniavpn.app.ui.ClevFontFamily]. */
object WidgetFonts {
    @Volatile private var regular: Typeface? = null
    @Volatile private var medium: Typeface? = null
    @Volatile private var semibold: Typeface? = null
    @Volatile private var bold: Typeface? = null

    fun regular(context: Context): Typeface =
        regular ?: load(context, R.font.sf_pro_regular).also { regular = it }

    fun medium(context: Context): Typeface =
        medium ?: load(context, R.font.sf_pro_medium).also { medium = it }

    fun semibold(context: Context): Typeface =
        semibold ?: load(context, R.font.sf_pro_semibold).also { semibold = it }

    fun bold(context: Context): Typeface =
        bold ?: load(context, R.font.sf_pro_bold).also { bold = it }

    private fun load(context: Context, resId: Int): Typeface =
        ResourcesCompat.getFont(context, resId) ?: Typeface.DEFAULT_BOLD
}
