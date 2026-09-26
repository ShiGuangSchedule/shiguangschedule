package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named

@KoinViewModel
class WebViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    @Named("FilesDir") val filesDir: Path,
    val fileSystem: FileSystem
) : ViewModel() {
    val isDeveloperModeEnabled: StateFlow<Boolean> = appSettingsRepository.getAppSettings()
        .map { it.developerModeEnabled }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
}