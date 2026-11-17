package com.xingheyuzhuan.shiguangschedule.data.sync

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.xingheyuzhuan.shiguangschedule.data.repository.AndroidAppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.service.CourseNotificationWorker
import com.xingheyuzhuan.shiguangschedule.service.DndSchedulerWorker
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.milliseconds

/**
 * 平台层同步管理器（Android 端）。
 * 仅负责响应共享层 (KMP) 的同步完成信号、Android 本地样式更新以及 Android 专属配置变更，
 * 执行系统 Widget 刷新及 WorkManager 调度。
 */
@OptIn(FlowPreview::class)
@Single(createdAtStart = true)
class SyncManager(
    private val appContext: Context,
    private val widgetDataSynchronizer: WidgetDataSynchronizer,
    private val styleSettingsRepository: StyleSettingsRepository,
    private val androidAppSettingsRepository: AndroidAppSettingsRepository
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // 1. 监听 KMP 共享层的同步完成信号（课表变更与公共提醒设置变更）
        widgetDataSynchronizer.syncCompletedFlow
            .debounce(300L.milliseconds)
            .onEach {
                Log.d("SyncManager", "收到共享层同步完成通知，正在调度 Worker 任务及刷新小组件...")
                triggerNotificationWorker()
                DndSchedulerWorker.enqueueWork(appContext)
                updateAllWidgets(appContext)
            }
            .launchIn(scope)

        // 2. 监听 Android 端专属的样式更新事件
        styleSettingsRepository.styleUpdatedFlow
            .debounce(300L.milliseconds)
            .onEach {
                Log.d("SyncManager", "收到样式更改通知，正在刷新小组件...")
                updateAllWidgets(appContext)
            }
            .launchIn(scope)

        // 3. 监听 Android 平台专属配置中与 DND / 通知调度相关的字段
        androidAppSettingsRepository.getSettings()
            .map { settings ->
                settings.autoControlMode to settings.compatWearableSync
            }
            .distinctUntilChanged()
            .debounce(300L.milliseconds)
            .onEach {
                Log.d("SyncManager", "收到 Android 专属配置变更，正在重新调度 DND 与通知任务...")
                DndSchedulerWorker.enqueueWork(appContext)
                triggerNotificationWorker()
            }
            .launchIn(scope)

        Log.d("SyncManager", "Android 平台同步调度器初始化完毕。")
    }

    private fun triggerNotificationWorker() {
        val workRequest = OneTimeWorkRequestBuilder<CourseNotificationWorker>().build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "CourseNotificationWorker_Sync_Update",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}