package com.xingheyuzhuan.shiguangschedule.widget.compact

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
import com.xingheyuzhuan.shiguangschedule.widget.common.CourseItemRenderer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 紧凑型原生小组件渲染器
 */
object CompactNativeRenderer {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd")

    /**
     * 渲染并返回紧凑型小组件的 RemoteViews 视图
     */
    fun render(
        context: Context,
        snapshot: WidgetSnapshot,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_today_compact_native)

        // 1. 状态彻底重置（同时重置常规与暗色 View 树）
        resetWidgetState(rv)

        // 2. 设置根布局点击跳转应用主界面（双树绑定）
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rv.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        rv.setOnClickPendingIntent(R.id.widget_root_dark, pendingIntent)

        // 3. 应用动态主题样式、透明度与字体缩放
        applyWidgetStyle(rv, snapshot)

        // 4. 数据准备
        val now = LocalTime.now()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val allCourses = snapshot.courses
        val currentWeek = if (snapshot.current_week <= 0) null else snapshot.current_week

        // 情况 A：假期/未开学处理 (支持开学倒计时显示)
        if (currentWeek == null) {
            val weekDaysArray = context.resources.getStringArray(R.array.week_days_names)
            val dayOfWeekStr = weekDaysArray[today.dayOfWeek.value - 1]
            rv.setTextViewText(R.id.tv_header_title, dayOfWeekStr)
            rv.setTextViewText(R.id.tv_header_title_dark, dayOfWeekStr)

            val daysUntilStart = snapshot.days_until_term_start
            val title = context.getString(R.string.title_vacation)
            val msg = if (daysUntilStart > 0) {
                context.getString(R.string.widget_days_until_term_start, daysUntilStart)
            } else {
                context.getString(R.string.widget_vacation_expecting)
            }

            showStatus(
                rv,
                title = title,
                msg = msg,
                isFullCover = true
            )
            return rv
        }

        // 核心调度逻辑
        val todayStr = today.toString()
        val tomorrowStr = tomorrow.toString()

        val todayRemaining = allCourses.filter {
            (it.date == todayStr || it.date.isBlank()) && !it.is_skipped && try { LocalTime.parse(it.end_time) > now } catch (_: Exception) { true }
        }.sortedBy { it.start_time }

        val tomorrowCourses = allCourses.filter { it.date == tomorrowStr && !it.is_skipped }.sortedBy { it.start_time }

        // 决定渲染路径与目标显示日期
        val displayDate: LocalDate
        val isTomorrowMode: Boolean

        when {
            todayRemaining.isNotEmpty() -> {
                // 状态 1：今日剩余
                displayDate = today
                isTomorrowMode = false
                val weekDaysArray = context.resources.getStringArray(R.array.week_days_names)
                val dayOfWeekStr = weekDaysArray[today.dayOfWeek.value - 1]
                rv.setTextViewText(R.id.tv_header_title, dayOfWeekStr)
                rv.setTextViewText(R.id.tv_header_title_dark, dayOfWeekStr)
                renderCourseListView(context, rv, appWidgetId, todayRemaining, snapshot, isTomorrow = false)
            }
            tomorrowCourses.isNotEmpty() -> {
                // 状态 2：明日预告
                displayDate = tomorrow
                isTomorrowMode = true
                val previewTitle = context.getString(R.string.widget_tomorrow_course_preview)
                rv.setTextViewText(R.id.tv_header_title, previewTitle)
                rv.setTextViewText(R.id.tv_header_title_dark, previewTitle)
                renderCourseListView(context, rv, appWidgetId, tomorrowCourses, snapshot, isTomorrow = true)
            }
            else -> {
                // 状态 3：今明无课
                displayDate = today
                isTomorrowMode = false
                val weekDaysArray = context.resources.getStringArray(R.array.week_days_names)
                val dayOfWeekStr = weekDaysArray[today.dayOfWeek.value - 1]
                rv.setTextViewText(R.id.tv_header_title, dayOfWeekStr)
                rv.setTextViewText(R.id.tv_header_title_dark, dayOfWeekStr)

                val hasCoursesToday = allCourses.any { it.date == todayStr || it.date.isBlank() }
                val tip = if (!hasCoursesToday) {
                    context.getString(R.string.text_no_courses_today)
                } else {
                    context.getString(R.string.widget_today_courses_finished)
                }
                showStatus(rv, tip, "", isFullCover = false)
            }
        }

