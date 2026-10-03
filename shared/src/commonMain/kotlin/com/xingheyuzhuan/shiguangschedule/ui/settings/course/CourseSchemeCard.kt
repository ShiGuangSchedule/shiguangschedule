package com.xingheyuzhuan.shiguangschedule.ui.settings.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.DualColor
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.award_star_24px
import shiguangschedule.shared.generated.resources.credit_error_invalid
import shiguangschedule.shared.generated.resources.credit_support_format
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.label_credit
import shiguangschedule.shared.generated.resources.label_custom_time
import shiguangschedule.shared.generated.resources.label_day_of_week
import shiguangschedule.shared.generated.resources.label_position
import shiguangschedule.shared.generated.resources.label_remark
import shiguangschedule.shared.generated.resources.label_section_range_suffix
import shiguangschedule.shared.generated.resources.label_teacher
import shiguangschedule.shared.generated.resources.location_on_24px
import shiguangschedule.shared.generated.resources.person_24px
import shiguangschedule.shared.generated.resources.sticky_note_2_24px
import shiguangschedule.shared.generated.resources.week_days_full_names

/**
 * 课程方案卡片
 * 负责展示单个课程的时间、地点、老师等具体安排
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseSchemeCard(
    scheme: CourseScheme,
    courseColorMaps: List<DualColor>,
    timeSlots: List<TimeSlot>,
    onTeacherChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onCreditChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onColorClick: () -> Unit,
    onTimeClick: () -> Unit,
    onWeeksClick: () -> Unit,
    onDayClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onToggleCustomTime: (Boolean) -> Unit,
    showRemoveButton: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {

            // 左侧颜色指示器块
            ColorIndicatorSection(
                colorIndex = scheme.colorIndex,
                colorMaps = courseColorMaps,
                onClick = onColorClick
            )

            // 右侧内容区
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(vectorResource(Res.drawable.person_24px), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    TextField(
                        value = scheme.teacher,
                        onValueChange = onTeacherChange,
                        placeholder = { Text(stringResource(Res.string.label_teacher)) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Text(stringResource(Res.string.label_custom_time), style = MaterialTheme.typography.labelSmall)
                    Switch(
                        checked = scheme.isCustomTime,
                        onCheckedChange = onToggleCustomTime,
                        modifier = Modifier.scale(0.7f)
                    )

                    if (showRemoveButton) {
                        IconButton(onClick = onRemoveClick) {
                            Icon(vectorResource(Res.drawable.delete_24px), null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // 地点输入
                OutlinedTextField(
                    value = scheme.position,
                    onValueChange = onPositionChange,
                    placeholder = { Text(stringResource(Res.string.label_position)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(vectorResource(Res.drawable.location_on_24px), null, Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 学分输入框
                OutlinedTextField(
                    value = scheme.credit,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() || it == '.' } && newValue.count { it == '.' } <= 1)) {
                            onCreditChange(newValue)
                        }
                    },
                    placeholder = { Text(stringResource(Res.string.label_credit)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(vectorResource(Res.drawable.award_star_24px), null, Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = scheme.credit.endsWith("."),
                    supportingText = {
                        if (scheme.credit.endsWith(".")) {
                            Text(stringResource(Res.string.credit_error_invalid))
                        } else {
                            Text(stringResource(Res.string.credit_support_format))
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 备注输入
                OutlinedTextField(
                    value = scheme.remark,
                    onValueChange = onRemarkChange,
                    placeholder = { Text(stringResource(Res.string.label_remark)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp, max = 140.dp),
                    leadingIcon = { Icon(imageVector = vectorResource(Res.drawable.sticky_note_2_24px), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    minLines = 1,
                    maxLines = 5,
                    supportingText = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${scheme.remark.length} / 300",
                                modifier = Modifier.align(Alignment.CenterEnd),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 底部区域：时间与周次
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 时间显示逻辑
                    if (scheme.isCustomTime) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val dayNames = stringArrayResource(Res.array.week_days_full_names)
                            TimeSection(
                                dayName = stringResource(Res.string.label_day_of_week),
                                timeDesc = dayNames.getOrNull(scheme.day - 1) ?: "",
                                onClick = onDayClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                            TimeSection(
                                dayName = stringResource(Res.string.label_custom_time),
                                timeDesc = "${scheme.customStartTime.ifBlank { "00:00" }}-${scheme.customEndTime.ifBlank { "00:00" }}",
                                onClick = onTimeClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        val dayNames = stringArrayResource(Res.array.week_days_full_names)
                        val startAlias = timeSlots.find { it.number == scheme.startSection }?.alias
                        val endAlias = timeSlots.find { it.number == scheme.endSection }?.alias
                        val startDisplay = startAlias ?: scheme.startSection.toString()
                        val endDisplay = endAlias ?: scheme.endSection.toString()

                        val sectionRangeSuffix = stringResource(Res.string.label_section_range_suffix)
                        val timeDesc = if (startDisplay == endDisplay) {
                            "$startDisplay $sectionRangeSuffix"
                        } else {
                            "$startDisplay-$endDisplay $sectionRangeSuffix"
                        }

                        TimeSection(
                            dayName = dayNames.getOrNull(scheme.day - 1) ?: "",
                            timeDesc = timeDesc,
                            onClick = onTimeClick,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }

                    // 周次展示（展示完整周次，撑起整个卡片高度）
                    WeekSection(
                        selectedWeeks = scheme.weeks,
                        onClick = onWeeksClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}