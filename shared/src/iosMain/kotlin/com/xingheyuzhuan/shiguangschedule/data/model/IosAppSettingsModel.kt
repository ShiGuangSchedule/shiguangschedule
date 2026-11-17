package com.xingheyuzhuan.shiguangschedule.data.model

import androidx.datastore.preferences.core.Preferences

/**
 * iOS 平台专属应用设置业务模型（骨架）
 * 后续需要时在此处添加字段和 Key
 */
data class IosAppSettingsModel(
    // 临时占位参数，满足 Kotlin 数据类必须有至少一个参数的语法要求
    val placeholder: Boolean = false
) {
    companion object {
        // TODO: 未来在这里定义 iOS 专属的 Preferences.Key

        /**
         * 从 Preferences 中解析出模型
         */
        fun fromPreferences(prefs: Preferences): IosAppSettingsModel {
            val d = IosAppSettingsModel()
            return IosAppSettingsModel(
                // placeholder = prefs[...] ?: d.placeholder
            )
        }
    }
}