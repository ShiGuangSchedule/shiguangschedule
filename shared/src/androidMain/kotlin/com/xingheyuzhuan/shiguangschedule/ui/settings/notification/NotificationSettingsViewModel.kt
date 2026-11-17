package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.model.AndroidAppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.AutoControlMode
import com.xingheyuzhuan.shiguangschedule.data.repository.AndroidAppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

/**
 * 权限检查结果。
 */
data class PermissionsState(
    val hasExactAlarmPermission: Boolean = false,
    val hasDndPermission: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val canPostPromotedNotifications: Boolean = false
)

data class NotificationSettingsUiState(
    val appSettings: AppSettingsModel = AppSettingsModel(),
    val androidSettings: AndroidAppSettingsModel = AndroidAppSettingsModel(),
    val isReady: Boolean = false
)

@KoinViewModel
class NotificationSettingsViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val androidAppSettingsRepository: AndroidAppSettingsRepository,
    private val context: Context
) : ViewModel() {

    val uiState: StateFlow<NotificationSettingsUiState> = combine(
        appSettingsRepository.getAppSettings(),
        androidAppSettingsRepository.getSettings()
    ) { appSettings, androidSettings ->
        NotificationSettingsUiState(
            appSettings = appSettings,
            androidSettings = androidSettings,
            isReady = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = NotificationSettingsUiState()
    )

    // 独立维护的权限状态，供 UI 监听
    private val _permissionsState = MutableStateFlow(PermissionsState())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()

    /**
     * 刷新系统的权限状态（精确闹钟、勿扰、电池优化、实时动态）。
     * 建议在 UI 层的 ON_RESUME 时调用。
     */
    fun refreshPermissions() {
        viewModelScope.launch {
            val exactAlarm = checkExactAlarmPermission()
            val dnd = checkDndPermission()
            val battery = checkBatteryOptimization()
            val promoted = checkPromotedNotificationPermission()

            _permissionsState.value = PermissionsState(
                hasExactAlarmPermission = exactAlarm,
                hasDndPermission = dnd,
                isIgnoringBatteryOptimizations = battery,
                canPostPromotedNotifications = promoted
            )
        }
    }

    private fun checkExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 12 以下不需要单独申请精确闹钟权限
        }
    }

    private fun checkDndPermission(): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    private fun checkBatteryOptimization(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun checkPromotedNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) { // API 36 (Android 16)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.canPostPromotedNotifications()
        } else {
            false
        }
    }

    // 公共设置
    fun onReminderEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = uiState.value.appSettings
            appSettingsRepository.insertOrUpdateAppSettings(
                currentSettings.copy(reminderEnabled = enabled)
            )
        }
    }

    fun onRemindBeforeMinutesChanged(minutes: Int) {
        viewModelScope.launch {
            val currentSettings = uiState.value.appSettings
            appSettingsRepository.insertOrUpdateAppSettings(
                currentSettings.copy(remindBeforeMinutes = minutes)
            )
        }
    }

    // Android 专属设置
    fun onAutoControlModeChanged(mode: AutoControlMode) {
        viewModelScope.launch {
            androidAppSettingsRepository.updateSettings {
                it.copy(autoControlMode = mode)
            }
        }
    }

    fun onCompatWearableSyncChanged(enabled: Boolean) {
        viewModelScope.launch {
            androidAppSettingsRepository.updateSettings {
                it.copy(compatWearableSync = enabled)
            }
        }
    }
}