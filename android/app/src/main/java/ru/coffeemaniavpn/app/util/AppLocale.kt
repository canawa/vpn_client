package ru.coffeemaniavpn.app.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import ru.coffeemaniavpn.app.data.AppLanguage
import java.util.Locale

object AppLocale {
    @Volatile
    var current: AppLanguage = AppLanguage.DEFAULT
        private set

    fun tagsFor(language: AppLanguage): LocaleListCompat = when (language) {
        AppLanguage.RU -> LocaleListCompat.forLanguageTags("ru")
        AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
    }

    fun localeFor(language: AppLanguage): Locale = when (language) {
        AppLanguage.RU -> Locale.forLanguageTag("ru")
        AppLanguage.EN -> Locale.forLanguageTag("en")
    }

    fun wrap(context: Context, language: AppLanguage = current): Context {
        val locale = localeFor(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun apply(language: AppLanguage) {
        current = language
        AppCompatDelegate.setApplicationLocales(tagsFor(language))
    }

    fun matches(language: AppLanguage): Boolean {
        val applied = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val target = tagsFor(language).toLanguageTags()
        return applied == target
    }
}
