package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.xingheyuzhuan.shiguangschedule.data.model.AndroidAppSettingsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Android 平台专属配置领域仓库（骨架）
 */
@Single
class AndroidAppSettingsRepository(
    @Named("AndroidAppSettings") private val dataStore: DataStore<Preferences>
) {
    /**
     * 获取 Android 设置数据流
     */
    fun getSettings(): Flow<AndroidAppSettingsModel> {
        return dataStore.data.map { prefs ->
            AndroidAppSettingsModel.fromPreferences(prefs)
        }
    }

    /**
     * 更新 Android 设置的通用管道
     */
    suspend fun updateSettings(transform: (AndroidAppSettingsModel) -> AndroidAppSettingsModel) {
        dataStore.edit { prefs ->
            val current = AndroidAppSettingsModel.fromPreferences(prefs)
            val updated = transform(current)
            // TODO: 未来在此处将 updated 的字段写回 prefs
        }
    }
}