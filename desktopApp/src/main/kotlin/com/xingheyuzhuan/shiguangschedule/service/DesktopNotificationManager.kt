package com.xingheyuzhuan.shiguangschedule.service

import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import com.xingheyuzhuan.shiguangschedule.data.sync.WidgetDataSynchronizer
import dev.nucleusframework.notification.common.NotificationManager
import dev.nucleusframework.notification.common.NotificationResult
import dev.nucleusframework.notification.common.notification
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Single
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.course_position_prefix
import shiguangschedule.shared.generated.resources.course_teacher_prefix
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds

@Single(createdAtStart = true)
class DesktopNotificationManager(
    private val widgetDataSynchronizer: WidgetDataSynchronizer,
    private val appSettingsRepository: AppSettingsRepository,
    private val widgetRepository: WidgetRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val scheduledJobs = ConcurrentHashMap<String, Job>()
    private val logger = Logger.getLogger("DesktopNotificationManager")

    init {
        NotificationManager.initialize()

        if (!NotificationManager.isAvailable()) {
            logger.warning("通知子系统不可用 (isAvailable=false)")
        }

        widgetDataSynchronizer.syncCompletedFlow
            .onEach {
                scheduleNotifications()
            }
            .launchIn(scope)
    }

    private fun scheduleNotifications() {
        scope.launch {
            try {
                val appSettings = appSettingsRepository.getAppSettings().first()
                if (!appSettings.reminderEnabled) {
                    cancelAllJobs()
                    return@launch
                }

                val remindBeforeMinutes = appSettings.remindBeforeMinutes.toLong()
                val now = LocalDateTime.now()
                val startDate = now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val endDate = now.toLocalDate().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE)

                val courses = widgetRepository.getWidgetCoursesByDateRange(startDate, endDate).first()
                    .filter { !it.isSkipped }

                cancelAllJobs()

                for (course in courses) {
                    val courseDate = LocalDate.parse(course.date)
                    val startDateTime = LocalDateTime.of(courseDate, LocalTime.parse(course.startTime))
                    val remindDateTime = startDateTime.minusMinutes(remindBeforeMinutes)

                    val delayMillis = ChronoUnit.MILLIS.between(now, remindDateTime)
                    if (delayMillis > 0) {
                        val job = scope.launch {
                            delay(delayMillis.milliseconds)
                            sendCourseNotification(course.name, course.position, course.teacher)
                        }
                        scheduledJobs[course.id] = job
                    }
                }
            } catch (e: Exception) {
                logger.severe("调度桌面通知时发生异常: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun sendCourseNotification(name: String, position: String, teacher: String) {
        try {
            if (!NotificationManager.isAvailable()) {
                logger.warning("通知子系统不可用，跳过发送课程通知：$name")
                return
            }

            val positionText = getString(Res.string.course_position_prefix, position)
            val teacherText = if (teacher.isNotBlank()) getString(Res.string.course_teacher_prefix, teacher) else null

            val messageText = if (teacherText != null) "$positionText\n$teacherText" else positionText

            val n = notification(
                title = name,
                message = messageText,
                onFailed = {
                    logger.severe("课程通知显示失败: $name")
                }
            )

            when (val result = n.send()) {
                is NotificationResult.Success -> Unit
                is NotificationResult.Failure -> {
                    logger.severe("桌面通知发送失败: $name, 原因=${result.reason}")
                }
            }
        } catch (e: Exception) {
            logger.severe("发送课程通知时发生异常: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun cancelAllJobs() {
        scheduledJobs.values.forEach { it.cancel() }
        scheduledJobs.clear()
    }
}