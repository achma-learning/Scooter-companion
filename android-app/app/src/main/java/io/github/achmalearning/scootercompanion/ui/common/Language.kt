package io.github.achmalearning.scootercompanion.ui.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * The in-app language. Arabic uses the Moroccan locale (ar-MA) so numbers keep Western
 * digits, as on Moroccan receipts and pump displays, and stay parseable in number fields.
 */
enum class AppLanguage(val tag: String, val label: String) {
    SYSTEM("", ""),
    FRENCH("fr", "Français"),
    ENGLISH("en", "English"),
    ARABIC("ar-MA", "العربية");

    companion object {
        val choices = listOf(FRENCH, ENGLISH, ARABIC)

        fun current(): AppLanguage {
            val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            return entries.firstOrNull { it != SYSTEM && tag.startsWith(it.tag.substringBefore('-')) } ?: SYSTEM
        }

        /** Applies [language] app-wide; the activity is recreated in the new language. */
        fun apply(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(
                if (language == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(language.tag)
            )
        }
    }
}
