package com.xingheyuzhuan.shiguangschedule.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.xingheyuzhuan.shiguangschedule.data.model.AutoControlMode
import com.xingheyuzhuan.shiguangschedule.data.repository.AndroidAppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import kotlinx.coroutines.flow.first
import org.koin.android.annotation.KoinWorker
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * WorkManager 用于调度上课时行为模式（勿扰/静音）开启/关闭闹钟的 Worker。
 */
@KoinWorker
class DndSchedulerWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val androidAppSettingsRepository: AndroidAppSettingsRepository,
    private val widgetRepository: WidgetRepository
) : CoroutineWorker(appContext, workerParams) {

    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = appContext.getSystemService<NotificationManager>()
        ?: throw IllegalStateException("NotificationManager not available")

    companion object {
        const val DND_SCHEDULER_WORK_TAG = "dnd_scheduler_worker_tag"
        private const val TAG = "DndSchedulerWorker"
        private const val DND_SCHEDULER_CHECK_DAYS = 7L
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)

        // 50000 作为基准 ID
        private const val DND_ALARM_ID_BASE = 50000

        fun enqueueWork(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<DndSchedulerWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .addTag(DND_SCHEDULER_WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                DND_SCHEDULER_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            Log.d(TAG, "上课时自动模式调度器已重新排队。")
        }
    }

    override suspend fun doWork(): Result {
        val androidSettings = androidAppSettingsRepository.getSettings().first()

        if (androidSettings.autoControlMode == AutoControlMode.OFF) {
            Log.d(TAG, "上课时自动模式已关闭，取消所有模式闹钟。")
            cancelDndAlarms()
            return Result.success()
        }

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Log.w(TAG, "勿扰模式权限未授予")
        }

        val shouldBeModeOn = isCurrentlyInDndTime()
        Log.d(TAG, "当前模式状态校准：$shouldBeModeOn")

        // 调度前先清理旧闹钟
        cancelDndAlarms()

        val (nextStartAlarm, nextEndAlarm) = findNextDndAlarmTimes(DND_SCHEDULER_CHECK_DAYS)

        if (nextStartAlarm == null && nextEndAlarm == null) {
            return Result.success()
        }

        nextStartAlarm?.let { scheduleDndAlarm(it, isStartAction = true) }
        nextEndAlarm?.let { scheduleDndAlarm(it, isStartAction = false) }

        return Result.success()
    }

    private suspend fun findNextDndAlarmTimes(checkDays: Long): Pair<LocalDateTime?, LocalDateTime?> {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val startDate = now.toLocalDate().format(DATE_FORMATTER)
        val endDate = now.toLocalDate().plusDays(checkDays - 1).format(DATE_FORMATTER)

        val courses = widgetRepository.getWidgetCoursesByDateRange(startDate, endDate).first()
            .filter { !it.isSkipped }

        var nextStartAlarm: LocalDateTime? = null
        var nextEndAlarm: LocalDateTime? = null

        for (course in courses) {
            val courseDate = LocalDate.parse(course.date, DATE_FORMATTER)
            val startDateTime = courseDate.atTime(LocalTime.parse(course.startTime))
            val endDateTime = courseDate.atTime(LocalTime.parse(course.endTime))

            if (startDateTime.isAfter(now)) {
                if (nextStartAlarm == null || startDateTime.isBefore(nextStartAlarm)) nextStartAlarm = startDateTime
            }
            if (endDateTime.isAfter(now)) {
                if (nextEndAlarm == null || endDateTime.isBefore(nextEndAlarm)) nextEndAlarm = endDateTime
            }
        }
        return Pair(nextStartAlarm, nextEndAlarm)
    }

    private suspend fun isCurrentlyInDndTime(): Boolean {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val todayStr = now.toLocalDate().format(DATE_FORMATTER)
        val courses = widgetRepository.getWidgetCoursesByDateRange(todayStr, todayStr).first()
            .filter { !it.isSkipped }
        val nowTime = now.toLocalTime()
        for (course in courses) {
            val startTime = LocalTime.parse(course.startTime)
            val endTime = LocalTime.parse(course.endTime)
            if (!nowTime.isBefore(startTime) && nowTime.isBefore(endTime)) return true
        }
        return false
    }

    /**
     * 设置一个精确的模式闹钟。
     */
    private fun scheduleDndAlarm(dateTime: LocalDateTime, isStartAction: Boolean) {
        val triggerTimeMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // 50001 为开启，50002 为关闭
        val dndActionValue = if (isStartAction) CourseAlarmReceiver.DND_ACTION_START else CourseAlarmReceiver.DND_ACTION_END
        val requestCode = DND_ALARM_ID_BASE + if (isStartAction) 1 else 2

        val intent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
            this.action = "com.xingheyuzhuan.shiguangschedule.ACTION_COURSE_REMIND"
            putExtra(CourseAlarmReceiver.EXTRA_DND_ACTION, dndActionValue)
            putExtra(CourseAlarmReceiver.EXTRA_ALARM_SLOT_ID, requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "无法设置精确闹钟：缺少权限")
                return
            }
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        Log.i(TAG, "已调度模式闹钟 [${if(isStartAction) "开启" else "关闭"}]: $dateTime (ID: $requestCode)")
    }

    /**
     * 取消所有由模式 Scheduler 设置的闹钟。
     */
    private fun cancelDndAlarms() {
        // 循环注销 50001 (开启) 和 50002 (关闭)
        listOf(1, 2).forEach { offset ->
            val requestCode = DND_ALARM_ID_BASE + offset
            val intent = Intent(applicationContext, CourseAlarmReceiver::class.java).apply {
                this.action = "com.xingheyuzhuan.shiguangschedule.ACTION_COURSE_REMIND"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
                val label = if (offset == 1) "开启" else "关闭"
                Log.d(TAG, "已从系统注销模式[$label]闹钟，ID: $requestCode")
            }
        }
    }
}