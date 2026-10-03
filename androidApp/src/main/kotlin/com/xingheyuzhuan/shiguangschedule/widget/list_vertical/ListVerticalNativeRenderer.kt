package com.xingheyuzhuan.shiguangschedule.widget.list_vertical

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.widget.RemoteViewsCompat
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.WidgetThemeModeProto
import com.xingheyuzhuan.shiguangschedule.tool.createMaterialKolorScheme
import com.xingheyuzhuan.shiguangschedule.widget.WidgetCourseProto
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 竖向列表原生小组件渲染器
 */
object ListVerticalNativeRenderer {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd")

    /**
     * 渲染并返回小组件的 RemoteViews 视图
     */
    fun render(
        context: Context,
        snapshot: WidgetSnapshot,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_list_vertical_native)

        resetWidgetState(rv)

        // 1. 绑定根布局点击事件（点击跳转主界面）
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rv.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        rv.setOnClickPendingIntent(R.id.widget_root_dark, pendingIntent)

        // 2. 应用小组件主题样式与字体缩放
        applyWidgetStyle(rv, snapshot)

        val now = LocalTime.now()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val allCourses = snapshot.courses
        val currentWeek = if (snapshot.current_week <= 0) null else snapshot.current_week

        // 3. 处理未开学/假期状态（支持开学倒计时）
        if (currentWeek == null) {
            val daysUntilStart = snapshot.days_until_term_start
            val title = context.getString(R.string.title_vacation)
            val msg = if (daysUntilStart > 0) {
                context.getString(R.string.widget_days_until_term_start, daysUntilStart)
            } else {
                context.getString(R.string.widget_vacation_expecting)
            }

            showStatus(rv, title = title, msg = msg, isFullCover = true)
            return rv
        }

        val todayStr = today.toString()
        val tomorrowStr = tomorrow.toString()

        val todayRemaining = allCourses.filter { course ->
            val matchDate = course.date == todayStr || course.date.isBlank()
            val notSkipped = !course.is_skipped
            val notEnded = try {
                LocalTime.parse(course.end_time) > now
            } catch (e: Exception) {
                true
            }
            matchDate && notSkipped && notEnded
        }.sortedBy { it.start_time }

        val tomorrowCourses = allCourses.filter { course ->
            course.date == tomorrowStr && !course.is_skipped
        }.sortedBy { it.start_time }

        val weekDaysArray = context.resources.getStringArray(R.array.week_days_names)
        val dayOfWeekStr = weekDaysArray[today.dayOfWeek.value - 1]
        val hideDate = snapshot.style?.widget_style?.hide_date ?: false

        // 4. 确定展示日期与是否为明日模式
        val displayDate: LocalDate
        val isTomorrowMode: Boolean

        when {
            todayRemaining.isNotEmpty() -> {
                displayDate = today
                isTomorrowMode = false
            }
            tomorrowCourses.isNotEmpty() -> {
                displayDate = tomorrow
                isTomorrowMode = true
            }
            else -> {
                displayDate = today
                isTomorrowMode = false
            }
        }

        // 5. 跨周自动累加计算（使用快照中的周起始日进行精准跨周判定）
        val firstDayOfWeekInt = if (snapshot.first_day_of_week in 1..7) snapshot.first_day_of_week else 1
        val firstDayOfWeek = DayOfWeek.of(firstDayOfWeekInt)

        val displayWeek = if (isTomorrowMode && displayDate.dayOfWeek == firstDayOfWeek) {
            currentWeek + 1
        } else {
            currentWeek
        }

        val weekText = context.getString(R.string.title_current_week, displayWeek.toString())
        val weekPart = if (hideDate) {
            weekText
        } else {
            "${displayDate.format(DATE_FORMATTER)} $weekText"
        }

