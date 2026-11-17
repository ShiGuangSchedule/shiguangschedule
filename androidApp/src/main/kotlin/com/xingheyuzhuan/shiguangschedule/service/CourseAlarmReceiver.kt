package com.xingheyuzhuan.shiguangschedule.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import com.xingheyuzhuan.shiguangschedule.MainActivity
import com.xingheyuzhuan.shiguangschedule.R
import com.xingheyuzhuan.shiguangschedule.data.model.AutoControlMode
import com.xingheyuzhuan.shiguangschedule.data.repository.AndroidAppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 课程闹钟与模式切换广播接收器。
 */
class CourseAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val androidAppSettingsRepository: AndroidAppSettingsRepository by inject()

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "course_notification_channel"
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_COURSE_POSITION = "course_position"
        const val EXTRA_COURSE_TEACHER = "extra_course_teacher"
        const val EXTRA_COURSE_ID = "course_id"

        const val EXTRA_DND_ACTION = "extra_dnd_action"
        const val DND_ACTION_START = "dnd_action_start"
        const val DND_ACTION_END = "dnd_action_end"

        const val EXTRA_ALARM_SLOT_ID = "EXTRA_ALARM_SLOT_ID"
        // 覆盖 50001 (DND) 到 50110 (Course Reminders)
        private const val SLOT_START = 50001
        private const val SLOT_END = 50110

        const val ACTION_DISMISS_NOTIFICATION = "com.xingheyuzhuan.shiguangschedule.ACTION_DISMISS_NOTIFICATION"

        private const val ALARM_IDS_PREFS = "alarm_ids_prefs"
        private const val KEY_ACTIVE_ALARM_IDS = "active_alarm_ids"
        private const val TAG = "CourseAlarmReceiver"

        fun toggleMode(context: Context, enableMode: Boolean, modeType: AutoControlMode) {
            val audioManager = context.getSystemService<AudioManager>()
            val notificationManager = context.getSystemService<NotificationManager>()
            if (audioManager == null || notificationManager == null) return
            if (!notificationManager.isNotificationPolicyAccessGranted) return
            when (modeType) {
                AutoControlMode.OFF -> Unit
                AutoControlMode.DND -> {
                    notificationManager.setInterruptionFilter(
                        if (enableMode) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                        else NotificationManager.INTERRUPTION_FILTER_ALL
                    )
                }
                AutoControlMode.SILENT -> {
                    audioManager.ringerMode = if (enableMode) AudioManager.RINGER_MODE_SILENT
                    else AudioManager.RINGER_MODE_NORMAL
                }
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->
            if (intent?.action == ACTION_DISMISS_NOTIFICATION) {
                val notifyId = intent.getIntExtra("target_notification_id", -1)
                if (notifyId != -1) {
                    val nm = ctx.getSystemService<NotificationManager>()
                    nm?.cancel(notifyId)
                }
                return
            }

            val slotId = intent?.getIntExtra(EXTRA_ALARM_SLOT_ID, -1) ?: -1
            val dndAction = intent?.getStringExtra(EXTRA_DND_ACTION)

            if (intent?.data != null || (slotId !in SLOT_START..SLOT_END && dndAction.isNullOrEmpty())) {
                Log.d(TAG, "已拦截非法或旧版闹钟。")
                return
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val androidSettings = androidAppSettingsRepository.getSettings().first()
                    val modeToUse = androidSettings.autoControlMode

                    val isCompatMode =
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA ||
                                androidSettings.compatWearableSync

                    if (!dndAction.isNullOrEmpty()) {
                        when (dndAction) {
                            DND_ACTION_START -> toggleMode(ctx, true, modeToUse)
                            DND_ACTION_END -> {
                                toggleMode(ctx, false, modeToUse)
                                DndSchedulerWorker.enqueueWork(ctx)
                            }
                        }
                    } else {
                        val courseName = intent?.getStringExtra(EXTRA_COURSE_NAME).takeUnless { it.isNullOrEmpty() }
                            ?: ctx.getString(R.string.notification_unknown_course)

                        val position = intent?.getStringExtra(EXTRA_COURSE_POSITION).takeUnless { it.isNullOrEmpty() }
                            ?: ctx.getString(R.string.notification_unknown_position)
                        val teacher = intent?.getStringExtra(EXTRA_COURSE_TEACHER) ?: ""
                        val courseIdString = intent?.getStringExtra(EXTRA_COURSE_ID)

                        if (!courseIdString.isNullOrEmpty()) {
                            showNotification(ctx, slotId, courseName, position, teacher, isCompatMode)
                            removeAlarmIdFromPrefs(ctx, courseIdString)
                            updateAllWidgets(ctx)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onReceive 异常", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        name: String,
        position: String,
        teacher: String,
        isCompatMode: Boolean
    ) {
        val nm = context.getSystemService<NotificationManager>() ?: return

        val alertTitle = context.getString(R.string.notification_title_course_alert)
        val closeActionText = context.getString(R.string.action_close)
        val liveStatusText = context.getString(R.string.notification_live_status_preparing)

        val formattedPosition = context.getString(R.string.course_position_prefix, position)
        val formattedTeacher = if (teacher.isNotBlank()) {
            context.getString(R.string.course_teacher_prefix, teacher)
        } else null

        val bigTextContent = if (formattedTeacher != null) {
            "$formattedPosition\n$formattedTeacher"
        } else {
            formattedPosition
        }

        if (nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.item_course_reminder),
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val dismissIntent = Intent(context, CourseAlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_NOTIFICATION
            putExtra("target_notification_id", notificationId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                identifier = notificationId.toString()
            }
        }
        val dismissPI = PendingIntent.getBroadcast(
            context, notificationId, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(name)
            .bigText(bigTextContent)

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setContentTitle(name)
            .setContentText(formattedPosition)
            .setSubText(alertTitle)
            .setStyle(bigTextStyle)

            // 非兼容模式应用
            .setOngoing(!isCompatMode)
            .setAutoCancel(isCompatMode)

            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setShowWhen(true)
            .addAction(0, closeActionText, dismissPI)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

        // Android 16 实时更新特性
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder.setRequestPromotedOngoing(true)
            builder.setShortCriticalText(liveStatusText)
        }

        nm.notify(notificationId, builder.build())
    }

    private fun removeAlarmIdFromPrefs(context: Context, courseId: String) {
        val sp = context.getSharedPreferences(ALARM_IDS_PREFS, Context.MODE_PRIVATE)
        val currentIds = sp.getStringSet(KEY_ACTIVE_ALARM_IDS, null)?.toMutableSet()
        if (currentIds != null) {
            currentIds.remove(courseId)
            sp.edit { putStringSet(KEY_ACTIVE_ALARM_IDS, currentIds) }
        }
    }
}