        // 5. 头部日期与周数渲染（使用快照中的周起始日进行跨周判断）
        val hideDate = snapshot.style?.widget_style?.hide_date ?: false
        renderDateAndWeek(rv, context, displayDate, currentWeek, isTomorrowMode, hideDate, snapshot)

        return rv
    }

    /**
     * 渲染月日日期与周数，实现日期隐藏联动与基于周起始日的跨周自动累加
     */
    private fun renderDateAndWeek(
        rv: RemoteViews,
        context: Context,
        displayDate: LocalDate,
        currentWeek: Int,
        isTomorrowMode: Boolean,
        hideDate: Boolean,
        snapshot: WidgetSnapshot
    ) {
        // 1. 使用快照中的周起始日计算要显示的周数
        val firstDayOfWeekInt = if (snapshot.first_day_of_week in 1..7) snapshot.first_day_of_week else 1
        val firstDayOfWeek = DayOfWeek.of(firstDayOfWeekInt)

        val displayWeek = if (isTomorrowMode && displayDate.dayOfWeek == firstDayOfWeek) {
            currentWeek + 1
        } else {
            currentWeek
        }
        val weekStr = context.getString(R.string.status_current_week_format, displayWeek)

        // 2. 根据 hideDate 决定是否包含月日（格式：MM.dd）
        val headerInfoText = if (hideDate) {
            weekStr
        } else {
            "${displayDate.format(DATE_FORMATTER)} $weekStr"
        }

        // 3. 更新 UI
        rv.setViewVisibility(R.id.tv_current_week, View.VISIBLE)
        rv.setTextViewText(R.id.tv_current_week, headerInfoText)
        rv.setViewVisibility(R.id.tv_current_week_dark, View.VISIBLE)
        rv.setTextViewText(R.id.tv_current_week_dark, headerInfoText)
    }

    /**
     * 重置所有 View 的可见性（常规 + 暗色树）
     */
    private fun resetWidgetState(rv: RemoteViews) {
        // 常规树重置
        rv.setViewVisibility(R.id.container_full_status, View.GONE)
        rv.setViewVisibility(R.id.inner_content_card, View.VISIBLE)
        rv.setViewVisibility(R.id.list_view_courses, View.GONE)
        rv.setViewVisibility(R.id.container_status, View.GONE)
        rv.setViewVisibility(R.id.tv_current_week, View.GONE)

        // 暗色树重置
        rv.setViewVisibility(R.id.container_full_status_dark, View.GONE)
        rv.setViewVisibility(R.id.inner_content_card_dark, View.VISIBLE)
        rv.setViewVisibility(R.id.list_view_courses_dark, View.GONE)
        rv.setViewVisibility(R.id.container_status_dark, View.GONE)
        rv.setViewVisibility(R.id.tv_current_week_dark, View.GONE)
    }

    /**
     * 使用 RemoteCollectionItems 渲染 ListView 课程列表
     */
    private fun renderCourseListView(
        context: Context,
        rv: RemoteViews,
        appWidgetId: Int,
        courses: List<WidgetCourseProto>,
        snapshot: WidgetSnapshot,
        isTomorrow: Boolean
    ) {
        // 显隐同步设置
        rv.setViewVisibility(R.id.list_view_courses, View.VISIBLE)
        rv.setViewVisibility(R.id.container_status, View.GONE)
        rv.setViewVisibility(R.id.list_view_courses_dark, View.VISIBLE)
        rv.setViewVisibility(R.id.container_status_dark, View.GONE)

        // 1. 更新底部汇总文本
        val footerRes = if (isTomorrow) R.string.widget_course_total_count else R.string.widget_course_remaining_count
        val footerStr = context.getString(footerRes, courses.size)
        rv.setTextViewText(R.id.tv_footer, footerStr)
        rv.setTextViewText(R.id.tv_footer_dark, footerStr)

        // 2. 使用 RemoteCollectionItems 构建数据源
        val builder = RemoteViewsCompat.RemoteCollectionItems.Builder()

        courses.forEach { course ->
            // 构建单个课程子视图
            val itemRv = CourseItemRenderer.createCourseItemView(context, snapshot, course)

            // 设置子项点击响应（填充 Intent - 兼顾常规与暗色树）
            itemRv.setOnClickFillInIntent(R.id.item_root_card, Intent())
            itemRv.setOnClickFillInIntent(R.id.item_root_card_dark, Intent())

            // 生成唯一 Stable ID
            val itemId = if (course.id.isNotBlank()) {
                course.id.hashCode().toLong() and 0x7FFFFFFFFL
            } else {
                "${course.name}_${course.start_time}".hashCode().toLong() and 0x7FFFFFFFFL
            }
            builder.addItem(itemId, itemRv)
        }

        builder.setHasStableIds(true)
        val collectionItems = builder.build()

        // 3. 绑定 RemoteAdapter 到常规和暗色的 list_view
        RemoteViewsCompat.setRemoteAdapter(context, rv, appWidgetId, R.id.list_view_courses, collectionItems)
        RemoteViewsCompat.setRemoteAdapter(context, rv, appWidgetId, R.id.list_view_courses_dark, collectionItems)

        // 4. 设置 PendingIntentTemplate 以处理卡片点击跳转
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
            // 常规树
            rv.setViewVisibility(R.id.inner_content_card, View.GONE)
            rv.setViewVisibility(R.id.container_status, View.GONE)
            rv.setViewVisibility(R.id.container_full_status, View.VISIBLE)
            rv.setTextViewText(R.id.tv_full_status_title, title)

            // 暗色树
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
            // 常规树
            rv.setViewVisibility(R.id.inner_content_card, View.VISIBLE)
            rv.setViewVisibility(R.id.list_view_courses, View.GONE)
            rv.setViewVisibility(R.id.container_status, View.VISIBLE)
            rv.setViewVisibility(R.id.container_full_status, View.GONE)
            rv.setTextViewText(R.id.tv_status_title, title)

            // 暗色树
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
     * 解析主题配置并分别应用于常规与暗色视图控件
     */
    private fun applyWidgetStyle(rv: RemoteViews, snapshot: WidgetSnapshot) {
        val widgetStyle = snapshot.style?.widget_style
        val alphaPercent: Float = widgetStyle?.background_alpha ?: 1.0f
        val fontScale: Float = widgetStyle?.font_scale ?: 1.0f
        val themeMode = widgetStyle?.theme_mode
        val seedColorLong = widgetStyle?.seed_color

        // 计算常规 ID 与 _dark ID 的深色标志
        val isDarkForLightId = when (themeMode) {
            WidgetThemeModeProto.WIDGET_THEME_DARK -> true
            else -> false
        }
        val isDarkForDarkId = when (themeMode) {
            WidgetThemeModeProto.WIDGET_THEME_LIGHT -> false
            else -> true
        }

        // 1. 应用常规亮色布局样式
        applyGroupStyle(
            rv = rv,
            isDarkMode = isDarkForLightId,
            seedColorLong = seedColorLong,
            alphaPercent = alphaPercent,
            fontScale = fontScale,
            cardBgId = R.id.card_bg_image,
            fullStatusBgId = R.id.full_status_bg_image,
            headerTitleId = R.id.tv_header_title,
            currentWeekId = R.id.tv_current_week,
            footerId = R.id.tv_footer,
            statusTitleId = R.id.tv_status_title,
            statusMsgId = R.id.tv_status_msg,
            fullStatusTitleId = R.id.tv_full_status_title,
            fullStatusMsgId = R.id.tv_full_status_msg
        )

        // 2. 应用暗色布局样式
        applyGroupStyle(
            rv = rv,
            isDarkMode = isDarkForDarkId,
            seedColorLong = seedColorLong,
            alphaPercent = alphaPercent,
            fontScale = fontScale,
            cardBgId = R.id.card_bg_image_dark,
            fullStatusBgId = R.id.full_status_bg_image_dark,
            headerTitleId = R.id.tv_header_title_dark,
            currentWeekId = R.id.tv_current_week_dark,
            footerId = R.id.tv_footer_dark,
            statusTitleId = R.id.tv_status_title_dark,
            statusMsgId = R.id.tv_status_msg_dark,
            fullStatusTitleId = R.id.tv_full_status_title_dark,
            fullStatusMsgId = R.id.tv_full_status_msg_dark
        )
    }

    /**
     * 设置组件组样式、颜色及缩放比例
     */
    private fun applyGroupStyle(
        rv: RemoteViews,
        isDarkMode: Boolean,
        seedColorLong: Long?,
        alphaPercent: Float,
        fontScale: Float,
        cardBgId: Int,
        fullStatusBgId: Int,
        headerTitleId: Int,
        currentWeekId: Int,
        footerId: Int,
        statusTitleId: Int,
        statusMsgId: Int,
        fullStatusTitleId: Int,
        fullStatusMsgId: Int
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

        // 正确作用于 XML 中定义的 ImageView 背景元素
        rv.setImageViewBackground(cardBgId, baseBgColor, alphaPercent)
        rv.setImageViewBackground(fullStatusBgId, baseBgColor, alphaPercent)

        // 动态设置文本颜色
        rv.setTextColor(headerTitleId, primaryTextColor)
        rv.setTextColor(currentWeekId, secondaryTextColor)
        rv.setTextColor(footerId, secondaryTextColor)
        rv.setTextColor(statusTitleId, primaryTextColor)
        rv.setTextColor(statusMsgId, secondaryTextColor)
        rv.setTextColor(fullStatusTitleId, primaryTextColor)
        rv.setTextColor(fullStatusMsgId, secondaryTextColor)

        // 基础字体大小定义 (dp)
        val baseHeaderTitleSize = 11f
        val baseCurrentWeekSize = 10f
        val baseFooterSize = 8f
        val baseStatusTitleSize = 16f
        val baseStatusMsgSize = 11f
        val baseFullStatusTitleSize = 18f
        val baseFullStatusMsgSize = 13f

        // 容错处理：确保缩放系数有效
        val scale = if (fontScale > 0f) fontScale else 1.0f

        rv.setTextViewTextSize(headerTitleId, TypedValue.COMPLEX_UNIT_DIP, baseHeaderTitleSize * scale)
        rv.setTextViewTextSize(currentWeekId, TypedValue.COMPLEX_UNIT_DIP, baseCurrentWeekSize * scale)
        rv.setTextViewTextSize(footerId, TypedValue.COMPLEX_UNIT_DIP, baseFooterSize * scale)
        rv.setTextViewTextSize(statusTitleId, TypedValue.COMPLEX_UNIT_DIP, baseStatusTitleSize * scale)
        rv.setTextViewTextSize(statusMsgId, TypedValue.COMPLEX_UNIT_DIP, baseStatusMsgSize * scale)
        rv.setTextViewTextSize(fullStatusTitleId, TypedValue.COMPLEX_UNIT_DIP, baseFullStatusTitleSize * scale)
        rv.setTextViewTextSize(fullStatusMsgId, TypedValue.COMPLEX_UNIT_DIP, baseFullStatusMsgSize * scale)
    }

    /**
     * 设置 ImageView 背景色与透明度（兼容不同 Android 版本）
     */
    private fun RemoteViews.setImageViewBackground(
        bgViewId: Int,
        @ColorInt baseColor: Int,
        @FloatRange(from = 0.0, to = 1.0) alphaPercent: Float
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