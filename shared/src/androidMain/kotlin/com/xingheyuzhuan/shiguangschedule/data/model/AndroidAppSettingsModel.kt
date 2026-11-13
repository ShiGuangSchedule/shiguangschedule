package com.xingheyuzhuan.shiguangschedule.data.model

import androidx.datastore.preferences.core.Preferences

/**
 * Android 平台专属应用设置业务模型（骨架）
 * 后续可将原 Android 独有的配置项迁移至此
 */
data class AndroidAppSettingsModel(
    // 临时占位参数，满足 Kotlin 数据类必须有至少一个参数的语法要求
    val placeholder: Boolean = false
) {
    companion object {
        // TODO: 未来在这里定义 Android 专属的 Preferences.Key（如通知开关、穿戴同步等）

        /**
         * 从 Preferences 中解析出模型
         */
        fun fromPreferences(prefs: Preferences): AndroidAppSettingsModel {
            val d = AndroidAppSettingsModel()
            return AndroidAppSettingsModel(
                // placeholder = prefs[...] ?: d.placeholder
            )
        }
    }
}