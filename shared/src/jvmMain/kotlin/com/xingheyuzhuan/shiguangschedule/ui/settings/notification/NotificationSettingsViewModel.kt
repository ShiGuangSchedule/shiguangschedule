package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.seconds

/**
 * 通知设置页 UI 状态。
 */
data class NotificationUiState(
    val appSettings: AppSettingsModel = AppSettingsModel(),
    val isReady: Boolean = false
)

@KoinViewModel
class NotificationSettingsViewModel(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    // 映射全局设置数据流，保持单一数据源
    val uiState: StateFlow<NotificationUiState> = appSettingsRepository
        .getAppSettings()
        .map { settings ->
            NotificationUiState(appSettings = settings, isReady = true)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = NotificationUiState()
        )

    /**
     * 更新课程提醒的总开关状态。
     */
    fun onReminderEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = uiState.value.appSettings
            appSettingsRepository.insertOrUpdateAppSettings(
                currentSettings.copy(reminderEnabled = enabled)
            )
        }
    }

    /**
     * 更新课程提醒的提前时间（单位：分钟）。
     */
    fun onRemindBeforeMinutesChanged(minutes: Int) {
        viewModelScope.launch {
            val currentSettings = uiState.value.appSettings
            appSettingsRepository.insertOrUpdateAppSettings(
                currentSettings.copy(remindBeforeMinutes = minutes)
            )
        }
    }
}