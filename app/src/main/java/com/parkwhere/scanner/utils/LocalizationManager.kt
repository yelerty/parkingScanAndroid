package com.parkwhere.scanner.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object LocalizationManager {

    private const val PREF_NAME = "parking_scanner"
    private const val KEY_LANGUAGE = "selected_language"

    // 지원하는 언어 목록 (iOS와 동일)
    val supportedLanguages = listOf(
        Language("한국어", "ko"),
        Language("English", "en"),
        Language("日本語", "ja"),
        Language("Français", "fr"),
        Language("中文", "zh"),
        Language("हिन्दी", "hi"),
        Language("Español", "es"),
        Language("Português", "pt"),
        Language("Deutsch", "de"),
        Language("العربية", "ar")
    )

    data class Language(val displayName: String, val code: String)

    fun getCurrentLanguage(context: Context): Language {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANGUAGE, "ko") ?: "ko"
        return supportedLanguages.find { it.code == code } ?: supportedLanguages[0]
    }

    fun setLanguage(context: Context, language: Language) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun applyLanguage(context: Context): Context {
        val language = getCurrentLanguage(context)
        val locale = Locale(language.code)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    fun updateConfiguration(context: Context) {
        val language = getCurrentLanguage(context)
        val locale = Locale(language.code)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
