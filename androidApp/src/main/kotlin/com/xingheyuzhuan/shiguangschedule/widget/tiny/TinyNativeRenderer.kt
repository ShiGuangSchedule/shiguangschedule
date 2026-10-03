package com.xingheyuzhuan.shiguangschedule.widget.tiny

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
import com.xingheyuzhuan.shiguangschedule.widget.WidgetSnapshot
import java.time.LocalDate
import java.time.LocalTime

/**
 * 迷你原生小组件渲染器
 */
object TinyNativeRenderer {

    /**
     * 渲染并返回小组件的 RemoteViews 视图
     */
    fun render(
        context: Context,
        snapshot: WidgetSnapshot,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    ): RemoteViews {
        val mainRv = RemoteViews(context.packageName, R.layout.widget_tiny_native)

        // 1. 绑定根布局点击事件（点击跳转主界面）
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mainRv.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        mainRv.setOnClickPendingIntent(R.id.widget_root_dark, pendingIntent)

        // 2. 应用小组件主题样式与字体缩放
        applyWidgetStyle(mainRv, snapshot)

        // 3. 解析快照数据并过滤今日剩余课程
        val currentWeek = if (snapshot.current_week <= 0) null else snapshot.current_week
        val todayStr = LocalDate.now().toString()
        val now = LocalTime.now()

        val todayAllCourses = snapshot.courses.filter { it.date == todayStr }
        val remainingCourses = todayAllCourses.filter { course ->
            !course.is_skipped && try {
                LocalTime.parse(course.end_time) > now
            } catch (_: Exception) { true }
        }

        // 4. 根据当前周数与剩余课程分发视图状态
        if (currentWeek != null && remainingCourses.isNotEmpty()) {
            // 显示课程列表视图，隐藏状态提示框
            mainRv.setViewVisibility(R.id.stack_view, View.VISIBLE)
            mainRv.setViewVisibility(R.id.container_status, View.GONE)
            mainRv.setViewVisibility(R.id.stack_view_dark, View.VISIBLE)
            mainRv.setViewVisibility(R.id.container_status_dark, View.GONE)

            val totalRemainingCount = remainingCourses.size
            val displayCourses = remainingCourses.take(20)
            val builder = RemoteViewsCompat.RemoteCollectionItems.Builder()

            // 填充课程卡片集合
            displayCourses.forEachIndexed { index, course ->
                val remainingCount = totalRemainingCount - index
                val cardRv = TinyCourseCardRenderer.buildCourseCard(context, snapshot, course, remainingCount)

                cardRv.setOnClickFillInIntent(R.id.inner_content_card, Intent())
                cardRv.setOnClickFillInIntent(R.id.inner_content_card_dark, Intent())

                val itemId = if (course.id.isNotBlank()) {
                    course.id.hashCode().toLong() and 0x7FFFFFFFFL
                } else {
                    "${course.name}_${course.start_time}".hashCode().toLong() and 0x7FFFFFFFFL
                }
                builder.addItem(itemId, cardRv)
            }

            builder.setHasStableIds(true)
            val collectionItems = builder.build()

            RemoteViewsCompat.setRemoteAdapter(context, mainRv, appWidgetId, R.id.stack_view, collectionItems)
            RemoteViewsCompat.setRemoteAdapter(context, mainRv, appWidgetId, R.id.stack_view_dark, collectionItems)

            mainRv.setEmptyView(R.id.stack_view, R.id.container_status)
            mainRv.setEmptyView(R.id.stack_view_dark, R.id.container_status_dark)

            // 设置列表项点击跳转模板
            val clickIntentTemplate = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntentTemplate = PendingIntent.getActivity(
                context,
                appWidgetId,
                clickIntentTemplate,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            mainRv.setPendingIntentTemplate(R.id.stack_view, pendingIntentTemplate)
            mainRv.setPendingIntentTemplate(R.id.stack_view_dark, pendingIntentTemplate)

        } else {
            // 隐藏课程列表视图，展示状态提示框（无课、假期或开学倒计时）
            mainRv.setViewVisibility(R.id.stack_view, View.GONE)
            mainRv.setViewVisibility(R.id.container_status, View.VISIBLE)
            mainRv.setViewVisibility(R.id.stack_view_dark, View.GONE)
            mainRv.setViewVisibility(R.id.container_status_dark, View.VISIBLE)

            val daysUntilStart = snapshot.days_until_term_start
            val title: String
            val message: String?

            if (daysUntilStart > 0) {
                // 未开学：展示开学倒计时
                title = context.getString(R.string.title_vacation)
                message = context.getString(R.string.widget_days_until_term_start, daysUntilStart)
            } else {
                // 已开学或无设置：根据当前周数及今日课程状态判定提示语
                title = when {
                    currentWeek == null -> context.getString(R.string.title_vacation)
                    todayAllCourses.isEmpty() -> context.getString(R.string.text_no_courses_today)
                    else -> context.getString(R.string.widget_today_courses_finished)
                }
                message = if (currentWeek == null) context.getString(R.string.widget_vacation_expecting) else null
            }

            mainRv.setTextViewText(R.id.tv_status_title, title)
            mainRv.setTextViewText(R.id.tv_status_title_dark, title)

            if (!message.isNullOrBlank()) {
                mainRv.setTextViewText(R.id.tv_status_msg, message)
                mainRv.setViewVisibility(R.id.tv_status_msg, View.VISIBLE)
                mainRv.setTextViewText(R.id.tv_status_msg_dark, message)
                mainRv.setViewVisibility(R.id.tv_status_msg_dark, View.VISIBLE)
            } else {
                mainRv.setViewVisibility(R.id.tv_status_msg, View.GONE)
                mainRv.setViewVisibility(R.id.tv_status_msg_dark, View.GONE)
            }
        }

        return mainRv
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

        val isDarkForLightId = when (themeMode) {
            WidgetThemeModeProto.WIDGET_THEME_DARK -> true
            else -> false
        }
        val isDarkForDarkId = when (themeMode) {
            WidgetThemeModeProto.WIDGET_THEME_LIGHT -> false
            else -> true
        }

        applyStatusGroupStyle(
            rv = rv,
            isDarkMode = isDarkForLightId,
            seedColorLong = seedColorLong,
            alphaPercent = alphaPercent,
            fontScale = fontScale,
            statusBgImgId = R.id.status_bg_image,
            titleId = R.id.tv_status_title,
            msgId = R.id.tv_status_msg
        )

        applyStatusGroupStyle(
            rv = rv,
            isDarkMode = isDarkForDarkId,
            seedColorLong = seedColorLong,
            alphaPercent = alphaPercent,
            fontScale = fontScale,
            statusBgImgId = R.id.status_bg_image_dark,
            titleId = R.id.tv_status_title_dark,
            msgId = R.id.tv_status_msg_dark
        )
    }

    /**
     * 设置状态视图组的背景色、文字颜色与字体缩放
     */
    private fun applyStatusGroupStyle(
        rv: RemoteViews,
        isDarkMode: Boolean,
        seedColorLong: Long?,
        alphaPercent: Float,
        fontScale: Float,
        statusBgImgId: Int,
        titleId: Int,
        msgId: Int
    ) {
        val scheme = if (seedColorLong != null) {
            createMaterialKolorScheme(darkTheme = isDarkMode, seedColor = Color(seedColorLong))
        } else {
            createMaterialKolorScheme(darkTheme = isDarkMode)
        }

        val baseBgColor: Int
        val titleColor: Int
        val msgColor: Int

        if (seedColorLong == null) {
            baseBgColor = scheme.surfaceContainer.toArgb()
            titleColor = scheme.onSurface.toArgb()
            msgColor = scheme.onSurfaceVariant.toArgb()
        } else {
            baseBgColor = scheme.primaryContainer.toArgb()
            val contentColor = scheme.onPrimaryContainer.toArgb()
            titleColor = contentColor
            msgColor = contentColor
        }

        rv.setCardBackground(statusBgImgId, baseBgColor, alphaPercent)
        rv.setTextColor(titleId, titleColor)
        rv.setTextColor(msgId, msgColor)

        val baseTitleSize = 15f
        val baseMsgSize = 11f
        val scale = if (fontScale > 0f) fontScale else 1.0f

        rv.setTextViewTextSize(titleId, TypedValue.COMPLEX_UNIT_DIP, baseTitleSize * scale)
        rv.setTextViewTextSize(msgId, TypedValue.COMPLEX_UNIT_DIP, baseMsgSize * scale)
    }

    /**
     * 设置卡片背景色与透明度（兼容不同 Android 版本）
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