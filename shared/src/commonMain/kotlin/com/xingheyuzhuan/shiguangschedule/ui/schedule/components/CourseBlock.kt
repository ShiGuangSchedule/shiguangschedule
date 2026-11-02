package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.BorderTypeProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.ui.theme.rememberCourseTextColors
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_stacked_hidden_courses

@Composable
fun CourseBlock(
    courseWrapper: CourseWithWeeks,
    isVisualDemoted: Boolean,
    style: ScheduleGridStyleComposed,
    timeSlots: List<TimeSlot>,
    modifier: Modifier = Modifier,
    isFloating: Boolean = false, // 标记当前块是否处于长按选中/悬浮状态
    stackCount: Int = 1,
    conflictCount: Int = stackCount, // 本周实际冲突课程数，不含非本周课程
    isStackTop: Boolean = false,
    onStackBadgeClick: (() -> Unit)? = null
) {
    val course = courseWrapper.course
    val isDarkTheme = LocalIsDarkTheme.current

    // 颜色适配
    val colorIndex = course.colorInt.takeIf { it in style.courseColorMaps.indices }
    val courseColorAdapted: Color? = colorIndex?.let { index ->
        val baseColorMap = style.courseColorMaps[index]
        if (isDarkTheme) baseColorMap.dark else baseColorMap.light
    }
    val fallbackColorAdapted: Color = if (isDarkTheme) style.courseColorMaps.first().dark else style.courseColorMaps.first().light

    val currentAlpha = if (isFloating) 0.95f else style.courseBlockAlpha
    val blockColor = (courseColorAdapted ?: fallbackColorAdapted).copy(alpha = currentAlpha)
    val effectiveBackground = blockColor.compositeOver(MaterialTheme.colorScheme.surface)
    val textColors = rememberCourseTextColors(
        background = effectiveBackground,
        mode = style.courseTextColorMode,
        globalColor = style.courseTextColor,
        tintStrength = style.courseTextTintStrength,
        contrastTarget = style.courseTextContrastRatio,
        sourceColor = courseColorAdapted ?: fallbackColorAdapted
    )
    val textColor = textColors.primary
    val secondaryTextColor = textColors.secondary

    // 字体大小保持原设计
    val s13 = (13 * style.fontScale).sp
    val s10 = (10 * style.fontScale).sp

    // 核心分支逻辑：判断 24小时模式 与 节次模式 的时间文本渲染
    val customStartTime = course.customStartTime
    val customEndTime = course.customEndTime
    val customTimeString = if (customStartTime != null && customEndTime != null) "$customStartTime - $customEndTime" else null
    val isCustomTimeCourse = customTimeString != null

    val timeTextToShow = if (style.scheduleMode == ScheduleModeProto.TIME_24H_MODE) {
        // 24小时绝对时间轴模式：全部课程都显示起止时间
        if (isCustomTimeCourse) {
            customTimeString
        } else {
            val startSlot = timeSlots.find { it.number == course.startSection }
            val endSlot = timeSlots.find { it.number == course.endSection }
            if (startSlot != null && endSlot != null) "${startSlot.startTime} - ${endSlot.endTime}" else null
        }
    } else {
        // 传统节次模式：只有自定义课程显示起止时间；普通节次课程只有在开启展示开始时间时才显示开始时间
        if (isCustomTimeCourse) {
            customTimeString
        } else if (style.showStartTime) {
            timeSlots.find { it.number == course.startSection }?.startTime
        } else {
            null
        }
    }

    // 边框样式配置
    val borderColor = if (isFloating) Color(0xFF2196F3) else MaterialTheme.colorScheme.outline
    val borderWidth = if (isFloating) 2.dp else 1.dp
    val borderAlpha = if (isFloating) 1.0f else style.courseBlockAlpha
    val shape = RoundedCornerShape(style.courseBlockCornerRadius)

    val borderModifier = when (style.borderType) {
        BorderTypeProto.BORDER_TYPE_SOLID -> {
            Modifier.border(borderWidth, borderColor.copy(alpha = borderAlpha), shape)
        }
        BorderTypeProto.BORDER_TYPE_DASHED -> {
            Modifier.drawBehind {
                val strokeWidth = borderWidth.toPx()
                val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = borderColor.copy(alpha = borderAlpha),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, pathEffect = dashPathEffect)
                )
            }
        }
        else -> {
            if (isFloating) Modifier.border(borderWidth, borderColor, shape) else Modifier
        }
    }

    val horizontalAlignment = if (style.textAlignCenterHorizontal) Alignment.CenterHorizontally else Alignment.Start
    val verticalArrangement = if (style.textAlignCenterVertical) Arrangement.Center else Arrangement.Top
    val textAlign = if (style.textAlignCenterHorizontal) TextAlign.Center else TextAlign.Start

    val showStackBadge = isStackTop && conflictCount > 1
    val showCourseContent = stackCount <= 1 || isStackTop || isFloating
    // 角标只占用顶部横带，避免压缩文字宽度导致窄列逐字竖排
    val badgeTopPadding = if (showStackBadge) 16.dp else 0.dp

    // 选中捏起时，增加三维物理阴影
    val floatingShadowModifier = if (isFloating) {
        Modifier.shadow(elevation = 8.dp, shape = shape, clip = false)
    } else {
        Modifier
    }

    // 堆叠格先铺一层不透明底色，避免课程块透明度把下层课程透出来
    val stackOpacityBacking = if (stackCount > 1 && (isStackTop || isFloating)) {
        Modifier.background(MaterialTheme.colorScheme.surface)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(floatingShadowModifier)
            .fillMaxSize()
            .then(borderModifier)
            .clip(shape)
            .then(stackOpacityBacking)
            .background(color = blockColor)
    ) {
        if (showCourseContent) {
            // 课程文字内容容器
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(style.courseBlockInnerPadding)
                    .padding(top = badgeTopPadding),
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = verticalArrangement
            ) {
                if (timeTextToShow != null) {
                    Text(
                        text = timeTextToShow,
                        fontSize = s10,
                        color = secondaryTextColor,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = textAlign,
                        style = TextStyle(lineHeight = 1.em)
                    )
                }

                Text(
                    text = course.name,
                    fontSize = s13,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = textAlign,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    style = TextStyle(lineHeight = 1.2.em)
                )

                if (!style.hideTeacher) {
                    val teacher = course.teacher
                    if (teacher.isNotBlank()) {
                        Text(text = teacher, fontSize = s10, color = textColor, textAlign = textAlign, overflow = TextOverflow.Ellipsis, style = TextStyle(lineHeight = 1.em))
                    }
                }

                if (!style.hideLocation) {
                    val position = course.position
                    if (position.isNotBlank()) {
                        val prefix = if (style.removeLocationAt) "" else "@"
                        Text(text = "$prefix$position", fontSize = s10, color = textColor, textAlign = textAlign, overflow = TextOverflow.Ellipsis, style = TextStyle(lineHeight = 1.em))
                    }
                }
            }
        }

        // 当单课不是当前周时，进行干净的全局遮罩染色与虚化斜线绘制
        if (isVisualDemoted && !isFloating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = (if (isDarkTheme) Color.Black else Color.White).copy(alpha = 0.618f))
                    .drawBehind {
                        val stripeWidth = 5.dp.toPx()
                        val stripeColor = (if (isDarkTheme) Color.White else Color.Black).copy(alpha = 0.06f)
                        val brush = Brush.linearGradient(
                            0.0f to stripeColor, 0.45f to stripeColor,
                            0.55f to Color.Transparent, 1.0f to Color.Transparent,
                            start = Offset(0f, 0f), end = Offset(stripeWidth, stripeWidth), tileMode = TileMode.Repeated
                        )
                        drawRect(brush = brush)
                    }
            )
        }

        if (showStackBadge) {
            StackBadge(
                hiddenCount = conflictCount - 1,
                textColor = textColor,
                onClick = onStackBadgeClick
            )
        }
    }
}

@Composable
private fun BoxScope.StackBadge(
    hiddenCount: Int,
    textColor: Color,
    onClick: (() -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    val description = stringResource(Res.string.a11y_stacked_hidden_courses, hiddenCount)

    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 3.dp, end = 3.dp)
            .size(20.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = onClick != null
            ) {
                onClick?.invoke()
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.TopEnd
    ) {
        Text(
            text = "+$hiddenCount",
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor.copy(alpha = 0.78f),
            style = TextStyle(lineHeight = 1.em),
            modifier = Modifier
                .padding(top = 3.dp, end = 3.dp)
                .clearAndSetSemantics {}
        )
    }
}
