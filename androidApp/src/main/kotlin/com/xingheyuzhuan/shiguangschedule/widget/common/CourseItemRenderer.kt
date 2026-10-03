package com.xingheyuzhuan.shiguangschedule.widget.common

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.WidgetThemeModeProto
import com.xingheyuzhuan.shiguangschedule.tool.createMaterialKolorScheme
import com.xingheyuzhuan.shiguangschedule.widget.WidgetCourseProto
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot

/**
 * 通用课程列表项渲染器（用于 Compact、DoubleDays 等 ListView 列表项）
 * 完整支持主题样式、字体缩放、隐藏地点、隐藏教师、颜色指示条等配置
 */
object CourseItemRenderer {

    /**
     * 绑定单条课程数据到 widget_item_course_common 布局
     */
    fun createCourseItemView(
        context: Context,
        snapshot: WidgetSnapshot,
        course: WidgetCourseProto
    ): RemoteViews {
        val itemRv = RemoteViews(context.packageName, R.layout.widget_item_course_common)

        // 1. 应用小组件动态主题样式与字体缩放
        applyWidgetStyle(itemRv, snapshot)

        // 2. 设置课程名称与时间
        itemRv.setTextViewText(R.id.tv_course_name, course.name)
        itemRv.setTextViewText(R.id.tv_course_name_dark, course.name)

        val timeText = "${course.start_time.take(5)}-${course.end_time.take(5)}"
        itemRv.setTextViewText(R.id.tv_course_time, timeText)
        itemRv.setTextViewText(R.id.tv_course_time_dark, timeText)

        // 3. 设置教室地点（遵循 hide_location 配置）
        val hideLocation = snapshot.style?.widget_style?.hide_location ?: false
        if (hideLocation || course.position.isBlank()) {
            itemRv.setViewVisibility(R.id.tv_course_position, View.GONE)
            itemRv.setViewVisibility(R.id.tv_course_position_dark, View.GONE)
        } else {
            itemRv.setTextViewText(R.id.tv_course_position, course.position)
            itemRv.setViewVisibility(R.id.tv_course_position, View.VISIBLE)
            itemRv.setTextViewText(R.id.tv_course_position_dark, course.position)
            itemRv.setViewVisibility(R.id.tv_course_position_dark, View.VISIBLE)
        }

        // 4. 设置教师姓名（遵循 hide_teacher 配置）
        val hideTeacher = snapshot.style?.widget_style?.hide_teacher ?: false
        if (hideTeacher || course.teacher.isBlank()) {
            itemRv.setViewVisibility(R.id.tv_course_teacher, View.GONE)
            itemRv.setViewVisibility(R.id.tv_course_teacher_dark, View.GONE)
        } else {
            itemRv.setTextViewText(R.id.tv_course_teacher, course.teacher)
            itemRv.setViewVisibility(R.id.tv_course_teacher, View.VISIBLE)
            itemRv.setTextViewText(R.id.tv_course_teacher_dark, course.teacher)
            itemRv.setViewVisibility(R.id.tv_course_teacher_dark, View.VISIBLE)
        }

        // 5. 设置课程左侧指示条颜色
        val colorInt = course.color_int
        val colorPair = snapshot.style?.course_color_maps?.getOrNull(colorInt)

        if (colorPair != null) {
            val themeMode = snapshot.style?.widget_style?.theme_mode

            val isDarkForLightId = when (themeMode) {
                WidgetThemeModeProto.WIDGET_THEME_DARK -> true
                else -> false
            }
            val isDarkForDarkId = when (themeMode) {
                WidgetThemeModeProto.WIDGET_THEME_LIGHT -> false
                else -> true
            }

            // 常规视图指示条着色
            val targetColorLight = (if (isDarkForLightId) colorPair.dark_color else colorPair.light_color).toInt()
            setIndicatorTint(itemRv, R.id.course_indicator, targetColorLight)

            // 暗色视图指示条着色
            val targetColorDark = (if (isDarkForDarkId) colorPair.dark_color else colorPair.light_color).toInt()
            setIndicatorTint(itemRv, R.id.course_indicator_dark, targetColorDark)
        }

        return itemRv
    }

