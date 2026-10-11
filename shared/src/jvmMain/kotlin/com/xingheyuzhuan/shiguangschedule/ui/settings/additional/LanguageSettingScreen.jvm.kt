package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import com.xingheyuzhuan.shiguangschedule.data.model.JvmBasePreferences
import java.util.Locale

/**
 * JVM / Desktop 平台的语言配置管理器 actual 实现
 * 继承 [JvmBasePreferences]，数据统一落盘保存在 AppData/files/locale_settings.properties
 */
actual object PlatformLocaleManager : JvmBasePreferences("locale_settings.properties") {

    private const val PREF_KEY_LANGUAGE = "app_language_tag"

    init {
        applyStoredOrSystemLanguage()
    }

    private fun applyStoredOrSystemLanguage() {
        val savedTag = getString(PREF_KEY_LANGUAGE, "")
        if (savedTag.isEmpty()) {
            Locale.setDefault(getTrueSystemLocale())
        } else {
            Locale.setDefault(Locale.forLanguageTag(savedTag))
        }
    }

    private fun getTrueSystemLocale(): Locale {
        val language = System.getProperty("user.language") ?: "en"
        val script = System.getProperty("user.script") ?: ""
        val country = System.getProperty("user.country") ?: ""
        val variant = System.getProperty("user.variant") ?: ""

        return Locale.Builder().apply {
            if (language.isNotEmpty()) setLanguage(language)
            if (script.isNotEmpty()) setScript(script)
            if (country.isNotEmpty()) setRegion(country)
            if (variant.isNotEmpty()) setVariant(variant)
        }.build()
    }

    actual fun setLanguageTag(tag: String) {
        if (tag.isEmpty()) {
            putString(PREF_KEY_LANGUAGE, null)
            Locale.setDefault(getTrueSystemLocale())
        } else {
            putString(PREF_KEY_LANGUAGE, tag)
            Locale.setDefault(Locale.forLanguageTag(tag))
        }
    }

    actual fun getCurrentLanguageTag(): String {
        return getString(PREF_KEY_LANGUAGE, "")
    }
}