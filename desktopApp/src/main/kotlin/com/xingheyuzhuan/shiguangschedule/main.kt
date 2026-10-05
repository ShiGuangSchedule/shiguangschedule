package com.xingheyuzhuan.shiguangschedule

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.xingheyuzhuan.shiguangschedule.data.di.SharedModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import org.koin.plugin.module.dsl.startKoin
import java.io.PrintWriter
import java.io.StringWriter

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
    var fatalError by mutableStateOf<String?>(null)

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        fatalError = getStackTraceText("UI 线程崩溃 [${thread.name}]", throwable)
    }

    try {
        startKoin<DesktopAppConfig>()
    } catch (e: Throwable) {
        fatalError = getStackTraceText("Koin 初始化失败", e)
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "shiguangschedule",
        ) {
            if (fatalError != null) {
                SelectionContainer(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "应用运行遇到严重错误:\n\n$fatalError",
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            } else {
                App()
            }
        }
    }
}

/**
 * 格式化 Exception 堆栈文本
 */
private fun getStackTraceText(prefix: String, throwable: Throwable): String {
    val sw = StringWriter()
    throwable.printStackTrace(PrintWriter(sw))
    return "[$prefix]\n${sw}"
}