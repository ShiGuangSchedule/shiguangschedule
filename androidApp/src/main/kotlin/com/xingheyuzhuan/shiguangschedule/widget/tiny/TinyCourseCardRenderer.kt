package com.xingheyuzhuan.shiguangschedule.widget.tiny

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.WidgetThemeModeProto
import com.xingheyuzhuan.shiguangschedule.tool.createMaterialKolorScheme
import com.xingheyuzhuan.shiguangschedule.widget.WidgetCourseProto
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot

/**
 * 迷你课程卡片渲染器（用于 StackView 子项）
 */
object TinyCourseCardRenderer {

    /**
     * 构建单张课程卡片 RemoteViews
     */
    fun buildCourseCard(
        context: Context,
        snapshot: WidgetSnapshot,
        course: WidgetCourseProto,
        remainingCount: Int
    ): RemoteViews {
        val cardRv = RemoteViews(context.packageName, R.layout.widget_tiny_item)

        // 1. 应用小组件主题样式与字体缩放
        applyWidgetStyle(cardRv, snapshot)

        // 2. 设置课程名称与时间
        cardRv.setTextViewText(R.id.tv_course_name, course.name)
        cardRv.setTextViewText(R.id.tv_course_name_dark, course.name)

        val timeText = "${course.start_time.take(5)} - ${course.end_time.take(5)}"
        cardRv.setTextViewText(R.id.tv_course_time, timeText)
        cardRv.setTextViewText(R.id.tv_course_time_dark, timeText)

        // 3. 设置课程地点（若隐藏或为空则隐藏控件）
        val hideLocation = snapshot.style?.widget_style?.hide_location ?: false
        if (hideLocation || course.position.isBlank()) {
            cardRv.setViewVisibility(R.id.tv_course_position, View.GONE)
            cardRv.setViewVisibility(R.id.tv_course_position_dark, View.GONE)
        } else {
            cardRv.setTextViewText(R.id.tv_course_position, course.position)
            cardRv.setViewVisibility(R.id.tv_course_position, View.VISIBLE)
            cardRv.setTextViewText(R.id.tv_course_position_dark, course.position)
            cardRv.setViewVisibility(R.id.tv_course_position_dark, View.VISIBLE)
        }

        // 4. 设置剩余课程角标数量
        val remainingStr = remainingCount.toString()
        cardRv.setTextViewText(R.id.tv_remaining_count, remainingStr)
        cardRv.setTextViewText(R.id.tv_remaining_count_dark, remainingStr)

        // 5. 设置课程主题气泡颜色
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

            // 常规视图气泡颜色设置
            val targetColorLight = (if (isDarkForLightId) colorPair.dark_color else colorPair.light_color).toInt()
            cardRv.setViewVisibility(R.id.bubble_bg_image, View.VISIBLE)
            setBubbleTint(cardRv, R.id.bubble_bg_image, targetColorLight)

            // 暗色视图气泡颜色设置
            val targetColorDark = (if (isDarkForDarkId) colorPair.dark_color else colorPair.light_color).toInt()
            cardRv.setViewVisibility(R.id.bubble_bg_image_dark, View.VISIBLE)
            setBubbleTint(cardRv, R.id.bubble_bg_image_dark, targetColorDark)
        }