    /**
     * 设置指示条着色
     */
    private fun setIndicatorTint(rv: RemoteViews, viewId: Int, colorInt: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rv.setColorStateList(
                viewId,
                "setImageTintList",
                ColorStateList.valueOf(colorInt)
            )
        } else {
            rv.setInt(viewId, "setColorFilter", colorInt)
        }
    }

    /**
     * 解析主题配置并分别应用于常规与暗色视图控件
     */
    private fun applyWidgetStyle(rv: RemoteViews, snapshot: WidgetSnapshot) {
        val widgetStyle = snapshot.style?.widget_style

        val fontScale: Float = widgetStyle?.font_scale ?: 1.0f
        val themeMode = widgetStyle?.theme_mode
        val seedColorLong = widgetStyle?.seed_color

        val isDarkForLightId = when (themeMode) {
            WidgetThemeModeProto.WIDGET_THEME_DARK -> true
            else -> false
        }
        val isDarkForDarkId = when (themeMode) {
            WidgetThemeModeProto.WIDGET_THEME_LIGHT -> false
            else -> true
        }

        // 应用常规亮色布局文本与字体样式
        applyStyleForGroup(
            rv = rv,
            isDarkMode = isDarkForLightId,
            seedColorLong = seedColorLong,
            fontScale = fontScale,
            courseNameId = R.id.tv_course_name,
            courseTimeId = R.id.tv_course_time,
            coursePositionId = R.id.tv_course_position,
            courseTeacherId = R.id.tv_course_teacher
        )

        // 应用暗色布局文本与字体样式
        applyStyleForGroup(
            rv = rv,
            isDarkMode = isDarkForDarkId,
            seedColorLong = seedColorLong,
            fontScale = fontScale,
            courseNameId = R.id.tv_course_name_dark,
            courseTimeId = R.id.tv_course_time_dark,
            coursePositionId = R.id.tv_course_position_dark,
            courseTeacherId = R.id.tv_course_teacher_dark
        )
    }

    /**
     * 设置样式组的文本颜色与动态字体缩放
     */
    private fun applyStyleForGroup(
        rv: RemoteViews,
        isDarkMode: Boolean,
        seedColorLong: Long?,
        fontScale: Float,
        courseNameId: Int,
        courseTimeId: Int,
        coursePositionId: Int,
        courseTeacherId: Int
    ) {
        val scheme = if (seedColorLong != null) {
            createMaterialKolorScheme(darkTheme = isDarkMode, seedColor = Color(seedColorLong))
        } else {
            createMaterialKolorScheme(darkTheme = isDarkMode)
        }

        val primaryTextColor: Int
        val secondaryTextColor: Int
        val hintTextColor: Int

        if (seedColorLong == null) {
            // 默认极简模式
            primaryTextColor = scheme.onSurface.toArgb()
            secondaryTextColor = scheme.onSurfaceVariant.toArgb()
            hintTextColor = scheme.outline.toArgb()
        } else {
            // 个性定制模式
            val contentColor = scheme.onPrimaryContainer.toArgb()
            primaryTextColor = contentColor
            secondaryTextColor = contentColor
            hintTextColor = contentColor
        }

        // 设置文本颜色
        rv.setTextColor(courseNameId, primaryTextColor)
        rv.setTextColor(coursePositionId, secondaryTextColor)
        rv.setTextColor(courseTimeId, secondaryTextColor)
        rv.setTextColor(courseTeacherId, hintTextColor)

        // 基础字体大小定义 (dp)
        val baseNameSize = 12f
        val basePositionSize = 10f
        val baseTimeSize = 10f
        val baseTeacherSize = 9f

        val effectiveScale = if (fontScale > 0f) fontScale else 1.0f

        rv.setTextViewTextSize(courseNameId, TypedValue.COMPLEX_UNIT_DIP, baseNameSize * effectiveScale)
        rv.setTextViewTextSize(coursePositionId, TypedValue.COMPLEX_UNIT_DIP, basePositionSize * effectiveScale)
        rv.setTextViewTextSize(courseTimeId, TypedValue.COMPLEX_UNIT_DIP, baseTimeSize * effectiveScale)
        rv.setTextViewTextSize(courseTeacherId, TypedValue.COMPLEX_UNIT_DIP, baseTeacherSize * effectiveScale)
    }
}