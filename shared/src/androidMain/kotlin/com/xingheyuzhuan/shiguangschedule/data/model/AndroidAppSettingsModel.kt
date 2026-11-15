package com.xingheyuzhuan.shiguangschedule.data.model

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import org.jetbrains.compose.resources.StringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.auto_mode_dnd
import shiguangschedule.shared.generated.resources.auto_mode_off
import shiguangschedule.shared.generated.resources.auto_mode_silent

/**
 * 上课时的自动化控制模式。
 * [OFF] 即总开关关闭，其余值表示开启并应用对应模式。
 */
enum class AutoControlMode(val value: String, val labelRes: StringResource) {
    /** 关闭自动化控制 */
    OFF("OFF", Res.string.auto_mode_off),

    /** 请勿打扰模式 */
    DND("DND", Res.string.auto_mode_dnd),

    /** 静音模式 */
    SILENT("SILENT", Res.string.auto_mode_silent);

    companion object {
        /** 根据字符串获取枚举值，匹配失败（含 null）时返回 [OFF] */
        fun fromString(value: String?): AutoControlMode =
            entries.find { it.value == value } ?: OFF
    }
}

/**
 * Android 平台专属应用设置业务模型。
 */
data class AndroidAppSettingsModel(
    /** 自动化控制模式（含关闭态），[AutoControlMode.OFF] 即关闭 */
    val autoControlMode: AutoControlMode = AutoControlMode.OFF,

    /**
     * 兼容穿戴设备同步通知。
     * 开启后关闭 Ongoing 通知以方便手环抓取；
     * 关闭后使用 Android 16 实时更新特性。
     */
    val compatWearableSync: Boolean = false
) {
    companion object {
        val KEY_AUTO_CONTROL_MODE = stringPreferencesKey("auto_control_mode")
        val KEY_COMPAT_WEARABLE_SYNC = booleanPreferencesKey("compat_wearable_sync")

        /** 从 Preferences 中解析出模型，缺失键时使用默认值。 */
        fun fromPreferences(prefs: Preferences): AndroidAppSettingsModel {
            val d = AndroidAppSettingsModel()
            return AndroidAppSettingsModel(
                autoControlMode = AutoControlMode.fromString(prefs[KEY_AUTO_CONTROL_MODE]),
                compatWearableSync = prefs[KEY_COMPAT_WEARABLE_SYNC] ?: d.compatWearableSync
            )
        }
    }
}