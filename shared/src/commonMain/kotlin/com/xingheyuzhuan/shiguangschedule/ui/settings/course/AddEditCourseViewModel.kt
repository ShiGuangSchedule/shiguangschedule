package com.xingheyuzhuan.shiguangschedule.ui.settings.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.DualColor
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeScheduleRepository
import com.xingheyuzhuan.shiguangschedule.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedule.navigation.PresetCourseData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.uuid.Uuid

data class CourseScheme(
    val id: String = Uuid.random().toString(),
    val dbId: String? = null,
    val teacher: String = "",
    val position: String = "",
    val credit: String = "",
    val remark: String = "",
    val day: Int = 1,
    val startSection: Int = 1,
    val endSection: Int = 1,
    val isCustomTime: Boolean = false,
    val customStartTime: String = "08:00",
    val customEndTime: String = "09:35",
    val weeks: Set<Int> = emptySet(),
    val colorIndex: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class AddEditCourseViewModel(
    private val courseTableRepository: CourseTableRepository,
    private val timeScheduleRepository: TimeScheduleRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val styleSettingsRepository: StyleSettingsRepository
) : ViewModel() {

    private var _courseId: String? = null
    private val courseId: String? get() = _courseId

    private val _uiState = MutableStateFlow(AddEditCourseUiState())
    val uiState: StateFlow<AddEditCourseUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    private var originalDbIds = setOf<String>()
    private var initialName: String = ""
    private var initialSchemes: List<CourseScheme> = emptyList()

    fun initWithId(id: String?) {
        if (_uiState.value.isDataLoaded) return

        _courseId = id
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val initialPresetData: PresetCourseData? = if (courseId == null) {
                try { AddEditCourseChannel.presetDataFlow.first() } catch (_: Exception) { null }
            } else { null }

            val appSettingsFlow = appSettingsRepository.getAppSettings()
            val styleFlow = styleSettingsRepository.styleFlow
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

            val timeSlotsFlow = appSettingsFlow.flatMapLatest { settings ->
                val tid = settings.currentCourseTableId
                timeScheduleRepository.observeEffectiveTimeSlots(tid, today)
            }

            val courseConfigFlow = appSettingsFlow.flatMapLatest { settings ->
                val tid = settings.currentCourseTableId
                appSettingsRepository.getCourseTableConfigFlow(tid)
            }

            combine(
                timeSlotsFlow,
                appSettingsFlow,
                courseConfigFlow,
                styleFlow,
                if (courseId != null) {
                    appSettingsFlow.flatMapLatest { settings ->
                        courseTableRepository.getCoursesWithWeeksByTableId(settings.currentCourseTableId)
                            .flatMapLatest { all ->
                                val current = all.find { it.course.id == courseId }
                                if (current != null) {
                                    flowOf(all.filter { it.course.name == current.course.name })
                                } else {
                                    flowOf(emptyList())
                                }
                            }
                    }
                } else {
                    flowOf(emptyList())
                }
            ) { timeSlots, appSettings, courseConfig, currentStyle, relatedCourseWithWeeks ->
                _uiState.update { currentState ->
                    val totalWeeks = courseConfig?.semesterTotalWeeks ?: 20
                    val currentColorMaps = currentStyle.courseColorMaps

                    if (currentState.schemes.isEmpty() && !currentState.isDataLoaded) {
                        // 仅在初次加载数据时进行排序，确保进入页面时列表是整齐的
                        val schemes = if (courseId == null) {
                            val newColor = currentStyle.generateRandomColorIndex()
                            listOf(
                                CourseScheme(
                                    teacher = initialPresetData?.teacher.orEmpty(),
                                    position = initialPresetData?.position.orEmpty(),
                                    credit = initialPresetData?.credit?.toString().orEmpty(),
                                    remark = initialPresetData?.remark.orEmpty(),
                                    day = initialPresetData?.day ?: 1,
                                    startSection = initialPresetData?.startSection ?: 1,
                                    endSection = initialPresetData?.endSection ?: 1,
                                    isCustomTime = initialPresetData?.isCustomTime ?: false,
                                    customStartTime = initialPresetData?.customStartTime ?: "08:00",
                                    customEndTime = initialPresetData?.customEndTime ?: "09:35",
                                    weeks = initialPresetData?.presetWeeks ?: (1..totalWeeks).toSet(),
                                    colorIndex = initialPresetData?.colorIndex ?: newColor
                                )
                            )
                        } else {
                            originalDbIds = relatedCourseWithWeeks.map { it.course.id }.toSet()
                            relatedCourseWithWeeks.map { cw ->
                                CourseScheme(
                                    id = cw.course.id,
                                    dbId = cw.course.id,
                                    teacher = cw.course.teacher,
                                    position = cw.course.position,
                                    credit = cw.course.credit?.toString().orEmpty(), // 新增：读取已有学分
                                    remark = cw.course.remark.orEmpty(),
                                    day = cw.course.day,
                                    startSection = cw.course.startSection ?: 1,
                                    endSection = cw.course.endSection ?: 1,
                                    isCustomTime = cw.course.isCustomTime,
                                    customStartTime = cw.course.customStartTime.orEmpty(),
                                    customEndTime = cw.course.customEndTime.orEmpty(),
                                    weeks = cw.weeks.map { it.weekNumber }.toSet(),
                                    colorIndex = cw.course.colorInt.coerceIn(0, currentColorMaps.size - 1)
                                )
                            }.sortedWith(schemeComparator())
                        }

                        // 保存备份
                        initialName = initialPresetData?.name ?: relatedCourseWithWeeks.firstOrNull()?.course?.name.orEmpty()
                        initialSchemes = schemes

                        currentState.copy(
                            isEditing = courseId != null,
                            isDataLoaded = true,
                            name = initialName,
                            schemes = schemes,
                            timeSlots = timeSlots,
                            currentCourseTableId = appSettings.currentCourseTableId,
                            semesterTotalWeeks = totalWeeks,
                            courseColorMaps = currentColorMaps
                        )
                    } else {
                        currentState.copy(
                            timeSlots = timeSlots,
                            semesterTotalWeeks = totalWeeks,
                            courseColorMaps = currentColorMaps
                        )
                    }
                }
            }.collect()
        }
    }

    fun onNameChange(name: String) { _uiState.update { it.copy(name = name) } }

    /**
     * 判断是否有未保存的内容变更
     */
    fun hasUnsavedChanges(): Boolean {
        val state = uiState.value
        if (!state.isDataLoaded) return false
        return state.name != initialName || state.schemes != initialSchemes
    }

    /**
     * 直接追加到末尾，不触发自动重排，方便用户立即编辑
     */
    fun addScheme() {
        _uiState.update { state ->
            val lastScheme = state.schemes.lastOrNull()
            val newScheme = CourseScheme(
                teacher = lastScheme?.teacher.orEmpty(),
                position = lastScheme?.position.orEmpty(),
                credit = lastScheme?.credit.orEmpty(),
                remark = lastScheme?.remark.orEmpty(),
                colorIndex = lastScheme?.colorIndex ?: 0,
                weeks = (1..state.semesterTotalWeeks).toSet()
            )
            state.copy(schemes = state.schemes + newScheme)
        }
    }

    fun removeScheme(schemeId: String) {
        _uiState.update { state ->
            if (state.schemes.size <= 1) return@update state
            state.copy(schemes = state.schemes.filter { it.id != schemeId })
        }
    }

    fun updateScheme(schemeId: String, transform: (CourseScheme) -> CourseScheme) {
        _uiState.update { state ->
            state.copy(schemes = state.schemes.map {
                if (it.id == schemeId) transform(it) else it
            })
        }
    }

    /**
     * 将当前页面中的所有课程方案统一设置为指定颜色索引
     */
    fun updateAllSchemesColor(colorIndex: Int) {
        _uiState.update { state ->
            state.copy(
                schemes = state.schemes.map { it.copy(colorIndex = colorIndex) }
            )
        }
    }

    fun onSchemeCreditChange(schemeId: String, credit: String) {
        if (credit.isEmpty() || credit.matches(Regex("^\\d*\\.?\\d*$"))) {
            updateScheme(schemeId) { it.copy(credit = credit) }
        }
    }

    fun onSchemeRemarkChange(schemeId: String, remark: String) {
        if (remark.length <= 300) {
            updateScheme(schemeId) { it.copy(remark = remark) }
        }
    }

    /**
     * 切换自定义时间
     */
    fun toggleCustomTime(schemeId: String, isCustom: Boolean) {
        updateScheme(schemeId) { scheme ->
            scheme.copy(
                isCustomTime = isCustom,
                customStartTime = if (isCustom && scheme.customStartTime.isBlank()) "08:00" else scheme.customStartTime,
                customEndTime = if (isCustom && scheme.customEndTime.isBlank()) "09:35" else scheme.customEndTime
            )
        }
    }

    /**
     * 主动排序
     */
    fun requestSort() {
        _uiState.update { state ->
            state.copy(schemes = state.schemes.sortedWith(schemeComparator()))
        }
    }

    fun onSave() {
        viewModelScope.launch {
            val state = uiState.value
            if (state.name.isBlank()) return@launch

            val tableId = state.currentCourseTableId.orEmpty()
            val currentSchemeDbIds = state.schemes.mapNotNull { it.dbId }.toSet()

            (originalDbIds - currentSchemeDbIds).forEach { idToRemove ->
                courseTableRepository.deleteCourse(createEmptyCourseForDelete(idToRemove, tableId))
            }

            state.schemes.forEach { scheme ->
                val course = Course(
                    id = scheme.dbId ?: Uuid.random().toString(),
                    courseTableId = tableId,
                    name = state.name,
                    teacher = scheme.teacher,
                    position = scheme.position,
                    credit = scheme.credit.toFloatOrNull(),
                    remark = scheme.remark,
                    day = scheme.day,
                    startSection = if (scheme.isCustomTime) null else scheme.startSection,
                    endSection = if (scheme.isCustomTime) null else scheme.endSection,
                    isCustomTime = scheme.isCustomTime,
                    customStartTime = if (scheme.isCustomTime) scheme.customStartTime else null,
                    customEndTime = if (scheme.isCustomTime) scheme.customEndTime else null,
                    colorInt = scheme.colorIndex
                )
                courseTableRepository.upsertCourse(course, scheme.weeks.toList())
            }
            _uiEvent.send(UiEvent.SaveSuccess)
        }
    }

    fun onDelete() {
        viewModelScope.launch {
            val tableId = uiState.value.currentCourseTableId.orEmpty()
            originalDbIds.forEach { id ->
                courseTableRepository.deleteCourse(createEmptyCourseForDelete(id, tableId))
            }
            _uiEvent.send(UiEvent.DeleteSuccess)
        }
    }

    fun onCancel() {
        viewModelScope.launch { _uiEvent.send(UiEvent.Cancel) }
    }

    private fun createEmptyCourseForDelete(id: String, tableId: String) = Course(
        id = id,
        courseTableId = tableId,
        name = "",
        teacher = "",
        position = "",
        credit = null,
        remark = null,
        day = 1,
        startSection = null,
        endSection = null,
        isCustomTime = false,
        customStartTime = null,
        customEndTime = null,
        colorInt = 0
    )

    private fun schemeComparator() = compareBy<CourseScheme>(
        { it.day },
        { if (it.isCustomTime) it.customStartTime else it.startSection.toString().padStart(2, '0') }
    )
}

sealed interface UiEvent {
    data object SaveSuccess : UiEvent
    data object DeleteSuccess : UiEvent
    data object Cancel : UiEvent
}

data class AddEditCourseUiState(
    val isEditing: Boolean = false,
    val isDataLoaded: Boolean = false,
    val name: String = "",
    val schemes: List<CourseScheme> = emptyList(),
    val timeSlots: List<TimeSlot> = emptyList(),
    val currentCourseTableId: String? = null,
    val semesterTotalWeeks: Int = 20,
    val courseColorMaps: List<DualColor> = emptyList()
)