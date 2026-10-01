package com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.tweaks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.model.Holiday
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.StringResource
import org.koin.core.annotation.KoinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.error_tweak_failed
import shiguangschedule.shared.generated.resources.error_tweak_no_table_or_semester
import shiguangschedule.shared.generated.resources.error_tweak_same_day
import shiguangschedule.shared.generated.resources.toast_tweak_success
import kotlin.time.Clock

/**
 * 带有格式化参数的资源字符串包装类，避免硬编码
 */
data class UiTextRes(
    val resource: StringResource,
    val args: List<Any> = emptyList()
)

/**
 * 获取当前系统本地日期的辅助函数
 */
private fun todayLocalDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/**
 * 调课页面 UI 状态。
 */
data class TweakScheduleUiState(
    // UI 显示所需的数据
    val allCourseTables: List<CourseTable> = emptyList(),
    val selectedCourseTable: CourseTable? = null,
    val fromDate: LocalDate = todayLocalDate(),
    val toDate: LocalDate = todayLocalDate(),
    val fromCourses: List<CourseWithWeeks> = emptyList(),
    val toCourses: List<CourseWithWeeks> = emptyList(),
    val tweakMode: CourseTableRepository.TweakMode = CourseTableRepository.TweakMode.MERGE,
    val workdays: List<Holiday> = emptyList(),

    // 业务逻辑和状态管理所需的数据
    val isSemesterSet: Boolean = false,
    val semesterStartDate: LocalDate? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiTextRes? = null,
    val successMessage: UiTextRes? = null
)

/**
 * 课程调动页面的 ViewModel。
 */
@KoinViewModel
class TweakScheduleViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository
) : ViewModel() {

    // UI 暴露的状态
    private val _uiState = MutableStateFlow(TweakScheduleUiState())
    val uiState: StateFlow<TweakScheduleUiState> = _uiState.asStateFlow()

    // 内部存储用户选择的私有 Flow
    private val _fromDate = MutableStateFlow(todayLocalDate())
    private val _toDate = MutableStateFlow(todayLocalDate())
    private val _selectedCourseTableByUser = MutableStateFlow<CourseTable?>(null)

    init {
        viewModelScope.launch {
            refreshUiState(isInitialLoad = true)
        }
    }

    /**
     * 刷新 UI 状态：加载配置、课表、补班数据以及预览区域的课程。
     */
    private suspend fun refreshUiState(isInitialLoad: Boolean = false) {
        val settings = appSettingsRepository.getAppSettings().first()
        val allTables = courseTableRepository.getAllCourseTables().first()

        // 仅过滤出 isHoliday == false（非假期/即补班）且设置了具体日期的记录
        val availableWorkdays = settings.holidays.filter { !it.isHoliday && it.dates.isNotEmpty() }

        val selectedTable = if (isInitialLoad) {
            val defaultSelectedTable = allTables.find { it.id == settings.currentCourseTableId }
            _selectedCourseTableByUser.value = defaultSelectedTable
            defaultSelectedTable
        } else {
            _selectedCourseTableByUser.value
        }

        val currentFromDate = _fromDate.value
        val currentToDate = _toDate.value

        val currentTableId = selectedTable?.id
        val courseConfig = if (currentTableId != null) {
            appSettingsRepository.getCourseConfigOnce(currentTableId)
        } else {
            null
        }

        val semesterStartDateString = courseConfig?.semesterStartDate
        val semesterStartDate: LocalDate? = try {
            semesterStartDateString?.let { LocalDate.parse(it) }
        } catch (_: Exception) {
            null
        }
        val isSemesterSet = semesterStartDate != null

        var fromCourses = emptyList<CourseWithWeeks>()
        var toCourses = emptyList<CourseWithWeeks>()

        if (isSemesterSet && selectedTable != null) {
            val fromWeekNumber = appSettingsRepository.getWeekIndexAtDate(
                targetDate = currentFromDate,
                startDateStr = courseConfig?.semesterStartDate,
                firstDayOfWeekInt = courseConfig?.firstDayOfWeek ?: 1
            ) ?: 1

            val fromDay = currentFromDate.dayOfWeek.ordinal + 1

            val toWeekNumber = appSettingsRepository.getWeekIndexAtDate(
                targetDate = currentToDate,
                startDateStr = courseConfig?.semesterStartDate,
                firstDayOfWeekInt = courseConfig?.firstDayOfWeek ?: 1
            ) ?: 1

            val toDay = currentToDate.dayOfWeek.ordinal + 1

            fromCourses = courseTableRepository.getCoursesForDay(selectedTable.id, fromWeekNumber, fromDay).first()
            toCourses = courseTableRepository.getCoursesForDay(selectedTable.id, toWeekNumber, toDay).first()
        }

        _uiState.update {
            it.copy(
                allCourseTables = allTables,
                isSemesterSet = isSemesterSet,
                selectedCourseTable = selectedTable,
                fromDate = currentFromDate,
                toDate = currentToDate,
                fromCourses = fromCourses,
                toCourses = toCourses,
                semesterStartDate = semesterStartDate,
                workdays = availableWorkdays,
                isLoading = false
            )
        }
    }

    // 响应 UI 层更改调课模式
    fun onTweakModeChanged(mode: CourseTableRepository.TweakMode) {
        _uiState.update { it.copy(tweakMode = mode) }
    }

    fun onCourseTableSelected(courseTable: CourseTable) {
        _selectedCourseTableByUser.value = courseTable
        viewModelScope.launch { refreshUiState() }
    }

    fun onFromDateSelected(date: LocalDate) {
        _fromDate.value = date
        viewModelScope.launch { refreshUiState() }
    }

    fun onToDateSelected(date: LocalDate) {
        _toDate.value = date
        viewModelScope.launch { refreshUiState() }
    }

    /**
     * 执行课程调动操作。
     */
    fun moveCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            val state = _uiState.value

            if (state.selectedCourseTable == null || state.semesterStartDate == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = UiTextRes(Res.string.error_tweak_no_table_or_semester)
                    )
                }
                return@launch
            }

            if (state.fromDate == state.toDate) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = UiTextRes(Res.string.error_tweak_same_day)
                    )
                }
                return@launch
            }

            try {
                val courseConfig = appSettingsRepository.getCourseConfigOnce(state.selectedCourseTable.id)

                val fromWeek = appSettingsRepository.getWeekIndexAtDate(
                    targetDate = state.fromDate,
                    startDateStr = courseConfig?.semesterStartDate,
                    firstDayOfWeekInt = courseConfig?.firstDayOfWeek ?: 1
                ) ?: 1

                val fromDay = state.fromDate.dayOfWeek.ordinal + 1

                val toWeek = appSettingsRepository.getWeekIndexAtDate(
                    targetDate = state.toDate,
                    startDateStr = courseConfig?.semesterStartDate,
                    firstDayOfWeekInt = courseConfig?.firstDayOfWeek ?: 1
                ) ?: 1

                val toDay = state.toDate.dayOfWeek.ordinal + 1

                courseTableRepository.tweakCoursesOnDate(
                    mode = state.tweakMode, // 传入当前选中的模式
                    courseTableId = state.selectedCourseTable.id,
                    fromWeek = fromWeek,
                    fromDay = fromDay,
                    toWeek = toWeek,
                    toDay = toDay
                )

                // 操作成功后刷新预览
                refreshUiState()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = UiTextRes(Res.string.toast_tweak_success)
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = UiTextRes(Res.string.error_tweak_failed, listOf(e.message ?: ""))
                    )
                }
            }
        }
    }

    fun resetMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}