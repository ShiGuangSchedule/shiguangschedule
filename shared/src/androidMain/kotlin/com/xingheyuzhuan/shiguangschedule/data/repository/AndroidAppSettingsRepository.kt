package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.xingheyuzhuan.shiguangschedule.data.model.AndroidAppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.AutoControlMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Android 平台专属配置领域仓库
 */
@Single
class AndroidAppSettingsRepository(
    @Named("AndroidAppSettings") private val dataStore: DataStore<Preferences>,
    @Named("AppSettings") private val legacyDataStore: DataStore<Preferences>
) {
    private val migrationMutex = Mutex()

    @Volatile
    private var migrated = false

    private suspend fun ensureMigrated() {
        if (migrated) return
        migrationMutex.withLock {
            if (migrated) return
            migrateFromLegacy()
            migrated = true
        }
    }

    /**
     * 从旧版公共 AppSettings 迁移 Android 专属配置。
     * 合并规则：
     * - 旧 enabled=false          → OFF
     * - 旧 enabled=true           → 原模式；缺失/非法 → DND
     * - 旧只有模式、无 enabled     → 保留该模式
     */
    private suspend fun migrateFromLegacy() {
        val oldPrefs = runCatching { legacyDataStore.data.first() }.getOrNull() ?: return
        val newPrefs = dataStore.data.first()

        val oldEnabledKey = AppSettingsModel.KEY_AUTO_MODE_ENABLED
        val oldModeKey = AppSettingsModel.KEY_AUTO_CONTROL_MODE
        val oldCompatKey = AppSettingsModel.KEY_COMPAT_WEARABLE_SYNC
        val newModeKey = AndroidAppSettingsModel.KEY_AUTO_CONTROL_MODE
        val newCompatKey = AndroidAppSettingsModel.KEY_COMPAT_WEARABLE_SYNC

        val oldHasEnabled = oldPrefs.contains(oldEnabledKey)
        val oldHasMode = oldPrefs.contains(oldModeKey)
        val oldHasCompat = oldPrefs.contains(oldCompatKey)

        val needMergeMode = !newPrefs.contains(newModeKey) && (oldHasEnabled || oldHasMode)
        val needMigrateCompat = !newPrefs.contains(newCompatKey) && oldHasCompat
        if (!needMergeMode && !needMigrateCompat) return

        dataStore.edit { prefs ->
            if (needMergeMode) {
                val enabled = if (oldHasEnabled) oldPrefs[oldEnabledKey] else null
                val rawMode = if (oldHasMode) oldPrefs[oldModeKey] else null
                prefs[newModeKey] = mergeMode(enabled, rawMode).value
            }
            if (needMigrateCompat) {
                prefs[newCompatKey] = oldPrefs[oldCompatKey]!!
            }
        }

        legacyDataStore.edit { prefs ->
            if (needMergeMode) {
                prefs.remove(oldEnabledKey)
                prefs.remove(oldModeKey)
            }
            if (needMigrateCompat) prefs.remove(oldCompatKey)
        }
    }

    private fun mergeMode(enabled: Boolean?, rawMode: String?): AutoControlMode {
        if (enabled == false) return AutoControlMode.OFF
        val parsed = AutoControlMode.fromString(rawMode)
        return if (parsed == AutoControlMode.OFF) AutoControlMode.DND else parsed
    }

    fun getSettings(): Flow<AndroidAppSettingsModel> = flow {
        ensureMigrated()
        emitAll(dataStore.data.map { prefs ->
            AndroidAppSettingsModel.fromPreferences(prefs)
        })
    }

    suspend fun updateSettings(transform: (AndroidAppSettingsModel) -> AndroidAppSettingsModel) {
        ensureMigrated()
        dataStore.edit { prefs ->
            val current = AndroidAppSettingsModel.fromPreferences(prefs)
            val updated = transform(current)

            prefs[AndroidAppSettingsModel.KEY_AUTO_CONTROL_MODE] = updated.autoControlMode.value
            prefs[AndroidAppSettingsModel.KEY_COMPAT_WEARABLE_SYNC] = updated.compatWearableSync
        }
    }
}