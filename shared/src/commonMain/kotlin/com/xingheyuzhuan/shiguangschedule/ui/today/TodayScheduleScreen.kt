package com.xingheyuzhuan.shiguangschedule.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.course_position_prefix
import shiguangschedule.shared.generated.resources.course_teacher_prefix
import shiguangschedule.shared.generated.resources.date_format_year_month_day
import shiguangschedule.shared.generated.resources.label_credit
import shiguangschedule.shared.generated.resources.label_remark
import shiguangschedule.shared.generated.resources.status_semester_ended
import shiguangschedule.shared.generated.resources.text_no_courses_today
import shiguangschedule.shared.generated.resources.title_current_week
import shiguangschedule.shared.generated.resources.title_semester_not_set
import shiguangschedule.shared.generated.resources.title_today_schedule
import shiguangschedule.shared.generated.resources.title_vacation_until_start
import shiguangschedule.shared.generated.resources.week_days_full_names
import kotlin.time.Clock

private const val DEFAULT_TIME_ZERO = "00:00"
private const val EMPTY_TIME_PLACEHOLDER = "--:--"
private const val DEFAULT_DAYS_ZERO = "0"
private const val DEFAULT_OVERDUE_DAYS = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScheduleScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: TodayScheduleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gridStyle by viewModel.gridStyle.collectAsState()
    val isDark = LocalIsDarkTheme.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.title_today_schedule),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is TodayUiState.Loading -> { /* 可放置圆圈加载 */ }
                is TodayUiState.Success -> {
                    TodayContent(state, gridStyle, isDark)
                }
            }
        }
    }
}

@Composable
fun TodayContent(
    state: TodayUiState.Success,
    gridStyle: ScheduleGridStyle,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val currentTime = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time }

    val targetScrollIndex = remember(state.courses, currentTime) {
        val firstActiveIndex = state.courses.indexOfFirst { model ->
            try {
                LocalTime.parse(model.endTime ?: DEFAULT_TIME_ZERO) >= currentTime
            } catch (e: Exception) {
                true
            }
        }

        if (firstActiveIndex == -1) {
            (state.courses.size - 1).coerceAtLeast(0)
        } else {
            firstActiveIndex
        }
    }

    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = targetScrollIndex
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        val weekDays = stringArrayResource(Res.array.week_days_full_names)

        val year = state.today.year.toString()
        val month = state.today.month.number.toString()
        val day = state.today.day.toString()

        val formattedDate = stringResource(Res.string.date_format_year_month_day, year, month, day)

        val dateStr = remember(state.today, weekDays, formattedDate) {
            val weekDayName = weekDays.getOrNull(state.today.dayOfWeek.isoDayNumber - 1).orEmpty()
            "$formattedDate $weekDayName"
        }

        val subTitle = when (state.status) {
            TodayStatus.NoSemesterConfig -> stringResource(Res.string.title_semester_not_set)

            TodayStatus.Vacation -> {
                val days = if (state.startDate != null) {
                    state.today.daysUntil(state.startDate).toString()
                } else DEFAULT_DAYS_ZERO
                stringResource(Res.string.title_vacation_until_start, days)
            }

            TodayStatus.SemesterEnded -> {
                val overdueDays = if (state.startDate != null) {
                    val targetDayOfWeek = DayOfWeek(state.firstDayOfWeek)
                    val daysShift = (state.startDate.dayOfWeek.ordinal - targetDayOfWeek.ordinal + 7) % 7
                    val firstWeekStart = LocalDate.fromEpochDays(state.startDate.toEpochDays() - daysShift)

                    val semesterEndDate = LocalDate.fromEpochDays(
                        firstWeekStart.toEpochDays() + (state.totalWeeks * 7) - 1
                    )
                    semesterEndDate.daysUntil(state.today).coerceAtLeast(DEFAULT_OVERDUE_DAYS)
                } else {
                    DEFAULT_OVERDUE_DAYS
                }
                stringResource(Res.string.status_semester_ended, overdueDays)
            }

            TodayStatus.Normal -> stringResource(Res.string.title_current_week, state.weekIndex.toString())
        }

        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(text = dateStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.courses.isEmpty()) {
            EmptyStateView()
        } else {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    bottom = 88.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                itemsIndexed(state.courses) { _, model ->
                    CourseTimelineItem(model, gridStyle, isDark)
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun CourseTimelineItem(
    model: CourseDisplayModel,
    gridStyle: ScheduleGridStyle,
    isDark: Boolean
) {
    val currentTime = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time }
    val isFinished = remember(model.endTime, currentTime) {
        try {
            LocalTime.parse(model.endTime ?: DEFAULT_TIME_ZERO) < currentTime
        } catch (e: Exception) { false }
    }

    val colorPair = gridStyle.courseColorMaps.getOrElse(model.course.colorInt) {
        ScheduleGridStyle.DEFAULT_COLOR_MAPS[0]
    }
    val themeColor = if (isDark) colorPair.dark else colorPair.light

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (isFinished) 0.5f else 1f)
    ) {
        Column(
            modifier = Modifier.width(65.dp).padding(top = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = model.startTime ?: EMPTY_TIME_PLACEHOLDER,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    textDecoration = if (isFinished) TextDecoration.LineThrough else null
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = model.endTime ?: EMPTY_TIME_PLACEHOLDER,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = themeColor
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = if (isFinished) CardDefaults.cardElevation(defaultElevation = 0.dp)
                else CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = model.course.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (isFinished) TextDecoration.LineThrough else null
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (model.course.position.isNotBlank()) {
                        Text(
                            text = stringResource(Res.string.course_position_prefix, model.course.position),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (model.course.teacher.isNotBlank()) {
                        Text(
                            text = stringResource(Res.string.course_teacher_prefix, model.course.teacher),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val credit = model.course.credit
                    if (credit != null && credit > 0f) {
                        val creditValStr = if (credit % 1f == 0f) {
                            credit.toInt().toString()
                        } else {
                            credit.toString()
                        }
                        val creditLabel = stringResource(Res.string.label_credit)
                        Text(
                            text = "$creditLabel: $creditValStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            model.course.remark?.takeIf { it.isNotBlank() }?.let { remark ->
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, start = 4.dp)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(Res.string.label_remark),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = remark,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.text_no_courses_today),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}