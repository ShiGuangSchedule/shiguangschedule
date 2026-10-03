package com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.delete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.StringResource
import org.koin.core.annotation.KoinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.error_load_failed
import shiguangschedule.shared.generated.resources.error_op_failed
import shiguangschedule.shared.generated.resources.quick_delete_success

/**
 * 带有格式化参数的资源字符串包装类，避免硬编码
 */
data class UiTextRes(
    val resource: StringResource,
    val args: List<Any> = emptyList()
)

/**
 * 预览项包装类：明确这门课属于哪一周，用于 UI 平铺显示
 */
data class AffectedCourseItem(
    val courseWithWeeks: CourseWithWeeks,
    val targetWeek: Int
)

data class QuickDeleteUiState(
    val allCourseTables: List<CourseTable> = emptyList(),
    val selectedCourseTable: CourseTable? = null,

    // 筛选维度：周次和星期
    val selectedWeeks: Set<Int> = emptySet(),
    val selectedDays: Set<Int> = emptySet(),

    // 筛选维度：日期范围
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,

    // 预览数据
    val affectedCourses: List<AffectedCourseItem> = emptyList(),
    val semesterStartDate: LocalDate? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiTextRes? = null,
    val successMessage: UiTextRes? = null
)

@KoinViewModel
class QuickDeleteViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickDeleteUiState())
    val uiState: StateFlow<QuickDeleteUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // 获取当前设置和所有课表
                val settings = appSettingsRepository.getAppSettings().first()
                val allTables = courseTableRepository.getAllCourseTables().first()
                val selectedTable = allTables.find { it.id == settings.currentCourseTableId } ?: allTables.firstOrNull()

                // 解析当前课表的学期开始日期
                val courseConfig = selectedTable?.let { appSettingsRepository.getCourseConfigOnce(it.id) }
                val semesterStartDate = try {
                    courseConfig?.semesterStartDate?.let { LocalDate.parse(it) }
                } catch (_: Exception) { null }

                _uiState.update { it.copy(
                    allCourseTables = allTables,
                    selectedCourseTable = selectedTable,
                    semesterStartDate = semesterStartDate
                ) }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = UiTextRes(Res.string.error_load_failed)) }
            }
        }
    }

    // --- 筛选逻辑控制 ---

    fun toggleWeek(week: Int) {
        _uiState.update { s ->
            val new = if (s.selectedWeeks.contains(week)) s.selectedWeeks - week else s.selectedWeeks + week
            s.copy(selectedWeeks = new)
        }
        refreshPreview()
    }

    fun toggleDay(day: Int) {
        _uiState.update { s ->
            val new = if (s.selectedDays.contains(day)) s.selectedDays - day else s.selectedDays + day
            s.copy(selectedDays = new)
        }
        refreshPreview()
    }

    fun selectAllDays() {
        _uiState.update { it.copy(selectedDays = (1..7).toSet()) }
        refreshPreview()
    }

    fun clearAllDays() {
        _uiState.update { it.copy(selectedDays = emptySet()) }
        refreshPreview()
    }

    fun clearWeeksAndDays() {
        _uiState.update { it.copy(selectedWeeks = emptySet(), selectedDays = emptySet()) }
        refreshPreview()
    }

    fun setDateRange(start: LocalDate, end: LocalDate) {
        _uiState.update { it.copy(startDate = start, endDate = end) }
        refreshPreview()
    }

    fun clearDateRange() {
        _uiState.update { it.copy(startDate = null, endDate = null) }
        refreshPreview()
    }

    /**
     * 核心刷新预览：将多维度选择转换为 (周次, 星期) 的并集
     */
    private fun refreshPreview() {
        viewModelScope.launch {
            val state = _uiState.value
            val tableId = state.selectedCourseTable?.id ?: return@launch

            val noWeekDaySelection = state.selectedWeeks.isEmpty() || state.selectedDays.isEmpty()
            val noDateRangeSelection = state.startDate == null || state.endDate == null

            if (noWeekDaySelection && noDateRangeSelection) {
                _uiState.update { it.copy(affectedCourses = emptyList()) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            val queryPairs = mutableSetOf<Pair<Int, Int>>()

            // 获取当前课表的配置以对齐周次计算
            val courseConfig = appSettingsRepository.getCourseConfigOnce(tableId)

            // 处理：周次 x 星期
            if (state.selectedWeeks.isNotEmpty() && state.selectedDays.isNotEmpty()) {
                state.selectedWeeks.forEach { w ->
                    state.selectedDays.forEach { d -> queryPairs.add(w to d) }
                }
            }

            // 处理：日期范围转换
            state.startDate?.let { start ->
                state.endDate?.let { end ->
                    var curr: LocalDate = start
                    while (curr <= end) {
                        val w = appSettingsRepository.getWeekIndexAtDate(
                            targetDate = curr,
                            startDateStr = courseConfig?.semesterStartDate,
                            firstDayOfWeekInt = courseConfig?.firstDayOfWeek ?: 1
                        ) ?: 1

                        val d = curr.dayOfWeek.ordinal + 1
                        queryPairs.add(w to d)
                        curr = curr.plus(DatePeriod(days = 1))
                    }
                }
            }

            // 查询并平铺数据
            val flatList = mutableListOf<AffectedCourseItem>()
            queryPairs.forEach { (w, d) ->
                val coursesForDay = courseTableRepository.getCoursesForDay(tableId, w, d).first()
                coursesForDay.forEach { courseWithWeeks ->
                    flatList.add(AffectedCourseItem(courseWithWeeks, w))
                }
            }

            // 排序
            val sortedList = flatList.sortedWith(
                compareBy({ it.targetWeek }, { it.courseWithWeeks.course.day }, { it.courseWithWeeks.course.startSection })
            )

            _uiState.update { it.copy(affectedCourses = sortedList, isLoading = false) }
        }
    }

    /**
     * 执行删除逻辑
     */
    fun executeDelete() {
        viewModelScope.launch {
            val state = _uiState.value
            val currentAffected = state.affectedCourses
            val tableId = state.selectedCourseTable?.id ?: return@launch

            if (currentAffected.isEmpty()) return@launch

            try {
                _uiState.update { it.copy(isLoading = true) }

                // 提取唯一的时间坐标对 (周次, 星期)
                val pairs = currentAffected.map { it.targetWeek to it.courseWithWeeks.course.day }.distinct()

                // 调用 Repository 执行批量删除关联记录
                courseTableRepository.deleteCoursesOnDates(tableId, pairs)

                _uiState.update {
                    it.copy(
                        successMessage = UiTextRes(Res.string.quick_delete_success),
                        affectedCourses = emptyList(),
                        isLoading = false,
                        // 重置所有筛选状态
                        selectedWeeks = emptySet(),
                        selectedDays = emptySet(),
                        startDate = null,
                        endDate = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = UiTextRes(Res.string.error_op_failed, listOf(e.message ?: "")),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun resetMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}