        // 6. 根据今日剩余课程或明日预告分发渲染
        when {
            todayRemaining.isNotEmpty() -> {
                val headerTitle = "$weekPart  $dayOfWeekStr"
                val countSummary = context.getString(R.string.widget_remaining_courses_format_today, todayRemaining.size)

                rv.setTextViewText(R.id.tv_header_title, headerTitle)
                rv.setTextViewText(R.id.tv_header_title_dark, headerTitle)
                rv.setTextViewText(R.id.tv_header_count_summary, countSummary)
                rv.setTextViewText(R.id.tv_header_count_summary_dark, countSummary)

                renderCourseListView(context, rv, appWidgetId, todayRemaining, snapshot)
            }
            tomorrowCourses.isNotEmpty() -> {
                val previewTitle = context.getString(R.string.widget_tomorrow_course_preview)
                val headerTitle = "$previewTitle $weekPart"
                val countSummary = context.getString(R.string.widget_remaining_courses_format_tomorrow, tomorrowCourses.size)

                rv.setTextViewText(R.id.tv_header_title, headerTitle)
                rv.setTextViewText(R.id.tv_header_title_dark, headerTitle)
                rv.setTextViewText(R.id.tv_header_count_summary, countSummary)
                rv.setTextViewText(R.id.tv_header_count_summary_dark, countSummary)

                renderCourseListView(context, rv, appWidgetId, tomorrowCourses, snapshot)
            }
            else -> {
                val headerTitle = "$weekPart  $dayOfWeekStr"

                rv.setTextViewText(R.id.tv_header_title, headerTitle)
                rv.setTextViewText(R.id.tv_header_title_dark, headerTitle)
                rv.setTextViewText(R.id.tv_header_count_summary, "")
                rv.setTextViewText(R.id.tv_header_count_summary_dark, "")

                val hasCoursesToday = allCourses.any { it.date == todayStr || it.date.isBlank() }
                val tip = if (!hasCoursesToday) context.getString(R.string.text_no_courses_today) else context.getString(R.string.widget_today_courses_finished)
                showStatus(rv, tip, "", isFullCover = false)
            }
        }
        return rv
    }

    /**
     * 重置小组件内部视图的可见性状态
     */
    private fun resetWidgetState(rv: RemoteViews) {
        rv.setViewVisibility(R.id.container_full_status, View.GONE)
        rv.setViewVisibility(R.id.inner_content_card, View.VISIBLE)
        rv.setViewVisibility(R.id.list_view_courses, View.GONE)
        rv.setViewVisibility(R.id.container_status, View.GONE)

        rv.setViewVisibility(R.id.container_full_status_dark, View.GONE)
        rv.setViewVisibility(R.id.inner_content_card_dark, View.VISIBLE)
        rv.setViewVisibility(R.id.list_view_courses_dark, View.GONE)
        rv.setViewVisibility(R.id.container_status_dark, View.GONE)
    }

    /**
     * 渲染课程列表视图
     */
    private fun renderCourseListView(
        context: Context,
        rv: RemoteViews,
        appWidgetId: Int,
        courses: List<WidgetCourseProto>,
        snapshot: WidgetSnapshot
    ) {
        rv.setViewVisibility(R.id.list_view_courses, View.VISIBLE)
        rv.setViewVisibility(R.id.container_status, View.GONE)
        rv.setViewVisibility(R.id.list_view_courses_dark, View.VISIBLE)
        rv.setViewVisibility(R.id.container_status_dark, View.GONE)

        val builder = RemoteViewsCompat.RemoteCollectionItems.Builder()

        courses.forEach { course ->
            val itemRv = ListVerticalCourseItemRenderer.createCourseItemView(context, snapshot, course)

            val fillInIntent = Intent().apply { putExtra("course_id", course.id) }
            itemRv.setOnClickFillInIntent(R.id.item_root_course, fillInIntent)
            itemRv.setOnClickFillInIntent(R.id.item_root_course_dark, fillInIntent)

            val itemId = if (course.id.isNotBlank()) {
                course.id.hashCode().toLong() and 0x7FFFFFFFFL
            } else {
                "${course.name}_${course.start_time}".hashCode().toLong() and 0x7FFFFFFFFL
            }
            builder.addItem(itemId, itemRv)
        }

        builder.setHasStableIds(true)
        val collectionItems = builder.build()

        RemoteViewsCompat.setRemoteAdapter(context, rv, appWidgetId, R.id.list_view_courses, collectionItems)
        RemoteViewsCompat.setRemoteAdapter(context, rv, appWidgetId, R.id.list_view_courses_dark, collectionItems)

        val clickIntentTemplate = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentTemplate = PendingIntent.getActivity(
            context,
            appWidgetId,
            clickIntentTemplate,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        rv.setPendingIntentTemplate(R.id.list_view_courses, pendingIntentTemplate)
        rv.setPendingIntentTemplate(R.id.list_view_courses_dark, pendingIntentTemplate)
    }

    /**
     * 显示状态提示视图（支持全屏覆盖或卡片内嵌入）
     */
    private fun showStatus(rv: RemoteViews, title: String, msg: String?, isFullCover: Boolean) {
        if (isFullCover) {
            rv.setViewVisibility(R.id.inner_content_card, View.GONE)
            rv.setViewVisibility(R.id.container_status, View.GONE)
            rv.setViewVisibility(R.id.container_full_status, View.VISIBLE)
            rv.setTextViewText(R.id.tv_full_status_title, title)

            rv.setViewVisibility(R.id.inner_content_card_dark, View.GONE)
            rv.setViewVisibility(R.id.container_status_dark, View.GONE)
            rv.setViewVisibility(R.id.container_full_status_dark, View.VISIBLE)
            rv.setTextViewText(R.id.tv_full_status_title_dark, title)

            if (!msg.isNullOrBlank()) {
                rv.setTextViewText(R.id.tv_full_status_msg, msg)
                rv.setViewVisibility(R.id.tv_full_status_msg, View.VISIBLE)
                rv.setTextViewText(R.id.tv_full_status_msg_dark, msg)
                rv.setViewVisibility(R.id.tv_full_status_msg_dark, View.VISIBLE)
            } else {
                rv.setViewVisibility(R.id.tv_full_status_msg, View.GONE)
                rv.setViewVisibility(R.id.tv_full_status_msg_dark, View.GONE)
            }
        } else {
            rv.setViewVisibility(R.id.inner_content_card, View.VISIBLE)
            rv.setViewVisibility(R.id.list_view_courses, View.GONE)
            rv.setViewVisibility(R.id.container_status, View.VISIBLE)
            rv.setViewVisibility(R.id.container_full_status, View.GONE)
            rv.setTextViewText(R.id.tv_status_title, title)

            rv.setViewVisibility(R.id.inner_content_card_dark, View.VISIBLE)
            rv.setViewVisibility(R.id.list_view_courses_dark, View.GONE)
            rv.setViewVisibility(R.id.container_status_dark, View.VISIBLE)
            rv.setViewVisibility(R.id.container_full_status_dark, View.GONE)
            rv.setTextViewText(R.id.tv_status_title_dark, title)

            if (!msg.isNullOrBlank()) {
                rv.setTextViewText(R.id.tv_status_msg, msg)
                rv.setViewVisibility(R.id.tv_status_msg, View.VISIBLE)
                rv.setTextViewText(R.id.tv_status_msg_dark, msg)
                rv.setViewVisibility(R.id.tv_status_msg_dark, View.VISIBLE)
            } else {
                rv.setViewVisibility(R.id.tv_status_msg, View.GONE)
                rv.setViewVisibility(R.id.tv_status_msg_dark, View.GONE)
            }
        }
    }

    /**
     * 解析主题配置并应用于亮色/暗色状态视图
     */
    private fun applyWidgetStyle(rv: RemoteViews, snapshot: WidgetSnapshot) {
        val widgetStyle = snapshot.style?.widget_style
        val alphaPercent: Float = widgetStyle?.background_alpha ?: 1.0f
        val fontScale: Float = widgetStyle?.font_scale ?: 1.0f
        val themeMode = widgetStyle?.theme_mode
        val seedColorLong = widgetStyle?.seed_color

        val isDarkForLightId = themeMode == WidgetThemeModeProto.WIDGET_THEME_DARK
        val isDarkForDarkId = themeMode != WidgetThemeModeProto.WIDGET_THEME_LIGHT

        applyGroupStyle(
            rv, isDarkForLightId, seedColorLong, alphaPercent, fontScale,
            R.id.card_bg_image, R.id.full_status_bg_image, R.id.tv_header_title, R.id.tv_header_count_summary,
            R.id.tv_status_title, R.id.tv_status_msg, R.id.tv_full_status_title, R.id.tv_full_status_msg
        )

        applyGroupStyle(
            rv, isDarkForDarkId, seedColorLong, alphaPercent, fontScale,
            R.id.card_bg_image_dark, R.id.full_status_bg_image_dark, R.id.tv_header_title_dark, R.id.tv_header_count_summary_dark,
            R.id.tv_status_title_dark, R.id.tv_status_msg_dark, R.id.tv_full_status_title_dark, R.id.tv_full_status_msg_dark
        )
    }

    /**
     * 设置状态视图组的背景色、文字颜色与字体缩放
     */
    private fun applyGroupStyle(
        rv: RemoteViews, isDarkMode: Boolean, seedColorLong: Long?, alphaPercent: Float, fontScale: Float,
        cardBgId: Int, fullStatusBgId: Int, headerTitleId: Int, headerCountSummaryId: Int,
        statusTitleId: Int, statusMsgId: Int, fullStatusTitleId: Int, fullStatusMsgId: Int
    ) {
        val scheme = if (seedColorLong != null) {
            createMaterialKolorScheme(darkTheme = isDarkMode, seedColor = Color(seedColorLong))
        } else {
            createMaterialKolorScheme(darkTheme = isDarkMode)
        }

        val baseBgColor: Int
        val primaryTextColor: Int
        val secondaryTextColor: Int

        if (seedColorLong == null) {
            baseBgColor = scheme.surfaceContainer.toArgb()
            primaryTextColor = scheme.onSurface.toArgb()
            secondaryTextColor = scheme.onSurfaceVariant.toArgb()
        } else {
            baseBgColor = scheme.primaryContainer.toArgb()
            val contentColor = scheme.onPrimaryContainer.toArgb()
            primaryTextColor = contentColor
            secondaryTextColor = contentColor
        }

        rv.setImageViewBackground(cardBgId, baseBgColor, alphaPercent)
        rv.setImageViewBackground(fullStatusBgId, baseBgColor, alphaPercent)

        rv.setTextColor(headerTitleId, primaryTextColor)
        rv.setTextColor(headerCountSummaryId, secondaryTextColor)
        rv.setTextColor(statusTitleId, primaryTextColor)
        rv.setTextColor(statusMsgId, secondaryTextColor)
        rv.setTextColor(fullStatusTitleId, primaryTextColor)
        rv.setTextColor(fullStatusMsgId, secondaryTextColor)

        val baseHeaderTitleSize = 11f
        val baseHeaderCountSize = 10f
        val baseStatusTitleSize = 16f
        val baseStatusMsgSize = 11f
        val baseFullStatusTitleSize = 18f
        val baseFullStatusMsgSize = 13f

        val scale = if (fontScale > 0f) fontScale else 1.0f

        rv.setTextViewTextSize(headerTitleId, TypedValue.COMPLEX_UNIT_DIP, baseHeaderTitleSize * scale)
        rv.setTextViewTextSize(headerCountSummaryId, TypedValue.COMPLEX_UNIT_DIP, baseHeaderCountSize * scale)
        rv.setTextViewTextSize(statusTitleId, TypedValue.COMPLEX_UNIT_DIP, baseStatusTitleSize * scale)
        rv.setTextViewTextSize(statusMsgId, TypedValue.COMPLEX_UNIT_DIP, baseStatusMsgSize * scale)
        rv.setTextViewTextSize(fullStatusTitleId, TypedValue.COMPLEX_UNIT_DIP, baseFullStatusTitleSize * scale)
        rv.setTextViewTextSize(fullStatusMsgId, TypedValue.COMPLEX_UNIT_DIP, baseFullStatusMsgSize * scale)
    }

    /**
     * 设置 ImageView 背景色与透明度（兼容不同 Android 版本）
     */
    private fun RemoteViews.setImageViewBackground(
        bgViewId: Int, @ColorInt baseColor: Int, @FloatRange(from = 0.0, to = 1.0) alphaPercent: Float
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.setColorStateList(bgViewId, "setImageTintList", ColorStateList.valueOf(baseColor))
        } else {
            this.setInt(bgViewId, "setColorFilter", baseColor)
        }

        val alphaInt = (alphaPercent * 255).toInt().coerceIn(0, 255)
        this.setInt(bgViewId, "setImageAlpha", alphaInt)
    }
}