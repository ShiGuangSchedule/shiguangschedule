package com.xingheyuzhuan.shiguangschedule.widget.double_days

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
 * 双日并排原生小组件渲染器
 */
object DoubleDaysNativeRenderer {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd")

    /**
     * 渲染并返回双日小组件的 RemoteViews 视图
     */
    fun render(
        context: Context,
        snapshot: WidgetSnapshot,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_double_days_native)

        // 1. 彻底重置状态（同时重置常规与暗色 View 树）
        resetWidgetState(rv)

        // 2. 根布局点击跳转应用主界面（双树绑定）
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

        // 4. 假期/未开学处理（支持开学倒计时）
        val currentWeek = if (snapshot.current_week <= 0) null else snapshot.current_week

        if (currentWeek == null) {
            val daysUntilStart = snapshot.days_until_term_start
            val title = context.getString(R.string.title_vacation)
            val msg = if (daysUntilStart > 0) {
                context.getString(R.string.widget_days_until_term_start, daysUntilStart)
            } else {
                context.getString(R.string.widget_vacation_expecting)
            }

            // 常规树
            rv.setViewVisibility(R.id.inner_content_card, View.GONE)
            rv.setViewVisibility(R.id.container_full_status, View.VISIBLE)
            rv.setTextViewText(R.id.tv_full_status_title, title)
            rv.setTextViewText(R.id.tv_full_status_msg, msg)

            // 暗色树
            rv.setViewVisibility(R.id.inner_content_card_dark, View.GONE)
            rv.setViewVisibility(R.id.container_full_status_dark, View.VISIBLE)
            rv.setTextViewText(R.id.tv_full_status_title_dark, title)
            rv.setTextViewText(R.id.tv_full_status_msg_dark, msg)

            return rv
        }

        // 显示双列卡片内容
        rv.setViewVisibility(R.id.inner_content_card, View.VISIBLE)
        rv.setViewVisibility(R.id.container_full_status, View.GONE)
        rv.setViewVisibility(R.id.inner_content_card_dark, View.VISIBLE)
        rv.setViewVisibility(R.id.container_full_status_dark, View.GONE)

        // 5. 数据准备
        val now = LocalTime.now()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val allCourses = snapshot.courses
        val hideDate = snapshot.style?.widget_style?.hide_date ?: false

        // 渲染左侧：今日课程
        val todayCourses = allCourses.filter { it.date == today.toString() || it.date.isBlank() }
        val remainingToday = todayCourses.filter {
            !it.is_skipped && try { LocalTime.parse(it.end_time) > now } catch (_: Exception) { true }
        }.sortedBy { it.start_time }

        renderColumn(
            context = context,
            rv = rv,
            appWidgetId = appWidgetId,
            listViewId = R.id.list_view_today,
            listViewDarkId = R.id.list_view_today_dark,
            titleId = R.id.tv_today_date,
            titleDarkId = R.id.tv_today_date_dark,
            footerId = R.id.tv_today_footer,
            footerDarkId = R.id.tv_today_footer_dark,
            emptyContainerId = R.id.empty_today_container,
            emptyContainerDarkId = R.id.empty_today_container_dark,
            emptyTextId = R.id.empty_today,
            emptyTextDarkId = R.id.empty_today_dark,
            date = today,
            displayCourses = remainingToday,
            currentWeek = currentWeek,
            isToday = true,
            hideDate = hideDate,
            snapshot = snapshot
        )

        // 渲染右侧：明日课程
        val tomorrowCourses = allCourses.filter { it.date == tomorrow.toString() }
        val effectiveTomorrow = tomorrowCourses.filter { !it.is_skipped }.sortedBy { it.start_time }

        renderColumn(
            context = context,
            rv = rv,
            appWidgetId = appWidgetId,
            listViewId = R.id.list_view_tomorrow,
            listViewDarkId = R.id.list_view_tomorrow_dark,
            titleId = R.id.tv_tomorrow_date,
            titleDarkId = R.id.tv_tomorrow_date_dark,
            footerId = R.id.tv_tomorrow_footer,
            footerDarkId = R.id.tv_tomorrow_footer_dark,
            emptyContainerId = R.id.empty_tomorrow_container,
            emptyContainerDarkId = R.id.empty_tomorrow_container_dark,
            emptyTextId = R.id.empty_tomorrow,
            emptyTextDarkId = R.id.empty_tomorrow_dark,
            date = tomorrow,
            displayCourses = effectiveTomorrow,
            currentWeek = currentWeek,
            isToday = false,
            hideDate = hideDate,
            snapshot = snapshot
        )

