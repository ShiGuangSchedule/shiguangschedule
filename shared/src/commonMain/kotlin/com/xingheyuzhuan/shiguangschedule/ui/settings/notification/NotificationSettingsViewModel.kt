package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.model.AutoControlMode
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * 通知设置页面弹窗状态
 */
sealed interface NotificationDialogType {
    data object None : NotificationDialogType
    data object EditRemindMinutes : NotificationDialogType
    data object AutoModeSelection : NotificationDialogType
}

/**
 * 通知设置页面 UI 状态
 *
 * @property reminderEnabled 课程提醒开关
 * @property remindBeforeMinutes 提前提醒分钟数
 * @property isLoading 是否正在加载或导入数据
 * @property exactAlarmStatus 系统精确闹钟权限允许状态
 * @property dndPermissionStatus 系统勿扰权限允许状态
 * @property autoModeEnabled 自动模式（勿扰/静音）开关
 * @property autoControlMode 自动控制模式类型
 * @property compatWearableSync 穿戴设备兼容同步开关
 * @property activeDialog 当前展示的弹窗类型
 */
data class NotificationSettingsUiState(
    val reminderEnabled: Boolean = false,
    val remindBeforeMinutes: Int = 15,
    val isLoading: Boolean = false,
    val exactAlarmStatus: Boolean = false,
    val dndPermissionStatus: Boolean = false,
    val autoModeEnabled: Boolean = false,
    val autoControlMode: AutoControlMode = AutoControlMode.DND,
    val compatWearableSync: Boolean = false,
    val activeDialog: NotificationDialogType = NotificationDialogType.None
)

/**
 * 通知设置 ViewModel
 *
 * 负责管理通知设置页面的 UI 状态及与配置存储库的数据持久化交互。
 */
@KoinViewModel
class NotificationSettingsViewModel(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * 加载本地通知设置配置
     */
    private fun loadSettings() {
        viewModelScope.launch {
            appSettingsRepository.getAppSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(
                    reminderEnabled = settings.reminderEnabled,
                    remindBeforeMinutes = settings.remindBeforeMinutes,
                    autoModeEnabled = settings.autoModeEnabled,
                    autoControlMode = settings.autoControlMode,
                    compatWearableSync = settings.compatWearableSync
                )
            }
        }
    }

    /**
     * 显示指定的弹窗
     */
    fun showDialog(type: NotificationDialogType) {
        _uiState.value = _uiState.value.copy(activeDialog = type)
    }

    /**
     * 关闭当前弹窗
     */
    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(activeDialog = NotificationDialogType.None)
    }

    /**
     * 更新精确闹钟权限允许状态
     */
    fun updateExactAlarmStatus(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(exactAlarmStatus = hasPermission)
    }

    /**
     * 更新勿扰权限允许状态
     */
    fun updateDndPermissionStatus(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(dndPermissionStatus = hasPermission)
    }

    /**
     * 更新课程提醒开关状态
     */
    fun updateReminderEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettings().first()
            appSettingsRepository.insertOrUpdateAppSettings(currentSettings.copy(reminderEnabled = isEnabled))
        }
    }

    /**
     * 更新穿戴设备兼容同步开关状态
     */
    fun updateCompatWearableSync(isEnabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettings().first()
            appSettingsRepository.insertOrUpdateAppSettings(currentSettings.copy(compatWearableSync = isEnabled))
        }
    }

    /**
     * 保存提前提醒分钟数并关闭弹窗
     */
    fun updateRemindBeforeMinutes(minutes: Int) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettings().first()
            appSettingsRepository.insertOrUpdateAppSettings(currentSettings.copy(remindBeforeMinutes = minutes))
            dismissDialog()
        }
    }

    /**
     * 更新自动模式开关状态及控制类型，更新完成后关闭弹窗
     */
    fun updateAutoMode(isEnabled: Boolean, newControlMode: AutoControlMode) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettings().first()
            appSettingsRepository.insertOrUpdateAppSettings(
                currentSettings.copy(
                    autoModeEnabled = isEnabled,
                    autoControlMode = newControlMode
                )
            )
            dismissDialog()
        }
    }
}