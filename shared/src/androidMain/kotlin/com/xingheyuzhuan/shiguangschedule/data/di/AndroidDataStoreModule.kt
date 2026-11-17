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
class AndroidDataStoreModule {

    /**
     * 提供 Android 专用的 DataStore 实例
     */
    @Single
    @Named("AndroidAppSettings")
    fun provideAndroidAppSettingsDataStore(
        @Named("FilesDir") filesDir: Path
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath(
            produceFile = { filesDir / "datastore" / "android_app_settings.preferences_pb" }
        )
    }
}