package com.xingheyuzhuan.shiguangschedule.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.AppThemeMode
import com.xingheyuzhuan.shiguangschedule.data.model.StartScreen
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 界面原子状态类：包含设置页渲染所需的全部数据包
 */
data class SettingsUiState(
    val appSettings: AppSettingsModel = AppSettingsModel(),
    val courseConfig: CourseTableConfig? = null,
    val currentWeek: Int? = null,
    val isReady: Boolean = false
)

@KoinViewModel
class SettingsViewModel(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    // 1. 基础配置流 (DataStore)
    private val appSettingsFlow = appSettingsRepository.getAppSettings()

    // 2. 动态物理配置流 (Room)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val courseTableConfigFlow = appSettingsFlow.flatMapLatest { settings ->
        val id = settings.currentCourseTableId
        if (id.isNotEmpty()) appSettingsRepository.getCourseTableConfigFlow(id)
        else flowOf(null)
    }

    /**
     * 核心优化：聚合 UI 状态流
     * 使用 combine 将多个异步源合并为一个原子包，消除状态裂缝
     */
    val uiState: StateFlow<SettingsUiState> = combine(
        appSettingsFlow,
        courseTableConfigFlow
    ) { settings, config ->
        val week = if (config != null) {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val rawWeek = appSettingsRepository.getWeekIndexAtDate(
                targetDate = today,
                startDateStr = config.semesterStartDate,
                firstDayOfWeekInt = config.firstDayOfWeek
            )
            rawWeek?.takeIf { it in 1..config.semesterTotalWeeks }
        } else null

        SettingsUiState(
            appSettings = settings,
            courseConfig = config,
            currentWeek = week,
            isReady = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = SettingsUiState()
    )

    /**
     * 是否显示非本周课程
     */
    fun onShowNonCurrentWeekChanged(show: Boolean) {
        viewModelScope.launch {
            val currentSettings = uiState.value.appSettings
            val updatedSettings = currentSettings.copy(showNonCurrentWeekCourses = show)
            appSettingsRepository.insertOrUpdateAppSettings(updatedSettings)
        }
    }

    /**
     * 更新周末显示
     */
    fun onShowWeekendsChanged(show: Boolean) {
        viewModelScope.launch {
            uiState.value.courseConfig?.let { currentConfig ->
                val update = if (!show) {
                    currentConfig.copy(showWeekends = false, firstDayOfWeek = DayOfWeek.MONDAY.isoDayNumber)
                } else {
                    currentConfig.copy(showWeekends = true)
                }
                appSettingsRepository.insertOrUpdateCourseConfig(update)
            }
        }
    }

    /**
     * 更新起始日期
     */
    fun onSemesterStartDateSelected(selectedDateMillis: Long?) {
        viewModelScope.launch {
            val dateMillis = selectedDateMillis ?: return@launch
            uiState.value.courseConfig?.let { currentConfig ->
                val selectedDate = Instant.fromEpochMilliseconds(dateMillis)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                val newConfig = currentConfig.copy(
                    semesterStartDate = selectedDate.toString()
                )
                appSettingsRepository.insertOrUpdateCourseConfig(newConfig)
            }
        }
    }

    /**
     * 更新总周数
     */
    fun onSemesterTotalWeeksSelected(totalWeeks: Int) {
        viewModelScope.launch {
            uiState.value.courseConfig?.let {
                appSettingsRepository.insertOrUpdateCourseConfig(it.copy(semesterTotalWeeks = totalWeeks))
            }
        }
    }

    /**
     * 手动对齐周数 (联动：反向推算开学日期)
     */
    fun onCurrentWeekManuallySet(weekNumber: Int?) {
        viewModelScope.launch {
            appSettingsRepository.setSemesterStartDateFromWeek(weekNumber)
        }
    }

    /**
     * 更新每周起始日
     */
    fun onFirstDayOfWeekSelected(dayOfWeekInt: Int) {
        viewModelScope.launch {
            uiState.value.courseConfig?.let { currentConfig ->
                appSettingsRepository.insertOrUpdateCourseConfig(currentConfig.copy(firstDayOfWeek = dayOfWeekInt))
            }
        }
    }

    /**
     * 更新应用启动时的默认主页
     */
    fun onStartScreenChanged(newScreen: StartScreen) {
        viewModelScope.launch {
            val currentSettings = uiState.value.appSettings
            val updatedSettings = currentSettings.copy(startScreen = newScreen)
            appSettingsRepository.insertOrUpdateAppSettings(updatedSettings)
        }
    }

    /**
     * 主题模式 (跟随系统/亮色/深色)
     */
    fun onThemeModeChanged(newMode: AppThemeMode) {
        viewModelScope.launch {
            val currentSettings = uiState.value.appSettings
            val updatedSettings = currentSettings.copy(themeMode = newMode)
            appSettingsRepository.insertOrUpdateAppSettings(updatedSettings)
        }
    }

    /**
     * 动态取色开关 (Material You)
     */
    fun onUseDynamicColorChanged(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = uiState.value.appSettings
            val updatedSettings = currentSettings.copy(useDynamicColor = enabled)
            appSettingsRepository.insertOrUpdateAppSettings(updatedSettings)
        }
    }

    /**
     * 自定义浅色模式种子色（传 Color 则修改，传 null 则重置）
     */
    fun onCustomLightPrimaryChanged(color: Color? = null) {
        viewModelScope.launch {
            val currentSettings = uiState.value.appSettings
            val defaultSettings = AppSettingsModel()
            val newColorArgb = color?.toArgb()?.toLong()
                ?: defaultSettings.customPrimaryColor.light

            val updatedSettings = currentSettings.copy(
                customPrimaryColor = currentSettings.customPrimaryColor.copy(light = newColorArgb)
            )
            appSettingsRepository.insertOrUpdateAppSettings(updatedSettings)
        }
    }

    /**
     * 自定义深色模式种子色（传 Color 则修改，传 null 则重置）
     */
    fun onCustomDarkPrimaryChanged(color: Color? = null) {
        viewModelScope.launch {
            val currentSettings = uiState.value.appSettings
            val defaultSettings = AppSettingsModel()
            val newColorArgb = color?.toArgb()?.toLong()
                ?: defaultSettings.customPrimaryColor.dark

            val updatedSettings = currentSettings.copy(
                customPrimaryColor = currentSettings.customPrimaryColor.copy(dark = newColorArgb)
            )
            appSettingsRepository.insertOrUpdateAppSettings(updatedSettings)
        }
    }

    /**
     * 更新开发者模式开关状态
     */
    fun onDeveloperModeChanged(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = uiState.value.appSettings
            val updatedSettings = currentSettings.copy(developerModeEnabled = enabled)
            appSettingsRepository.insertOrUpdateAppSettings(updatedSettings)
        }
    }
}