        return cardRv
    }

    /**
     * 设置气泡着色
     */
    private fun setBubbleTint(rv: RemoteViews, viewId: Int, colorInt: Int) {
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

        val alphaPercent: Float = widgetStyle?.background_alpha ?: 1.0f
        val fontScale: Float = widgetStyle?.font_scale ?: 1.0f
        val themeMode = widgetStyle?.theme_mode

        // 直接提取可空的 seed_color（未配置时为 null）
        val seedColorLong = widgetStyle?.seed_color

        val isDarkForLightId = when (themeMode) {
            WidgetThemeModeProto.WIDGET_THEME_DARK -> true
            else -> false
        }
        val isDarkForDarkId = when (themeMode) {
            WidgetThemeModeProto.WIDGET_THEME_LIGHT -> false
            else -> true
        }

        // 应用常规亮色布局样式
        applyStyleForGroup(
            rv = rv,
            isDarkMode = isDarkForLightId,
            seedColorLong = seedColorLong,
            alphaPercent = alphaPercent,
            fontScale = fontScale,
            cardBgId = R.id.card_bg_image,
            courseNameId = R.id.tv_course_name,
            remainingCountId = R.id.tv_remaining_count,
            courseTimeId = R.id.tv_course_time,
            coursePositionId = R.id.tv_course_position
        )

        // 应用暗色布局样式
        applyStyleForGroup(
            rv = rv,
            isDarkMode = isDarkForDarkId,
            seedColorLong = seedColorLong,
            alphaPercent = alphaPercent,
            fontScale = fontScale,
            cardBgId = R.id.card_bg_image_dark,
            courseNameId = R.id.tv_course_name_dark,
            remainingCountId = R.id.tv_remaining_count_dark,
            courseTimeId = R.id.tv_course_time_dark,
            coursePositionId = R.id.tv_course_position_dark
        )
    }

    /**
     * 设置样式组的背景颜色、文本颜色与字体缩放
     */
    private fun applyStyleForGroup(
        rv: RemoteViews,
        isDarkMode: Boolean,
        seedColorLong: Long?,
        alphaPercent: Float,
        fontScale: Float,
        cardBgId: Int,
        courseNameId: Int,
        remainingCountId: Int,
        courseTimeId: Int,
        coursePositionId: Int
    ) {
        // 根据 seedColorLong 是否为空选择生成 Scheme 的方式（不传 seedColor 时使用函数内部默认参数）
        val scheme = if (seedColorLong != null) {
            createMaterialKolorScheme(darkTheme = isDarkMode, seedColor = Color(seedColorLong))
        } else {
            createMaterialKolorScheme(darkTheme = isDarkMode)
        }

        val baseBgColor: Int
        val primaryTextColor: Int
        val secondaryTextColor: Int

        if (seedColorLong == null) {
            // 1. 默认极简模式：Surface 语义（背景极简黑白灰，标题与副标题靠 onSurface 与 onSurfaceVariant 区分）
            baseBgColor = scheme.surfaceContainer.toArgb()
            primaryTextColor = scheme.onSurface.toArgb()
            secondaryTextColor = scheme.onSurfaceVariant.toArgb()
        } else {
            // 2. 个性定制模式：Container 语义（彩色背景，文本统一使用对应 onContainer 保证可读性）
            baseBgColor = scheme.primaryContainer.toArgb()
            val contentColor = scheme.onPrimaryContainer.toArgb()
            primaryTextColor = contentColor
            secondaryTextColor = contentColor
        }

        rv.setCardBackground(cardBgId, baseBgColor, alphaPercent)

        // 设置文本颜色
        rv.setTextColor(courseNameId, primaryTextColor)
        rv.setTextColor(remainingCountId, primaryTextColor)
        rv.setTextColor(courseTimeId, secondaryTextColor)
        rv.setTextColor(coursePositionId, secondaryTextColor)

        // 基础字体大小定义 (dp)
        val baseCourseNameSize = 14f
        val baseRemainingCountSize = 14f
        val baseCourseTimeSize = 12f
        val baseCoursePositionSize = 12f

        // 容错处理：确保缩放系数有效
        val scale = if (fontScale > 0f) fontScale else 1.0f

        rv.setTextViewTextSize(courseNameId, TypedValue.COMPLEX_UNIT_DIP, baseCourseNameSize * scale)
        rv.setTextViewTextSize(remainingCountId, TypedValue.COMPLEX_UNIT_DIP, baseRemainingCountSize * scale)
        rv.setTextViewTextSize(courseTimeId, TypedValue.COMPLEX_UNIT_DIP, baseCourseTimeSize * scale)
        rv.setTextViewTextSize(coursePositionId, TypedValue.COMPLEX_UNIT_DIP, baseCoursePositionSize * scale)
    }

    /**
     * 设置卡片背景色及透明度
     */
    private fun RemoteViews.setCardBackground(
        bgImageViewId: Int,
        @ColorInt baseColor: Int,
        @FloatRange(from = 0.0, to = 1.0) alphaPercent: Float
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.setColorStateList(bgImageViewId, "setImageTintList", ColorStateList.valueOf(baseColor))
        } else {
            this.setInt(bgImageViewId, "setColorFilter", baseColor)
        }

        val alphaInt = (alphaPercent * 255).toInt().coerceIn(0, 255)
        this.setInt(bgImageViewId, "setImageAlpha", alphaInt)
    }
}