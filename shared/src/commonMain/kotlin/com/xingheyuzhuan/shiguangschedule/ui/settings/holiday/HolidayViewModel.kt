package com.xingheyuzhuan.shiguangschedule.ui.settings.holiday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.api.date.ApiDateImporter
import com.xingheyuzhuan.shiguangschedule.data.model.Holiday
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.core.annotation.KoinViewModel

data class HolidayUiState(
    val holidays: List<Holiday> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

@KoinViewModel
class HolidayViewModel(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HolidayUiState())
    val uiState: StateFlow<HolidayUiState> = _uiState.asStateFlow()

    init {
        loadHolidays()
    }

    private fun loadHolidays() {
        viewModelScope.launch {
            appSettingsRepository.getAppSettings().collect { settings ->
                _uiState.update { it.copy(holidays = settings.holidays) }
            }
        }
    }

    /**
     * 触发在线网络同步
     */
    fun syncFromApi() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            try {
                ApiDateImporter.importAndSaveSkippedDates(appSettingsRepository)
                _uiState.update { it.copy(isLoading = false, message = "同步成功") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "同步失败: ${e.message ?: "未知错误"}") }
            }
        }
    }

    /**
     * 保存或修改假期/调休项
     *
     * @param name 名称
     * @param isHoliday 是否是假期 (true: 假期/放假; false: 调休/补班)
     * @param dates 所属日期集合
     * @param oldHoliday 如果是修改操作，传入修改前的对象；若为新增则传 null
     */
    fun saveHoliday(
        name: String,
        isHoliday: Boolean,
        dates: Set<LocalDate>,
        oldHoliday: Holiday? = null
    ) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettingsOnce()
            val existing = currentSettings.holidays.toMutableList()

            val updatedHoliday = Holiday(name = name, isHoliday = isHoliday, dates = dates)

            if (oldHoliday != null) {
                // 编辑模式：按原始对象查找并替换
                val oldIndex = existing.indexOfFirst { it == oldHoliday }
                if (oldIndex >= 0) {
                    existing[oldIndex] = updatedHoliday
                } else {
                    existing.add(updatedHoliday)
                }
            } else {
                // 新增模式：若存在同名同类型的项则替换，否则直接追加
                val index = existing.indexOfFirst { it.name == name && it.isHoliday == isHoliday }
                if (index >= 0) {
                    existing[index] = updatedHoliday
                } else {
                    existing.add(updatedHoliday)
                }
            }

            appSettingsRepository.insertOrUpdateAppSettings(
                currentSettings.copy(holidays = existing)
            )
        }
    }

    /**
     * 批量删除选中的假期/调休项
     */
    fun deleteHolidays(holidaysToDelete: List<Holiday>) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettingsOnce()
            val updatedList = currentSettings.holidays.filterNot { it in holidaysToDelete }
            appSettingsRepository.insertOrUpdateAppSettings(
                currentSettings.copy(holidays = updatedList)
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}