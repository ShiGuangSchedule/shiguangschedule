package com.xingheyuzhuan.shiguangschedule.data.model

import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xingheyuzhuan.shiguangschedule.ui.theme.DefaultThemeColor
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.StringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.nav_course_schedule
import shiguangschedule.shared.generated.resources.nav_today_schedule
import shiguangschedule.shared.generated.resources.theme_dark
import shiguangschedule.shared.generated.resources.theme_follow_system
import shiguangschedule.shared.generated.resources.theme_light

/**
 * 通用的深浅色配置包装类
 *
 * @param light 浅色模式下的配置项
 * @param dark 深色模式下的配置项
 */
@Serializable
data class LightDarkValue<T>(
    val light: T,
    val dark: T
)

/**
 * 节假日/调休 数据结构
 *
 * @property name 名称字符字段
 * @property isHoliday 是否是假期 (true: 假期/放假; false: 调休/补班)
 * @property dates 所属日期集合 (KMP 官方库强类型，默认以 ISO-8601 "YYYY-MM-DD" 序列化存储)
 */
@Serializable
data class Holiday(
    val name: String,
    val isHoliday: Boolean = true,
    val dates: Set<LocalDate> = emptySet()
)

/**
 * 上课时的自动化控制模式枚举
 */
enum class AutoControlMode(val value: String) {
    /** 请勿打扰模式 */
    DND("DND"),

    /** 静音模式 */
    SILENT("SILENT");

    companion object {
        /**
         * 根据字符串获取对应的枚举值，如果匹配失败则返回默认的 DND 模式
         */
        fun fromString(value: String?): AutoControlMode {
            return entries.find { it.value == value } ?: DND
        }
    }
}

/**
 * 可选的启动页面枚举
 */
enum class StartScreen(val value: String, val labelRes: StringResource) {
    /** 周课表 */
    COURSE_SCHEDULE("COURSE_SCHEDULE", Res.string.nav_course_schedule),

    /** 今日课表 */
    TODAY_SCHEDULE("TODAY_SCHEDULE", Res.string.nav_today_schedule);

    companion object {
        fun fromString(value: String?): StartScreen {
            return entries.find { it.value == value } ?: COURSE_SCHEDULE
        }
    }
}

/**
 * 应用主题模式枚举
 */
enum class AppThemeMode(val value: String, val labelRes: StringResource) {
    /** 跟随系统 */
    FOLLOW_SYSTEM("FOLLOW_SYSTEM", Res.string.theme_follow_system),

    /** 浅色模式 */
    LIGHT("LIGHT", Res.string.theme_light),

    /** 深色模式 */
    DARK("DARK", Res.string.theme_dark);

    companion object {
        fun fromString(value: String?): AppThemeMode? {
            return entries.find { it.value == value }
        }
    }
}

/**
 * 应用全局设置业务模型（DataStore 专用）
 * 集中管理业务字段、存储键 (Keys) 以及默认值。
 */
