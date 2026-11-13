package com.xingheyuzhuan.shiguangschedule

import android.app.Application
import androidx.work.Configuration
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration as KoinConfiguration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.plugin.module.dsl.startKoin

@Suppress("unused")
@Module
@KoinConfiguration
@ComponentScan("com.xingheyuzhuan.shiguangschedule")
class AppModule

@KoinApplication
class ScheduleAppConfig

class MyApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()

        startKoin<ScheduleAppConfig> {
            androidLogger()
            androidContext(this@MyApplication)
            workManagerFactory()
        }
    }
}