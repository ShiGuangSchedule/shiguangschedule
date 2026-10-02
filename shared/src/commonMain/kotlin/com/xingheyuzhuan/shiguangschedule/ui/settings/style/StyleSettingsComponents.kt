package com.xingheyuzhuan.shiguangschedule.ui.settings.style

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.BorderTypeProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.ui.components.AdvancedColorPicker
import com.xingheyuzhuan.shiguangschedule.ui.components.ColorPickerConfig
import com.xingheyuzhuan.shiguangschedule.ui.components.SliderWithInputField
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGrid
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridActions
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridStyleComposed
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridViewState
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.rememberScheduleGridState
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.MergedCourseBlock
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.WeeklyScheduleUiState
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.action_reset
import shiguangschedule.shared.generated.resources.action_reset_style
import shiguangschedule.shared.generated.resources.border_type_dashed
import shiguangschedule.shared.generated.resources.border_type_solid
import shiguangschedule.shared.generated.resources.check_24px
import shiguangschedule.shared.generated.resources.desc_wallpaper_set
import shiguangschedule.shared.generated.resources.desc_wallpaper_unset
import shiguangschedule.shared.generated.resources.dialog_reset_message
import shiguangschedule.shared.generated.resources.dialog_reset_title
import shiguangschedule.shared.generated.resources.format_week_display
import shiguangschedule.shared.generated.resources.image_24px
import shiguangschedule.shared.generated.resources.label_border_type
import shiguangschedule.shared.generated.resources.label_corner_radius
import shiguangschedule.shared.generated.resources.label_course_text_color
import shiguangschedule.shared.generated.resources.label_day_header_height
import shiguangschedule.shared.generated.resources.label_font_scale
import shiguangschedule.shared.generated.resources.label_hide_date_under_day
import shiguangschedule.shared.generated.resources.label_hide_grid_lines
import shiguangschedule.shared.generated.resources.label_hide_location
import shiguangschedule.shared.generated.resources.label_hide_section_time
import shiguangschedule.shared.generated.resources.label_hide_teacher
import shiguangschedule.shared.generated.resources.label_inner_padding
import shiguangschedule.shared.generated.resources.label_none
import shiguangschedule.shared.generated.resources.label_opacity
import shiguangschedule.shared.generated.resources.label_outer_padding
import shiguangschedule.shared.generated.resources.label_page_text_color
import shiguangschedule.shared.generated.resources.label_remove_location_at
import shiguangschedule.shared.generated.resources.label_schedule_mode_24h
import shiguangschedule.shared.generated.resources.label_section_height
import shiguangschedule.shared.generated.resources.label_show_start_time
import shiguangschedule.shared.generated.resources.label_text_align_center_h
import shiguangschedule.shared.generated.resources.label_text_align_center_v
import shiguangschedule.shared.generated.resources.label_time_column_width
import shiguangschedule.shared.generated.resources.label_wallpaper
import shiguangschedule.shared.generated.resources.preview_dark_mode
import shiguangschedule.shared.generated.resources.preview_light_mode
import shiguangschedule.shared.generated.resources.refresh_24px
import shiguangschedule.shared.generated.resources.status_not_set
import shiguangschedule.shared.generated.resources.style_category_color_scheme
import shiguangschedule.shared.generated.resources.style_category_course_block
import shiguangschedule.shared.generated.resources.style_category_grid_size
import shiguangschedule.shared.generated.resources.style_category_interface
import shiguangschedule.shared.generated.resources.style_category_other
import shiguangschedule.shared.generated.resources.title_dark_color_pool
import shiguangschedule.shared.generated.resources.title_light_color_pool
import shiguangschedule.shared.generated.resources.title_widget_style
import kotlin.time.Clock