data class AppSettingsModel(
    /** 当前正在使用的课表 ID */
    val currentCourseTableId: String = "",

    /** 是否开启上课前提醒 */
    val reminderEnabled: Boolean = false,

    /** 提前提醒的时间（分钟） */
    val remindBeforeMinutes: Int = 15,

    /** 节假日及调休列表 */
    val holidays: List<Holiday> = emptyList(),

    /** 自动化模式的总开关 */
    val autoModeEnabled: Boolean = false,

    /** 自动化控制的具体模式，限定为 [AutoControlMode] */
    val autoControlMode: AutoControlMode = AutoControlMode.DND,

    /**
     * 兼容穿戴设备同步通知的开关
     * true: 开启兼容模式（关闭 Ongoing，方便手环抓取）
     * false: 关闭兼容模式（默认，使用 Android 16 实时更新特性）
     */
    val compatWearableSync: Boolean = false,

    /** 是否显示非本周课程 */
    val showNonCurrentWeekCourses: Boolean = false,

    /** 应用启动时显示的页面 */
    val startScreen: StartScreen = StartScreen.COURSE_SCHEDULE,

    /** 应用主题模式 */
    val themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,

    /** 是否开启动态取色 (Material You) */
    val useDynamicColor: Boolean = true,

    /** 自定义主题主色（包含浅色和深色模式） */
    val customPrimaryColor: LightDarkValue<Long> = LightDarkValue(
        light = DefaultThemeColor.toArgb().toLong(),
        dark = DefaultThemeColor.toArgb().toLong()
    ),

    /** 背景壁纸路径 (包含浅色和深色模式，存储在私有目录下的绝对路径) */
    val backgroundImagePath: LightDarkValue<String> = LightDarkValue(
        light = "",
        dark = ""
    ),

    /** 开发者功能总开关（默认关闭） */
    val developerModeEnabled: Boolean = false,
) {
    /**
     * 将 DataStore 的 Key 定义在伴生对象中。
     * 这样在 Repository 中可以直接通过 AppSettingsModel.KEY_xxx 访问，
     * 避免了修改一处逻辑需要动多个文件的问题。
     */
    companion object {
        val KEY_CURRENT_COURSE_TABLE_ID = stringPreferencesKey("current_course_table_id")
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val KEY_REMIND_BEFORE_MINUTES = intPreferencesKey("remind_before_minutes")
        val KEY_HOLIDAYS_JSON = stringPreferencesKey("holidays_json")
        val KEY_AUTO_MODE_ENABLED = booleanPreferencesKey("auto_mode_enabled")
        val KEY_AUTO_CONTROL_MODE = stringPreferencesKey("auto_control_mode")
        val KEY_COMPAT_WEARABLE_SYNC = booleanPreferencesKey("compat_wearable_sync")
        val KEY_SHOW_NON_CURRENT_WEEK_COURSES = booleanPreferencesKey("show_non_current_week_courses")
        val KEY_START_SCREEN = stringPreferencesKey("start_screen")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val KEY_CUSTOM_LIGHT_PRIMARY = longPreferencesKey("custom_light_primary")
        val KEY_CUSTOM_DARK_PRIMARY = longPreferencesKey("custom_dark_primary")
        val KEY_BACKGROUND_IMAGE_PATH_LIGHT = stringPreferencesKey("background_image_path_light")
        val KEY_BACKGROUND_IMAGE_PATH_DARK = stringPreferencesKey("background_image_path_dark")
        val KEY_DEVELOPER_MODE_ENABLED = booleanPreferencesKey("developer_mode_enabled")

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * 从 Preferences 中解析出 AppSettingsModel
         */
        fun fromPreferences(prefs: Preferences, fallbackTableId: String): AppSettingsModel {
            val d = AppSettingsModel() // 默认值模板

            val rawHolidaysJson = prefs[KEY_HOLIDAYS_JSON]
            val parsedHolidays = if (!rawHolidaysJson.isNullOrEmpty()) {
                runCatching { json.decodeFromString<List<Holiday>>(rawHolidaysJson) }.getOrDefault(emptyList())
            } else {
                emptyList()
            }

            return AppSettingsModel(
                currentCourseTableId = prefs[KEY_CURRENT_COURSE_TABLE_ID] ?: fallbackTableId.ifEmpty { d.currentCourseTableId },
                reminderEnabled = prefs[KEY_REMINDER_ENABLED] ?: d.reminderEnabled,
                remindBeforeMinutes = prefs[KEY_REMIND_BEFORE_MINUTES] ?: d.remindBeforeMinutes,
                holidays = parsedHolidays,
                autoModeEnabled = prefs[KEY_AUTO_MODE_ENABLED] ?: d.autoModeEnabled,
                autoControlMode = AutoControlMode.fromString(prefs[KEY_AUTO_CONTROL_MODE]),
                compatWearableSync = prefs[KEY_COMPAT_WEARABLE_SYNC] ?: d.compatWearableSync,
                showNonCurrentWeekCourses = prefs[KEY_SHOW_NON_CURRENT_WEEK_COURSES] ?: d.showNonCurrentWeekCourses,
                startScreen = prefs[KEY_START_SCREEN]?.let { StartScreen.fromString(it) } ?: d.startScreen,
                themeMode = prefs[KEY_THEME_MODE]?.let { AppThemeMode.fromString(it) } ?: d.themeMode,
                useDynamicColor = prefs[KEY_USE_DYNAMIC_COLOR] ?: d.useDynamicColor,
                customPrimaryColor = LightDarkValue(
                    light = prefs[KEY_CUSTOM_LIGHT_PRIMARY] ?: d.customPrimaryColor.light,
                    dark = prefs[KEY_CUSTOM_DARK_PRIMARY] ?: d.customPrimaryColor.dark
                ),
                backgroundImagePath = LightDarkValue(
                    light = prefs[KEY_BACKGROUND_IMAGE_PATH_LIGHT] ?: d.backgroundImagePath.light,
                    dark = prefs[KEY_BACKGROUND_IMAGE_PATH_DARK] ?: d.backgroundImagePath.dark
                ),
                developerModeEnabled = prefs[KEY_DEVELOPER_MODE_ENABLED] ?: d.developerModeEnabled,
            )
        }
    }
}