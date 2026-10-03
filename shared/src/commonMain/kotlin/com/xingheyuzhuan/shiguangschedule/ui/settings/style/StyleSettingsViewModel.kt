package com.xingheyuzhuan.shiguangschedule.ui.settings.style

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.DualColor
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.BorderTypeProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridStyleComposed
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridStyleComposed.Companion.toComposedStyle
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.MergedCourseBlock
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.WeeklyScheduleUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@KoinViewModel
class StyleSettingsViewModel(
    private val styleRepository: StyleSettingsRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val fileSystem: FileSystem,
    @Named("FilesDir") private val filesDir: Path
) : ViewModel() {

    // 控制当前预览区域的深浅色模式
    private val _isPreviewDark = MutableStateFlow(false)
    val isPreviewDark: StateFlow<Boolean> = _isPreviewDark.asStateFlow()

    private var isPreviewInitialized = false

    /**
     * 初始化预览的深浅色模式（仅在首次进入页面时跟随软件全局主题）
     */
    fun initPreviewDark(isDark: Boolean) {
        if (!isPreviewInitialized) {
            isPreviewInitialized = true
            _isPreviewDark.value = isDark
        }
    }

    // 订阅应用设置状态
    val appSettingsState: StateFlow<AppSettingsModel?> = appSettingsRepository.getAppSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 订阅样式设置
    val styleState: StateFlow<ScheduleGridStyleComposed?> = styleRepository.styleFlow
        .map { it.toComposedStyle() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * 根据当前的 _isPreviewDark 状态，精确获取对应的深色或浅色壁纸路径字符串
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val wallpaperPathState: StateFlow<String?> = combine(
        appSettingsRepository.getAppSettings(),
        _isPreviewDark
    ) { settings, isDark ->
        val path = if (isDark) settings.backgroundImagePath.dark else settings.backgroundImagePath.light
        path.ifBlank { null }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val demoUiState: StateFlow<WeeklyScheduleUiState> = appSettingsRepository.getAppSettings()
        .flatMapLatest { settings ->
            val configFlow = settings.currentCourseTableId.let { tableId ->
                appSettingsRepository.getCourseTableConfigFlow(tableId)
            }

            combine(configFlow, styleRepository.styleFlow) { config, currentStyle ->
                val dummyTableId = Uuid.random().toString()
                val is24HourMode = currentStyle.scheduleMode == ScheduleModeProto.TIME_24H_MODE
                val todayDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

                WeeklyScheduleUiState(
                    style = currentStyle,
                    currentMergedCourses = createDemoCourses(dummyTableId, is24HourMode),
                    timeSlots = createDemoTimeSlots(dummyTableId),
                    showWeekends = config?.showWeekends ?: true,
                    totalWeeks = 20,
                    isSemesterSet = true,
                    semesterStartDate = todayDate,
                    firstDayOfWeek = 1,
                    currentWeekNumber = 1
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WeeklyScheduleUiState(showWeekends = true)
        )

    /**
     * 切换预览界面的深浅色模式
     */
    fun setPreviewDark(isDark: Boolean) {
        _isPreviewDark.value = isDark
    }


    /**
     * 保存裁切后的壁纸
     * @param imageBytes 图片字节数组
     * @param isDark true 代表写入深色模式壁纸，false 代表写入浅色模式壁纸
     */
    fun saveCroppedWallpaper(imageBytes: ByteArray, isDark: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val appSettings = appSettingsRepository.getAppSettingsOnce()
            val currentPath = if (isDark) appSettings.backgroundImagePath.dark else appSettings.backgroundImagePath.light

            if (currentPath.isNotEmpty()) {
                val oldPath = currentPath.toPath()
                if (fileSystem.exists(oldPath)) {
                    fileSystem.delete(oldPath)
                }
            }

            val prefix = if (isDark) "wallpaper_dark_" else "wallpaper_light_"
            val newFileName = "$prefix${Uuid.random()}.jpg"
            val newFile = filesDir / newFileName

            fileSystem.write(newFile) {
                write(imageBytes)
            }

            // 精准更新对应的背景路径字段
            val newBgPath = if (isDark) {
                appSettings.backgroundImagePath.copy(dark = newFile.toString())
            } else {
                appSettings.backgroundImagePath.copy(light = newFile.toString())
            }
            appSettingsRepository.insertOrUpdateAppSettings(appSettings.copy(backgroundImagePath = newBgPath))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 移除指定模式下的壁纸（深色或浅色独立清除）
     */
    fun removeWallpaper(isDark: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val appSettings = appSettingsRepository.getAppSettingsOnce()

            val targetPathStr = if (isDark) appSettings.backgroundImagePath.dark else appSettings.backgroundImagePath.light
            val otherPathStr = if (isDark) appSettings.backgroundImagePath.light else appSettings.backgroundImagePath.dark

            if (targetPathStr.isNotEmpty()) {
                val isSharedWithOther = targetPathStr == otherPathStr

                if (!isSharedWithOther) {
                    val path = targetPathStr.toPath()
                    if (fileSystem.exists(path)) {
                        fileSystem.delete(path)
                    }
                }
            }

            // 精准清空对应模式的路径
            val newBgPath = if (isDark) {
                appSettings.backgroundImagePath.copy(dark = "")
            } else {
                appSettings.backgroundImagePath.copy(light = "")
            }
            appSettingsRepository.insertOrUpdateAppSettings(appSettings.copy(backgroundImagePath = newBgPath))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 同时移除浅色和深色所有壁纸
     */
    fun removeAllWallpapers() = viewModelScope.launch(Dispatchers.IO) {
        removeWallpaper(isDark = false)
        removeWallpaper(isDark = true)
    }

    /**
     * 恢复默认设置 (但保留壁纸)
     */
    fun resetStyleSettings() = viewModelScope.launch {
        styleRepository.resetAllStyleSettingsExceptWallpaper()
    }

    /**
     * 彻底重置所有 (包括深浅色所有壁纸)
     */
    fun resetEverything() = viewModelScope.launch(Dispatchers.IO) {
        removeAllWallpapers()
        styleRepository.resetAllStyleSettings()
    }

    // --- 尺寸与边距 API ---
    fun updateSectionHeight(height: Float) = viewModelScope.launch { styleRepository.setSectionHeight(height) }
    fun updateTimeColumnWidth(width: Float) = viewModelScope.launch { styleRepository.setTimeColumnWidth(width) }

    /** 更新日表头高度 (DayHeader) */
    fun updateDayHeaderHeight(height: Float) = viewModelScope.launch {
        styleRepository.setDayHeaderHeight(height)
    }

    fun updateCornerRadius(radius: Float) = viewModelScope.launch { styleRepository.setCourseBlockCornerRadius(radius) }
    fun updateOuterPadding(padding: Float) = viewModelScope.launch { styleRepository.setCourseBlockOuterPadding(padding) }

    /** 更新课程块内部填充 (InnerPadding) */
    fun updateInnerPadding(padding: Float) = viewModelScope.launch {
        styleRepository.setCourseBlockInnerPadding(padding)
    }

    fun updateAlpha(alpha: Float) = viewModelScope.launch { styleRepository.setCourseBlockAlpha(alpha) }

    // --- UI 渲染开关 API ---

    /** 更新是否隐藏网格线 */
    fun updateHideGridLines(hide: Boolean) = viewModelScope.launch {
        styleRepository.setHideGridLines(hide)
    }

    /** 更新是否隐藏左侧时间列的具体时间 */
    fun updateHideSectionTime(hide: Boolean) = viewModelScope.launch {
        styleRepository.setHideSectionTime(hide)
    }

    /** 更新是否隐藏星期栏下的日期 */
    fun updateHideDateUnderDay(hide: Boolean) = viewModelScope.launch {
        styleRepository.setHideDateUnderDay(hide)
    }

    /** 更新是否在课程块内显示开始时间 */
    fun updateShowStartTime(show: Boolean) = viewModelScope.launch {
        styleRepository.setShowStartTime(show)
    }

    /** 更新课表时间段展示模式（传统节次模式 vs 24小时绝对时间轴模式） */
    fun updateScheduleMode(mode: ScheduleModeProto) = viewModelScope.launch {
        styleRepository.setScheduleMode(mode)
    }

    /** * 更新课程块字体的缩放比例
     * @param scale 缩放倍数，通常范围在 0.5 - 2.0 之间
     */
    fun updateCourseBlockFontScale(scale: Float) = viewModelScope.launch {
        styleRepository.setCourseBlockFontScale(scale)
    }

    /** 更新是否隐藏课程块内的上课地点 */
    fun updateHideLocation(hide: Boolean) = viewModelScope.launch {
        styleRepository.setHideLocation(hide)
    }

    /** 更新是否隐藏课程块内的授课老师 */
    fun updateHideTeacher(hide: Boolean) = viewModelScope.launch {
        styleRepository.setHideTeacher(hide)
    }

    /** 更新是否移除地点前的 "@" 符号 */
    fun updateRemoveLocationAt(remove: Boolean) = viewModelScope.launch {
        styleRepository.setRemoveLocationAt(remove)
    }

    /** 更新文字是否水平居中 */
    fun updateTextAlignCenterHorizontal(center: Boolean) = viewModelScope.launch {
        styleRepository.setTextAlignCenterHorizontal(center)
    }

    /** 更新文字是否垂直居中 */
    fun updateTextAlignCenterVertical(center: Boolean) = viewModelScope.launch {
        styleRepository.setTextAlignCenterVertical(center)
    }

    /** 更新课程块边框类型 (无/实线/虚线) */
    fun updateBorderType(type: BorderTypeProto) = viewModelScope.launch {
        styleRepository.setBorderType(type)
    }

    /** 更新页面网格文字颜色 */
    fun updatePageTextColor(color: Color?) = viewModelScope.launch {
        styleRepository.setPageTextColor(color)
    }

    /** 更新课程块内部文字颜色 */
    fun updateCourseTextColor(color: Color?) = viewModelScope.launch {
        styleRepository.setCourseTextColor(color)
    }

    /**
     * 更新普通课程的主色
     * @param index UI 传递过来的颜色索引
     * @param color 新颜色
     * @param isDark 是否为深色模式下的颜色
     */
    fun updatePrimaryColor(index: Int, color: Color, isDark: Boolean) = viewModelScope.launch {
        // 1. 获取当前 Repository 中最新的样式快照
        val currentStyle = styleRepository.getStyleOnce()
        // 2. 将 Proto 转出的 List 转换为 MutableList 以便修改
        val updatedMaps = currentStyle.courseColorMaps.toMutableList()

        // 3. 安全检查：如果索引越界（比如初始列表为空），则填充默认值
        if (index >= updatedMaps.size) {
            repeat(index - updatedMaps.size + 1) {
                updatedMaps.add(DualColor(light = Color.Gray, dark = Color.Gray))
            }
        }

        // 4. 更新对应索引位置的颜色
        val oldPair = updatedMaps[index]
        updatedMaps[index] = if (isDark) {
            oldPair.copy(dark = color)
        } else {
            oldPair.copy(light = color)
        }

        // 5. 调用 Repository 的 setCourseColorMaps 接口写回 DataStore
        styleRepository.setCourseColorMaps(updatedMaps)
    }

    private fun createDemoCourses(dummyTableId: String, is24HourMode: Boolean): List<MergedCourseBlock> {
        return if (is24HourMode) {
            listOf(
                // 24小时绝对时间模式：全部是自定义时间（自由时间），需要携带具体的时间字符串
                createBlock(
                    tableId = dummyTableId, day = 1, start = 0.0f, end = 1.75f, name = "深夜研讨",
                    startSection = null, endSection = null,
                    isCustomTime = true, customStartTime = "00:00", customEndTime = "00:45",
                    colorInt = 0
                ),
                createBlock(
                    tableId = dummyTableId, day = 2, start = 0.5f, end = 2.0f, name = "凌晨补录",
                    startSection = null, endSection = null,
                    isCustomTime = true, customStartTime = "01:00", customEndTime = "02:00",
                    colorInt = 1
                )
            )
        } else {
            listOf(
                // 传统节次模式 1：标准节次课程（对应第1节到第2节），isCustomTime 为 false，不传时间字符串
                createBlock(
                    tableId = dummyTableId, day = 1, start = 0f, end = 2f, name = "普通课程展示",
                    startSection = 1, endSection = 2,
                    isCustomTime = false, customStartTime = null, customEndTime = null,
                    colorInt = 0
                ),
                // 传统节次模式 2：精准自由时间演示（在传统表格里按绝对时间乱飞的非标准课程），isCustomTime 为 true
                createBlock(
                    tableId = dummyTableId, day = 2, start = 0.618f, end = 2.5f, name = "精准渲染演示",
                    startSection = null, endSection = null,
                    isCustomTime = true, customStartTime = "09:15", customEndTime = "11:05",
                    colorInt = 1
                ),
                // 传统节次模式 3：冲突课程 A（标准节次课程，对应第1节到第3节）
                createBlock(
                    tableId = dummyTableId, day = 3, start = 0f, end = 3f, name = "冲突课程 A",
                    startSection = 1, endSection = 3,
                    isCustomTime = false, customStartTime = null, customEndTime = null,
                    subColumn = 0f, totalColumns = 2f,
                    colorInt = 2
                ),
                // 传统节次模式 4：冲突课程 B（标准节次课程，对应第2节到第3节，非当前周降级显示）
                createBlock(
                    tableId = dummyTableId, day = 3, start = 1f, end = 3f, name = "冲突课程 B",
                    startSection = 2, endSection = 3,
                    isCustomTime = false, customStartTime = null, customEndTime = null,
                    subColumn = 1f, totalColumns = 2f,
                    isNonCurrentWeek = true,
                    colorInt = 3
                )
            )
        }
    }

    private fun createBlock(
        tableId: String,
        day: Int,
        start: Float,
        end: Float,
        name: String,
        startSection: Int?,
        endSection: Int?,
        isCustomTime: Boolean,
        customStartTime: String?,
        customEndTime: String?,
        subColumn: Float = 0f,
        totalColumns: Float = 1f,
        isNonCurrentWeek: Boolean = false,
        colorInt: Int
    ): MergedCourseBlock {
        val course = Course(
            id = Uuid.random().toString(),
            courseTableId = tableId,
            name = name,
            teacher = "老师",
            position = "地点",
            day = day,
            startSection = startSection,
            endSection = endSection,
            isCustomTime = isCustomTime,
            customStartTime = customStartTime,
            customEndTime = customEndTime,
            colorInt = colorInt,
            remark = ""
        )

        return MergedCourseBlock(
            day = day,
            startSection = start,
            endSection = end,
            courses = listOf(CourseWithWeeks(course, emptyList())),
            needsProportionalRendering = isCustomTime,
            isVisualDemoted = isNonCurrentWeek,
            nonActiveRanges = listOf(subColumn to totalColumns)
        )
    }

    private fun createDemoTimeSlots(dummyTableId: String): List<TimeSlot> = listOf(
        TimeSlot(timeTableId = dummyTableId, number = 1, startTime = "08:20", endTime = "09:05"),
        TimeSlot(timeTableId = dummyTableId, number = 2, startTime = "09:15", endTime = "10:00"),
        TimeSlot(timeTableId = dummyTableId, number = 3, startTime = "10:20", endTime = "11:05"),
        TimeSlot(timeTableId = dummyTableId, number = 4, startTime = "11:15", endTime = "12:00"),
        TimeSlot(timeTableId = dummyTableId, number = 5, startTime = "14:00", endTime = "14:45"),
        TimeSlot(timeTableId = dummyTableId, number = 6, startTime = "14:55", endTime = "15:40"),
        TimeSlot(timeTableId = dummyTableId, number = 7, startTime = "16:00", endTime = "16:45"),
        TimeSlot(timeTableId = dummyTableId, number = 8, startTime = "16:55", endTime = "17:40")
    )
}