package com.xingheyuzhuan.shiguangschedule.data.model

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * JVM / 桌面平台专属应用设置业务模型
 */
data class JvmAppSettingsModel(
    /** 应用语言标签（如 "zh-CN", "en" 等，空字符串表示跟随系统） */
    val languageTag: String = ""
) {
    companion object {
        /** 语言设置的存储键 */
        val KEY_LANGUAGE_TAG = stringPreferencesKey("app_language_tag")

        /**
         * 从 Preferences 中解析出模型
         */
        fun fromPreferences(prefs: Preferences): JvmAppSettingsModel {
            val defaultModel = JvmAppSettingsModel()
            return JvmAppSettingsModel(
                languageTag = prefs[KEY_LANGUAGE_TAG] ?: defaultModel.languageTag
            )
        }
    }
}