package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.ExamEntry
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

private val WEEKDAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/**
 * 考试日程面板：把课表里的考试块单独列出来，按日期排序。
 *
 * 入口在课表页顶栏的「考试」按钮。数据来自当前课表的考试课程块，
 * 不依赖额外数据库字段（识别靠课程名前缀，见 ExamModels.kt）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScheduleSheet(
    entries: List<ExamEntry>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "考试日程",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (entries.isEmpty()) "" else "${entries.count { it.daysUntil >= 0 }} 场待考 / 共 ${entries.size} 场",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "来自当前课表里带「${"【考】"}」标记的课程块",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "当前课表还没有考试安排。\n重新执行一次「教务导入」并选择一起导入考试即可。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }

            entries.forEachIndexed { index, entry ->
                ExamRow(entry)
                if (index != entries.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamRow(entry: ExamEntry) {
    val isPast = entry.daysUntil < 0
    val isSoon = entry.daysUntil in 0..3
    val accent = when {
        isPast -> MaterialTheme.colorScheme.outline
        isSoon -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // 左侧日期块
        Column(
            modifier = Modifier
                .width(64.dp)
                .background(
                    color = accent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatMonthDay(entry.date),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = weekdayName(entry.date),
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = 0.85f)
            )
            Text(
                text = "第${entry.week}周",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = buildString {
                    if (entry.startTime.isNotBlank()) append("${entry.startTime}-${entry.endTime}")
                    if (entry.position.isNotBlank()) {
                        if (isNotEmpty()) append("  ·  ")
                        append(entry.position)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            entry.remark?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }

        Text(
            text = when {
                isPast -> "已结束"
                entry.daysUntil == 0 -> "今天"
                entry.daysUntil == 1 -> "明天"
                else -> "${entry.daysUntil} 天后"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (isPast) MaterialTheme.colorScheme.outline else accent,
            fontWeight = if (isSoon) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
        )
    }
}

private fun formatMonthDay(date: LocalDate): String = "${date.monthNumber}月${date.dayOfMonth}日"

private fun weekdayName(date: LocalDate): String =
    WEEKDAY_NAMES.getOrNull(date.dayOfWeek.isoDayNumber - 1) ?: ""
