package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.xingheyuzhuan.shiguangschedule.data.model.IosAppSettingsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * iOS 平台专属配置领域仓库（骨架）
 */
@Single
class IosAppSettingsRepository(
    @Named("IosAppSettings") private val dataStore: DataStore<Preferences>
) {
    /**
     * 获取 iOS 设置数据流
     */
    fun getSettings(): Flow<IosAppSettingsModel> {
        return dataStore.data.map { prefs ->
            IosAppSettingsModel.fromPreferences(prefs)
        }
    }

    /**
     * 更新 iOS 设置的通用管道
     */
    suspend fun updateSettings(transform: (IosAppSettingsModel) -> IosAppSettingsModel) {
        dataStore.edit { prefs ->
            val current = IosAppSettingsModel.fromPreferences(prefs)
            val updated = transform(current)
            // TODO: 未来在此处将 updated 的字段写回 prefs
        }
    }
}