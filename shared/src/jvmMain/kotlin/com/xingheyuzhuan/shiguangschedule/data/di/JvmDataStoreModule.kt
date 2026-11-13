package com.xingheyuzhuan.shiguangschedule.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@Configuration
@Suppress("unused")
class JvmDataStoreModule {

    /**
     * 提供 JVM 专用的 DataStore 实例
     */
    @Single
    @Named("JvmAppSettings")
    fun provideJvmAppSettingsDataStore(
        @Named("FilesDir") filesDir: Path
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = { filesDir / "datastore" / "jvm_app_settings.preferences_pb" }
        )
    }
}