        return rv
    }

    /**
     * 重置小组件内部双树视图的可见性状态
     */
    private fun resetWidgetState(rv: RemoteViews) {
        // 常规树重置
        rv.setViewVisibility(R.id.inner_content_card, View.VISIBLE)
        rv.setViewVisibility(R.id.container_full_status, View.GONE)
        rv.setViewVisibility(R.id.empty_today_container, View.GONE)
        rv.setViewVisibility(R.id.empty_tomorrow_container, View.GONE)

        // 暗色树重置
        rv.setViewVisibility(R.id.inner_content_card_dark, View.VISIBLE)
        rv.setViewVisibility(R.id.container_full_status_dark, View.GONE)
        rv.setViewVisibility(R.id.empty_today_container_dark, View.GONE)
        rv.setViewVisibility(R.id.empty_tomorrow_container_dark, View.GONE)
    }

    /**
     * 渲染单侧（今日或明日）课程列
     */
    private fun renderColumn(
        context: Context,
        rv: RemoteViews,
        appWidgetId: Int,
        listViewId: Int,
        listViewDarkId: Int,
        titleId: Int,
        titleDarkId: Int,
        footerId: Int,
        footerDarkId: Int,
        emptyContainerId: Int,
        emptyContainerDarkId: Int,
        emptyTextId: Int,
        emptyTextDarkId: Int,
        date: LocalDate,
        displayCourses: List<WidgetCourseProto>,
        currentWeek: Int,
        isToday: Boolean,
        hideDate: Boolean,
        snapshot: WidgetSnapshot
    ) {
        // 1. 标题拼接（包含前缀、日期、周几和第x周）
        val prefix = if (isToday) {
            context.getString(R.string.widget_title_today)
        } else {
            context.getString(R.string.widget_title_tomorrow)
        }
        val weekDaysArray = context.resources.getStringArray(R.array.week_days_names)
        val dayOfWeekStr = weekDaysArray[date.dayOfWeek.value - 1]

        // 使用快照中的周起始日进行精准跨周判定（明日模式下到达周起始日时周数 +1）
        val firstDayOfWeekInt = if (snapshot.first_day_of_week in 1..7) snapshot.first_day_of_week else 1
        val firstDayOfWeek = DayOfWeek.of(firstDayOfWeekInt)

        val displayWeek = if (!isToday && date.dayOfWeek == firstDayOfWeek) {
            currentWeek + 1
        } else {
            currentWeek
        }
        val weekStr = context.getString(R.string.status_current_week_format, displayWeek)

        // 仅隐藏 MM.dd（例如：1.07）
        val titleText = if (hideDate) {
            "$prefix $dayOfWeekStr $weekStr"
        } else {
            "$prefix ${date.format(DATE_FORMATTER)} $dayOfWeekStr $weekStr"
        }

        rv.setTextViewText(titleId, titleText)
        rv.setTextViewText(titleDarkId, titleText)

        // 2. 空视图与列表渲染切换
        if (displayCourses.isEmpty()) {
            rv.setViewVisibility(listViewId, View.GONE)
            rv.setViewVisibility(listViewDarkId, View.GONE)
            rv.setViewVisibility(footerId, View.GONE)
            rv.setViewVisibility(footerDarkId, View.GONE)

            rv.setViewVisibility(emptyContainerId, View.VISIBLE)
            rv.setViewVisibility(emptyContainerDarkId, View.VISIBLE)

            val noCourseText = context.getString(R.string.text_no_course)
            rv.setTextViewText(emptyTextId, noCourseText)
            rv.setTextViewText(emptyTextDarkId, noCourseText)
        } else {
            rv.setViewVisibility(listViewId, View.VISIBLE)
            rv.setViewVisibility(listViewDarkId, View.VISIBLE)
            rv.setViewVisibility(footerId, View.VISIBLE)
            rv.setViewVisibility(footerDarkId, View.VISIBLE)

            rv.setViewVisibility(emptyContainerId, View.GONE)
            rv.setViewVisibility(emptyContainerDarkId, View.GONE)

            // 设置尾部统计文本：今日为“剩余”，明日为“共有”
            val countRes = if (isToday) R.string.widget_course_remaining_count else R.string.widget_course_total_count
            val countStr = context.getString(countRes, displayCourses.size)
            rv.setTextViewText(footerId, countStr)
            rv.setTextViewText(footerDarkId, countStr)

            // 3. 构建 RemoteCollectionItems 并绑定至双侧 ListView
            val builder = RemoteViewsCompat.RemoteCollectionItems.Builder()

            displayCourses.forEach { course ->
                val itemRv = CourseItemRenderer.createCourseItemView(context, snapshot, course)

                itemRv.setOnClickFillInIntent(R.id.item_root_card, Intent())
                itemRv.setOnClickFillInIntent(R.id.item_root_card_dark, Intent())

                val itemId = if (course.id.isNotBlank()) {
                    course.id.hashCode().toLong() and 0x7FFFFFFFFL
                } else {
                    "${course.name}_${course.start_time}".hashCode().toLong() and 0x7FFFFFFFFL
                }
                builder.addItem(itemId, itemRv)
            }

            builder.setHasStableIds(true)
            val collectionItems = builder.build()

            RemoteViewsCompat.setRemoteAdapter(context, rv, appWidgetId, listViewId, collectionItems)
            RemoteViewsCompat.setRemoteAdapter(context, rv, appWidgetId, listViewDarkId, collectionItems)

            // 4. 点击 PendingIntent 模板设置
            val clickIntentTemplate = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntentTemplate = PendingIntent.getActivity(
                context,
                appWidgetId,
                clickIntentTemplate,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            rv.setPendingIntentTemplate(listViewId, pendingIntentTemplate)
            rv.setPendingIntentTemplate(listViewDarkId, pendingIntentTemplate)
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
            todayDateId = R.id.tv_today_date,
            todayFooterId = R.id.tv_today_footer,
            tomorrowDateId = R.id.tv_tomorrow_date,
            tomorrowFooterId = R.id.tv_tomorrow_footer,
            emptyTodayId = R.id.empty_today,
            emptyTomorrowId = R.id.empty_tomorrow,
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
            todayDateId = R.id.tv_today_date_dark,
            todayFooterId = R.id.tv_today_footer_dark,
            tomorrowDateId = R.id.tv_tomorrow_date_dark,
            tomorrowFooterId = R.id.tv_tomorrow_footer_dark,
            emptyTodayId = R.id.empty_today_dark,
            emptyTomorrowId = R.id.empty_tomorrow_dark,
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
        todayDateId: Int,
        todayFooterId: Int,
        tomorrowDateId: Int,
        tomorrowFooterId: Int,
        emptyTodayId: Int,
        emptyTomorrowId: Int,
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

        // 背景着色与透明度控制
        rv.setImageViewBackground(cardBgId, baseBgColor, alphaPercent)
        rv.setImageViewBackground(fullStatusBgId, baseBgColor, alphaPercent)

        // 设置文本颜色
        rv.setTextColor(todayDateId, secondaryTextColor)
        rv.setTextColor(todayFooterId, secondaryTextColor)
        rv.setTextColor(tomorrowDateId, secondaryTextColor)
        rv.setTextColor(tomorrowFooterId, secondaryTextColor)
        rv.setTextColor(emptyTodayId, secondaryTextColor)
        rv.setTextColor(emptyTomorrowId, secondaryTextColor)
        rv.setTextColor(fullStatusTitleId, primaryTextColor)
        rv.setTextColor(fullStatusMsgId, secondaryTextColor)

        // 基础字体大小定义 (dp)
        val baseDateSize = 10f
        val baseFooterSize = 8f
        val baseEmptySize = 12f
        val baseFullStatusTitleSize = 18f
        val baseFullStatusMsgSize = 13f

        val effectiveScale = if (fontScale > 0f) fontScale else 1.0f

        rv.setTextViewTextSize(todayDateId, TypedValue.COMPLEX_UNIT_DIP, baseDateSize * effectiveScale)
        rv.setTextViewTextSize(todayFooterId, TypedValue.COMPLEX_UNIT_DIP, baseFooterSize * effectiveScale)
        rv.setTextViewTextSize(tomorrowDateId, TypedValue.COMPLEX_UNIT_DIP, baseDateSize * effectiveScale)
        rv.setTextViewTextSize(tomorrowFooterId, TypedValue.COMPLEX_UNIT_DIP, baseFooterSize * effectiveScale)
        rv.setTextViewTextSize(emptyTodayId, TypedValue.COMPLEX_UNIT_DIP, baseEmptySize * effectiveScale)
        rv.setTextViewTextSize(emptyTomorrowId, TypedValue.COMPLEX_UNIT_DIP, baseEmptySize * effectiveScale)
        rv.setTextViewTextSize(fullStatusTitleId, TypedValue.COMPLEX_UNIT_DIP, baseFullStatusTitleSize * effectiveScale)
        rv.setTextViewTextSize(fullStatusMsgId, TypedValue.COMPLEX_UNIT_DIP, baseFullStatusMsgSize * effectiveScale)
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