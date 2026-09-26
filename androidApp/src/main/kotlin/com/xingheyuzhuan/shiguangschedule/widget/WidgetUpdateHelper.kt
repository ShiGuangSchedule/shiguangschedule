package com.xingheyuzhuan.shiguangschedule.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.toProto
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import com.xingheyuzhuan.shiguangschedule.widget.compact.CompactNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.compact.CompactNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.double_days.DoubleDaysNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.list_vertical.ListVerticalNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.list_vertical.ListVerticalNativeRenderer
import com.xingheyuzhuan.shiguangschedule.widget.tiny.TinyNativeProvider
import com.xingheyuzhuan.shiguangschedule.widget.tiny.TinyNativeRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// 1. 定义在顶层，避免 "local type aliases" 实验性特性报错
private typealias RenderFunc = (Context, WidgetSnapshot, Int) -> RemoteViews

// 创建一个局部的注入代理中心，用于在全局顶层方法中安全提取注入实例
private object WidgetDependencyContainer : KoinComponent {
    val repository: WidgetRepository by inject()
    val styleSettingsRepository: StyleSettingsRepository by inject()
}

/**
 * 全局防抖管理中心
 * 用于拦截拖动滑块、频繁数据监听引起的密集 updateAppWidget 调用
 */
object WidgetUpdateDebouncer {
    private var updateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * 触发带防抖功能的更新
     * @param delayMillis 触发等待间隔，默认 300ms
     */
    fun requestUpdate(context: Context, delayMillis: Long = 300L) {
        updateJob?.cancel() // 取消未完成的排队任务
        updateJob = scope.launch {
            delay(delayMillis.milliseconds)
            realUpdateAllWidgets(context)
        }
    }
}

/**
 * 小组件统一分发中心对外公开函数
 * 保持兼容原本的顶层调用接口，内部引入防抖逻辑
 */
suspend fun updateAllWidgets(context: Context) {
    WidgetUpdateDebouncer.requestUpdate(context)
}

/**
 * 实际执行全量更新的私有核心方法
 */
private suspend fun realUpdateAllWidgets(context: Context) {
    try {
        // 1. 从 Koin 容器中动态获取单例化的仓库
        val repository = WidgetDependencyContainer.repository
        val styleSettingsRepository = WidgetDependencyContainer.styleSettingsRepository

        // 2. 准备基础数据
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val dbCourses = withTimeoutOrNull(3.seconds) {
            repository.getWidgetCoursesByDateRange(today.toString(), tomorrow.toString()).first()
        } ?: emptyList()

        val currentWeek = withTimeoutOrNull(2.seconds) {
            repository.getCurrentWeekFlow().first()
        } ?: 0

        // 获取小组件设置（包含开学日期和周起始日）
        val appSettings = withTimeoutOrNull(2.seconds) {
            repository.getAppSettingsFlow().first()
        }

        val currentStyle = withTimeoutOrNull(2.seconds) {
            styleSettingsRepository.getStyleOnce()
        }

        val finalStyleToSync = currentStyle?.toProto() ?: ScheduleGridStyle.DEFAULT.toProto()

        // 计算开学倒计时天数
        val semesterStartDateStr = appSettings?.semesterStartDate
        val daysUntilTermStart = if (!semesterStartDateStr.isNullOrEmpty()) {
            try {
                val startDate = LocalDate.parse(semesterStartDateStr)
                java.time.temporal.ChronoUnit.DAYS.between(today, startDate).toInt()
            } catch (_: Exception) {
                0
            }
        } else {
            0
        }

        // 获取周起始日（若拿不到则兜底为 1，即周一）
        val firstDayOfWeek = appSettings?.firstDayOfWeek ?: 1

        // 3. 构造数据快照 (Protobuf)
        val courseProtoList = dbCourses.map { course ->
            WidgetCourseProto(
                id = course.id,
                name = course.name,
                teacher = course.teacher,
                position = course.position,
                start_time = course.startTime,
                end_time = course.endTime,
                color_int = course.colorInt,
                is_skipped = course.isSkipped,
                date = course.date
            )
        }

        val snapshot = WidgetSnapshot(
            current_week = currentWeek,
            style = finalStyleToSync,
            courses = courseProtoList,
            days_until_term_start = daysUntilTermStart,
            first_day_of_week = firstDayOfWeek
        )

        // 4. 定义所有原生尺寸的映射列表
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val nativeConfigs: List<Pair<Class<*>, RenderFunc>> = listOf(
            TinyNativeProvider::class.java to { ctx, snap, id -> TinyNativeRenderer.render(ctx, snap, id) },
            CompactNativeProvider::class.java to { ctx, snap, id -> CompactNativeRenderer.render(ctx, snap, id) },
            DoubleDaysNativeProvider::class.java to { ctx, snap, id -> DoubleDaysNativeRenderer.render(ctx, snap, id) },
            ListVerticalNativeProvider::class.java to { ctx, snap, id -> ListVerticalNativeRenderer.render(ctx, snap, id) }
        )

        // 5. 统一分发更新
        nativeConfigs.forEachIndexed { index, (providerClass, renderFunc) ->
            val componentName = ComponentName(context, providerClass)
            val ids = appWidgetManager.getAppWidgetIds(componentName)

            if (ids.isNotEmpty()) {
                if (index > 0) {
                    delay(300.milliseconds)
                }

                try {
                    // 逐个更新对应的 appWidgetId
                    for (appWidgetId in ids) {
                        val remoteViews = renderFunc(context, snapshot, appWidgetId)
                        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
                    }

                    Log.d("WidgetUpdateHelper", "成功刷新规格 ${providerClass.simpleName}")
                } catch (e: Exception) {
                    Log.e("WidgetUpdateHelper", "规格 ${providerClass.simpleName} 渲染失败", e)
                }
            }
        }

    } catch (e: Exception) {
        Log.e("WidgetUpdateHelper", "更新流程异常: ${e.stackTraceToString()}")
    }
}