package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.xingheyuzhuan.shiguangschedule.data.model.JvmAppSettingsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/**
 * JVM / 桌面平台专属配置仓库
 */
@Single
class JvmAppSettingsRepository(
    @Named("JvmAppSettings") private val dataStore: DataStore<Preferences>
) {
    /**
     * 设置数据流
     */
    fun getSettings(): Flow<JvmAppSettingsModel> {
        return dataStore.data.map { prefs ->
            JvmAppSettingsModel.fromPreferences(prefs)
        }
    }

    /**
     * 更新设置
     */
    suspend fun updateSettings(transform: (JvmAppSettingsModel) -> JvmAppSettingsModel) {
        dataStore.edit { prefs ->
            val updated = transform(JvmAppSettingsModel.fromPreferences(prefs)).ensureApiKey()

            prefs[JvmAppSettingsModel.KEY_LANGUAGE_TAG] = updated.languageTag
            prefs[JvmAppSettingsModel.KEY_EXTENSION_BRIDGE_PORT] = updated.extensionBridge.port
            prefs[JvmAppSettingsModel.KEY_EXTENSION_BRIDGE_API_KEY] = updated.extensionBridge.apiKey
        }
    }

    /**
     * 获取桥接配置，密钥未生成时自动生成并保存
     */
    suspend fun ensureExtensionBridge(): JvmAppSettingsModel.ExtensionBridge {
        val current = getSettings().first().extensionBridge
        if (current.apiKey.isNotBlank()) return current

        val ready = current.copy(apiKey = generateApiKey())
        updateSettings { it.copy(extensionBridge = ready) }
        return ready
    }

    /**
     * 密钥为空时补一个
     */
    private fun JvmAppSettingsModel.ensureApiKey(): JvmAppSettingsModel {
        val bridge = extensionBridge
        return if (bridge.apiKey.isBlank()) {
            copy(extensionBridge = bridge.copy(apiKey = generateApiKey()))
        } else {
            this
        }
    }

    /**
     * 生成 32 位十六进制密钥
     */
    private fun generateApiKey(): String = Uuid.random().toHexString()
}
