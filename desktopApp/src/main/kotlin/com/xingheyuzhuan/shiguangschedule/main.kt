package com.xingheyuzhuan.shiguangschedule

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.xingheyuzhuan.shiguangschedule.data.di.SharedModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.koin.plugin.module.dsl.startKoin

@Module(includes = [SharedModule::class])
@ComponentScan("com.xingheyuzhuan.shiguangschedule")
class DesktopAppModule {

    @Single
    @Named("AppVersionCode")
    fun provideAppVersionCode(): Int = 36

    @Single
    @Named("AppVersionName")
    fun provideAppVersionName(): String = "2.1.0"
}

@KoinApplication(modules = [DesktopAppModule::class])
class DesktopAppConfig

fun main() {
    startKoin<DesktopAppConfig>()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "shiguangschedule",
        ) {
            App()
        }
    }
}