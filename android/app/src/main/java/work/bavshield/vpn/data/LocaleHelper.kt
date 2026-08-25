package work.bavshield.vpn.data

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Applies the user-selected language independently of the system locale.
 * Stored in SharedPreferences so [attachBaseContext] can read it synchronously.
 */
object LocaleHelper {
    private const val PREFS = "bavshield_locale"
    private const val KEY_LANGUAGE = "language"

    fun current(context: Context): AppLanguage {
        val stored = prefs(context).getString(KEY_LANGUAGE, null)
        return AppLanguage.fromStored(stored)
    }

    fun persist(context: Context, language: AppLanguage) {
        prefs(context)
            .edit()
            .putString(KEY_LANGUAGE, language.tag)
            .apply()
    }

    fun wrap(base: Context): Context {
        val language = current(base)
        val locale = language.locale
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    fun strings(context: Context): Context {
        val app = context.applicationContext ?: context
        return wrap(app)
    }

    private fun prefs(context: Context) =
        (context.applicationContext ?: context)
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
