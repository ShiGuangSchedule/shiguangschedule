package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.xingheyuzhuan.shiguangschedule.data.model.JvmAppSettingsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * JVM / 桌面平台专属配置领域仓库
 */
@Single
class JvmAppSettingsRepository(
    @Named("JvmAppSettings") private val dataStore: DataStore<Preferences>
) {
    /**
     * 获取 JVM 设置数据流
     */
    fun getSettings(): Flow<JvmAppSettingsModel> {
        return dataStore.data.map { prefs ->
            JvmAppSettingsModel.fromPreferences(prefs)
        }
    }

    /**
     * 更新 JVM 设置的通用管道
     */
    suspend fun updateSettings(transform: (JvmAppSettingsModel) -> JvmAppSettingsModel) {
        dataStore.edit { prefs ->
            val current = JvmAppSettingsModel.fromPreferences(prefs)
            val updated = transform(current)

            prefs[JvmAppSettingsModel.KEY_LANGUAGE_TAG] = updated.languageTag
        }
    }
}