package com.xingheyuzhuan.shiguangschedule.widget.list_vertical

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

object ListVerticalCourseItemRenderer {

    fun createCourseItemView(
        context: Context,
        snapshot: WidgetSnapshot,
        course: WidgetCourseProto
    ): RemoteViews {
        val itemRv = RemoteViews(context.packageName, R.layout.widget_item_course_list_node)

        // 1. 应用动态主题与缩放
        applyWidgetStyle(itemRv, snapshot)

        // 2. 设置课程名称
        itemRv.setTextViewText(R.id.tv_course_name, course.name)
        itemRv.setTextViewText(R.id.tv_course_name_dark, course.name)

        // 3. 设置时间
        val startTimeText = course.start_time.take(5)
        val endTimeText = course.end_time.take(5)

        itemRv.setTextViewText(R.id.tv_course_start_time, startTimeText)
        itemRv.setTextViewText(R.id.tv_course_start_time_dark, startTimeText)
        itemRv.setTextViewText(R.id.tv_course_end_time, endTimeText)
        itemRv.setTextViewText(R.id.tv_course_end_time_dark, endTimeText)

        // 4. 地点隐藏控制
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

        // 5. 教师隐藏控制
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

        // 6. 指示条着色
        val colorInt = course.color_int
        val colorPair = snapshot.style?.course_color_maps?.getOrNull(colorInt)

        if (colorPair != null) {
            val themeMode = snapshot.style?.widget_style?.theme_mode

            val isDarkForLightId = themeMode == WidgetThemeModeProto.WIDGET_THEME_DARK
            val isDarkForDarkId = themeMode != WidgetThemeModeProto.WIDGET_THEME_LIGHT

            val targetColorLight = (if (isDarkForLightId) colorPair.dark_color else colorPair.light_color).toInt()
            setIndicatorTint(itemRv, R.id.course_indicator, targetColorLight)

            val targetColorDark = (if (isDarkForDarkId) colorPair.dark_color else colorPair.light_color).toInt()
            setIndicatorTint(itemRv, R.id.course_indicator_dark, targetColorDark)
        }

        return itemRv
    }

    private fun setIndicatorTint(rv: RemoteViews, viewId: Int, colorInt: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rv.setColorStateList(viewId, "setImageTintList", ColorStateList.valueOf(colorInt))
        } else {
            rv.setInt(viewId, "setColorFilter", colorInt)
        }
    }

    private fun applyWidgetStyle(rv: RemoteViews, snapshot: WidgetSnapshot) {
        val widgetStyle = snapshot.style?.widget_style
        val fontScale: Float = widgetStyle?.font_scale ?: 1.0f
        val themeMode = widgetStyle?.theme_mode
        val seedColorLong = widgetStyle?.seed_color

        val isDarkForLightId = themeMode == WidgetThemeModeProto.WIDGET_THEME_DARK
        val isDarkForDarkId = themeMode != WidgetThemeModeProto.WIDGET_THEME_LIGHT

        applyStyleForGroup(rv, isDarkForLightId, seedColorLong, fontScale,
            R.id.tv_course_name, R.id.tv_course_start_time, R.id.tv_course_end_time, R.id.tv_course_position, R.id.tv_course_teacher)

        applyStyleForGroup(rv, isDarkForDarkId, seedColorLong, fontScale,
            R.id.tv_course_name_dark, R.id.tv_course_start_time_dark, R.id.tv_course_end_time_dark, R.id.tv_course_position_dark, R.id.tv_course_teacher_dark)
    }

    private fun applyStyleForGroup(
        rv: RemoteViews, isDarkMode: Boolean, seedColorLong: Long?, fontScale: Float,
        courseNameId: Int, startTimeId: Int, endTimeId: Int, coursePositionId: Int, courseTeacherId: Int
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
            primaryTextColor = scheme.onSurface.toArgb()
            secondaryTextColor = scheme.onSurfaceVariant.toArgb()
            hintTextColor = scheme.outline.toArgb()
        } else {
            val contentColor = scheme.onPrimaryContainer.toArgb()
            primaryTextColor = contentColor
            secondaryTextColor = contentColor
            hintTextColor = contentColor
        }

        rv.setTextColor(courseNameId, primaryTextColor)
        rv.setTextColor(startTimeId, primaryTextColor)
        rv.setTextColor(endTimeId, secondaryTextColor)
        rv.setTextColor(coursePositionId, secondaryTextColor)
        rv.setTextColor(courseTeacherId, hintTextColor)

        val baseNameSize = 13f
        val baseTimeSize = 10.5f
        val basePositionSize = 10f
        val baseTeacherSize = 9f
        val scale = if (fontScale > 0f) fontScale else 1.0f

        rv.setTextViewTextSize(courseNameId, TypedValue.COMPLEX_UNIT_DIP, baseNameSize * scale)
        rv.setTextViewTextSize(startTimeId, TypedValue.COMPLEX_UNIT_DIP, baseTimeSize * scale)
        rv.setTextViewTextSize(endTimeId, TypedValue.COMPLEX_UNIT_DIP, baseTimeSize * scale)
        rv.setTextViewTextSize(coursePositionId, TypedValue.COMPLEX_UNIT_DIP, basePositionSize * scale)
        rv.setTextViewTextSize(courseTeacherId, TypedValue.COMPLEX_UNIT_DIP, baseTeacherSize * scale)
    }
}