package com.xingheyuzhuan.shiguangschedule.data.di

import okio.FileSystem
import okio.Path
import okio.SYSTEM
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module(includes = [
    DatabaseModule::class,
    DataStoreModule::class
])
@Configuration
@ComponentScan("com.xingheyuzhuan.shiguangschedule")
class SharedModule {

    @Single
    fun provideFileSystem(): FileSystem = FileSystem.SYSTEM

    @Single
    @Named("FilesDir")
    fun provideFilesDir(appStorage: AppStorage): Path {
        return appStorage.filesDir
    }

    /**
     * 提供全局应用缓存目录路径
     */
    @Single
    @Named("CacheDir")
    fun provideCacheDir(appStorage: AppStorage): Path {
        return appStorage.cacheDir
    }
}