@Composable
fun SettingsListContent(
    currentStyle: ScheduleGridStyleComposed,
    viewModel: StyleSettingsViewModel,
    isPreviewDark: Boolean,
    wallpaperPath: String? = null,
    onWallpaperClick: () -> Unit,
    onNavigate: (Destination) -> Unit,
    onPick: (isDark: Boolean, index: Int) -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(Res.string.dialog_reset_title)) },
            text = { Text(stringResource(Res.string.dialog_reset_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetStyleSettings()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(Res.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Text(stringResource(Res.string.action_reset_style))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // 其他样式分类
        Text(
            text = stringResource(Res.string.style_category_other),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // 小组件样式设置跳转入口
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onNavigate(Destination.WidgetStyleSettings) }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.title_widget_style),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(stringResource(Res.string.style_category_interface), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        WallpaperItem(
            wallpaperPath = wallpaperPath,
            onClick = onWallpaperClick,
            onLongClick = { viewModel.removeWallpaper(isPreviewDark) }
        )
        StyleSwitchItem(
            label = stringResource(Res.string.label_schedule_mode_24h),
            checked = currentStyle.scheduleMode == ScheduleModeProto.TIME_24H_MODE
        ) { isChecked ->
            val targetMode = if (isChecked) {
                ScheduleModeProto.TIME_24H_MODE
            } else {
                ScheduleModeProto.SECTION_MODE
            }
            viewModel.updateScheduleMode(targetMode)
        }
        StyleSwitchItem(stringResource(Res.string.label_hide_section_time), currentStyle.hideSectionTime) { viewModel.updateHideSectionTime(it) }
        StyleSwitchItem(stringResource(Res.string.label_hide_date_under_day), currentStyle.hideDateUnderDay) { viewModel.updateHideDateUnderDay(it) }
        StyleSwitchItem(label = stringResource(Res.string.label_hide_grid_lines), checked = currentStyle.hideGridLines) { viewModel.updateHideGridLines(it) }
        ColorPickerItem(
            label = stringResource(Res.string.label_page_text_color),
            currentColor = currentStyle.pageTextColor,
            onColorChanged = { viewModel.updatePageTextColor(it) },
            onReset = { viewModel.updatePageTextColor(null) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(stringResource(Res.string.style_category_grid_size), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        SliderWithInputField(stringResource(Res.string.label_section_height), currentStyle.sectionHeight.value, 40f..120f) { viewModel.updateSectionHeight(it) }
        SliderWithInputField(stringResource(Res.string.label_time_column_width), currentStyle.timeColumnWidth.value, 20f..80f) { viewModel.updateTimeColumnWidth(it) }
        SliderWithInputField(stringResource(Res.string.label_day_header_height), currentStyle.dayHeaderHeight.value, 30f..80f) { viewModel.updateDayHeaderHeight(it) }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(stringResource(Res.string.style_category_course_block), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        ColorPickerItem(
            label = stringResource(Res.string.label_course_text_color),
            currentColor = currentStyle.courseTextColor,
            onColorChanged = { viewModel.updateCourseTextColor(it) },
            onReset = { viewModel.updateCourseTextColor(null) }
        )
        StyleSwitchItem(stringResource(Res.string.label_show_start_time), currentStyle.showStartTime) { viewModel.updateShowStartTime(it) }
        StyleSwitchItem(stringResource(Res.string.label_hide_location), currentStyle.hideLocation) { viewModel.updateHideLocation(it) }
        StyleSwitchItem(stringResource(Res.string.label_hide_teacher), currentStyle.hideTeacher) { viewModel.updateHideTeacher(it) }
        StyleSwitchItem(stringResource(Res.string.label_remove_location_at), currentStyle.removeLocationAt) { viewModel.updateRemoveLocationAt(it) }
        StyleSwitchItem(stringResource(Res.string.label_text_align_center_h), currentStyle.textAlignCenterHorizontal) { viewModel.updateTextAlignCenterHorizontal(it) }
        StyleSwitchItem(stringResource(Res.string.label_text_align_center_v), currentStyle.textAlignCenterVertical) { viewModel.updateTextAlignCenterVertical(it) }
        BorderTypeSelector(currentStyle.borderType) { viewModel.updateBorderType(it) }

        SliderWithInputField(stringResource(Res.string.label_font_scale), currentStyle.fontScale, 0.5f..2.0f, 0.1f) { viewModel.updateCourseBlockFontScale(it) }
        SliderWithInputField(stringResource(Res.string.label_corner_radius), currentStyle.courseBlockCornerRadius.value, 0f..24f, 1f) { viewModel.updateCornerRadius(it) }
        SliderWithInputField(stringResource(Res.string.label_inner_padding), currentStyle.courseBlockInnerPadding.value, 0f..12f, 1f) { viewModel.updateInnerPadding(it) }
        SliderWithInputField(stringResource(Res.string.label_outer_padding), currentStyle.courseBlockOuterPadding.value, 0f..8f, 1f) { viewModel.updateOuterPadding(it) }
        SliderWithInputField(stringResource(Res.string.label_opacity), currentStyle.courseBlockAlpha, 0.1f..1f, 0.05f) { viewModel.updateAlpha(it) }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(stringResource(Res.string.style_category_color_scheme), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

        ColorSchemeSection(
            title = stringResource(Res.string.title_light_color_pool),
            bgColor = lightColorScheme().surfaceContainerLow,
            isDarkSection = false,
            colors = currentStyle.courseColorMaps.map { it.light },
            onEditColor = { onPick(false, it) }
        )

        ColorSchemeSection(
            title = stringResource(Res.string.title_dark_color_pool),
            bgColor = darkColorScheme().surfaceContainerLow,
            isDarkSection = true,
            colors = currentStyle.courseColorMaps.map { it.dark },
            onEditColor = { onPick(true, it) }
        )
    }
}

@Composable
fun BorderTypeSelector(
    currentType: BorderTypeProto,
    onTypeChange: (BorderTypeProto) -> Unit
) {
    val types = listOf(
        BorderTypeProto.BORDER_TYPE_NONE to stringResource(Res.string.label_none),
        BorderTypeProto.BORDER_TYPE_SOLID to stringResource(Res.string.border_type_solid),
        BorderTypeProto.BORDER_TYPE_DASHED to stringResource(Res.string.border_type_dashed)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.label_border_type), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            types.forEach { (type, label) ->
                val isSelected = currentType == type
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onTypeChange(type) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ColorSchemeSection(
    title: String,
    bgColor: Color,
    isDarkSection: Boolean,
    colors: List<Color>,
    onEditColor: (Int) -> Unit
) {
    val contentColor = if (isDarkSection) Color.White else Color.Black

    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bgColor).padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = contentColor)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            colors.forEachIndexed { index, color ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(color).clickable { onEditColor(index) })
                    Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(0.6f), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
fun ColorPreviewBox(color: Color, isLightModeUI: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(color), contentAlignment = Alignment.Center) {
        Text(
            text = if (isLightModeUI) stringResource(Res.string.preview_light_mode) else stringResource(Res.string.preview_dark_mode),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isLightModeUI) lightColorScheme().onSurface else Color.White
        )
    }
}

@Composable
fun ScheduleGridContent(
    style: ScheduleGridStyleComposed,
    demoUiState: WeeklyScheduleUiState,
    wallpaperPath: String? = null
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val localDates = remember(demoUiState.firstDayOfWeek) {
        val targetDayOfWeek = DayOfWeek.entries.getOrNull(demoUiState.firstDayOfWeek - 1) ?: DayOfWeek.MONDAY
        var startOfWeek = today
        while (startOfWeek.dayOfWeek != targetDayOfWeek) {
            startOfWeek = startOfWeek.minus(1, DateTimeUnit.DAY)
        }
        (0..6).map { startOfWeek.plus(it, DateTimeUnit.DAY) }
    }
    val currentYearString = remember(today) { today.year.toString() }
    val dummyDates = remember(localDates) {
        localDates.map {
            val month = it.month.number.toString().padStart(2, '0')
            val day = it.day.toString().padStart(2, '0')
            "$month/$day"
        }
    }
    val dynamicTodayIndex = remember(localDates) { localDates.indexOf(today) }
    val previewWeekStr = stringResource(Res.string.format_week_display, 1)
    val previewScrollState = rememberScrollState()
    val gridState = rememberScheduleGridState(gridScrollState = previewScrollState)
    val gridViewState = remember(dummyDates, currentYearString, demoUiState, dynamicTodayIndex, previewWeekStr) {
        ScheduleGridViewState(
            dates = dummyDates,
            currentYear = currentYearString,
            currentWeek = previewWeekStr,
            timeSlots = demoUiState.timeSlots,
            mergedCourses = demoUiState.currentMergedCourses,
            showWeekends = demoUiState.showWeekends,
            todayIndex = dynamicTodayIndex,
            firstDayOfWeek = demoUiState.firstDayOfWeek,
            currentSectionIndex = -1
        )
    }

    val gridActions = remember {
        object : ScheduleGridActions {
            override fun onCourseBlockClicked(block: MergedCourseBlock) {}
            override fun onGridCellClicked(day: Int, section: Int) {}
            override fun onTimeSlotClicked() {}
        }
    }

    val hasWallpaper = !wallpaperPath.isNullOrEmpty()

    val backgroundModifier = if (hasWallpaper) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    }

    Box(modifier = backgroundModifier) {
        if (hasWallpaper) {
            AsyncImage(
                model = wallpaperPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )
        }

        ScheduleGrid(
            state = gridState,
            viewState = gridViewState,
            actions = gridActions,
            style = style,
            modifier = Modifier
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scrollable(
                    orientation = Orientation.Vertical,
                    state = ScrollableState { delta ->
                        previewScrollState.dispatchRawDelta(-delta)
                        delta
                    },
                    flingBehavior = ScrollableDefaults.flingBehavior()
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {},
                        onTap = {}
                    )
                }
        )
    }
}

@Composable
fun StyleSwitchItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                { Icon(modifier = Modifier.size(SwitchDefaults.IconSize), imageVector = vectorResource(Res.drawable.check_24px), contentDescription = null) }
            } else null
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperItem(
    wallpaperPath: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val hasWallpaper = !wallpaperPath.isNullOrEmpty()

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(Res.string.label_wallpaper), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (hasWallpaper) stringResource(Res.string.desc_wallpaper_set)
                else stringResource(Res.string.desc_wallpaper_unset),
                style = MaterialTheme.typography.labelSmall,
                color = if (hasWallpaper) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = vectorResource(Res.drawable.image_24px),
            contentDescription = null,
            tint = if (hasWallpaper) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerItem(
    label: String,
    currentColor: Color?,
    onColorChanged: (Color) -> Unit,
    onReset: () -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showSheet = true }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)

        if (currentColor != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
            )
        } else {
            Text(
                text = stringResource(Res.string.status_not_set),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 40.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    TextButton(onClick = {
                        onReset()
                        showSheet = false
                    }) {
                        Icon(vectorResource(Res.drawable.refresh_24px), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.action_reset))
                    }
                }
                val pickerInitialColor = currentColor ?: MaterialTheme.colorScheme.primary

                AdvancedColorPicker(
                    initialColor = pickerInitialColor,
                    onColorChanged = onColorChanged,
                    config = ColorPickerConfig(
                        showAlpha = false,
                        showInputMode = true
                    )
                )
            }
        }
    }
}