package com.xingheyuzhuan.shiguangschedule.ui.settings.style.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.model.WidgetStyle
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.WidgetThemeModeProto
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel


@KoinViewModel
class WidgetStyleViewModel(
    private val repository: StyleSettingsRepository
) : ViewModel() {

    val widgetStyle: StateFlow<WidgetStyle> = repository.styleFlow
        .map { it.widgetStyle }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WidgetStyle.DEFAULT
        )

    fun updateFontScale(scale: Float) {
        viewModelScope.launch {
            repository.setWidgetFontScale(scale)
        }
    }

    fun updateBackgroundAlpha(alpha: Float) {
        viewModelScope.launch {
            repository.setWidgetBackgroundAlpha(alpha)
        }
    }

    fun updateHideTeacher(hide: Boolean) {
        viewModelScope.launch {
            repository.setWidgetHideTeacher(hide)
        }
    }

    fun updateHideLocation(hide: Boolean) {
        viewModelScope.launch {
            repository.setWidgetHideLocation(hide)
        }
    }

    fun updateHideDate(hide: Boolean) {
        viewModelScope.launch {
            repository.setWidgetHideDate(hide)
        }
    }

    fun updateThemeMode(mode: WidgetThemeModeProto) {
        viewModelScope.launch {
            repository.setWidgetThemeMode(mode)
        }
    }

    fun updateSeedColor(color: Color?) {
        viewModelScope.launch {
            repository.setWidgetSeedColor(color?.toArgb()?.toLong())
        }
    }

    fun resetWidgetStyle() {
        viewModelScope.launch {
            repository.resetWidgetStyleSettings()
        }
    }
}