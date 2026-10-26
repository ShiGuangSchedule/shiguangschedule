package com.xingheyuzhuan.shiguangschedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberWindowState
import com.xingheyuzhuan.shiguangschedule.component.DesktopToastHost
import com.xingheyuzhuan.shiguangschedule.component.DesktopTray
import com.xingheyuzhuan.shiguangschedule.component.rememberDesktopFileDropState
import com.xingheyuzhuan.shiguangschedule.data.di.AppStorage
import com.xingheyuzhuan.shiguangschedule.data.di.SharedModule
import com.xingheyuzhuan.shiguangschedule.data.di.TargetPlatform
import com.xingheyuzhuan.shiguangschedule.data.di.isPlatformSupported
import com.xingheyuzhuan.shiguangschedule.service.DesktopAppController
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsViewModel
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.PlatformLocaleManager
import com.xingheyuzhuan.shiguangschedule.ui.theme.DeveloperIconBgColor
import com.xingheyuzhuan.shiguangschedule.ui.theme.NormalIconBgColor
import com.xingheyuzhuan.shiguangschedule.ui.theme.ShiguangScheduleTheme
import com.xingheyuzhuan.shiguangschedule.ui.theme.rememberSystemDark
import dev.nucleusframework.application.DecoratedWindow
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.autolaunch.AutoLaunch
import dev.nucleusframework.window.WindowAppearance
import dev.nucleusframework.window.WindowAppearanceMode
import dev.nucleusframework.window.WindowBackground
import dev.nucleusframework.window.WindowControls
import dev.nucleusframework.window.WindowScaffold
import dev.nucleusframework.window.windowDragArea
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.context.GlobalContext
import org.koin.plugin.module.dsl.startKoin
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.app_name
import shiguangschedule.shared.generated.resources.ic_launcher
import shiguangschedule.shared.generated.resources.ic_menu_logo
import java.io.PrintWriter
import java.io.StringWriter
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import kotlin.time.Duration.Companion.milliseconds

@Module
@ComponentScan("com.xingheyuzhuan.shiguangschedule")
class DesktopAppConfig

@KoinApplication(modules = [SharedModule::class, DesktopAppConfig::class])
class DesktopKoinApp

fun main(args: Array<String>) {
    val rootLogger = Logger.getLogger("")

    try {
        startKoin<DesktopKoinApp>()
    } catch (e: Throwable) {
        e.printStackTrace()
    }

    setupLogging()
    rootLogger.info("应用启动，日志系统已就绪")

    PlatformLocaleManager.initialize()

    val startedAtLogin = AutoLaunch.wasStartedAtLogin(args)

    nucleusApplication(backend = NucleusBackend.Tao) {
        var fatalError by remember { mutableStateOf<String?>(null) }
        var showMainWindow by remember { mutableStateOf(!startedAtLogin) }

        val mainWindowState = rememberWindowState(
            width = 1000.dp,
            height = 700.dp
        )

        LaunchedEffect(Unit) {
            if (showMainWindow && isPlatformSupported(TargetPlatform.MACOS)) {
                var attempts = 0
                while (attempts < 10) {
                    if (AutoLaunch.wasStartedAtLogin(args)) {
                        showMainWindow = false
                        mainWindowState.isMinimized = true
                        break
                    }
                    attempts++
                    delay(100.milliseconds)
                }
            }
        }

        val appController = GlobalContext.get().get<DesktopAppController>()

        LaunchedEffect(Unit) {
            appController.showWindowEvent.collect {
                if (showMainWindow) {
                    mainWindowState.isMinimized = false
                } else {
                    showMainWindow = true
                    mainWindowState.isMinimized = false
                    delay(100.milliseconds)
                }
            }
        }

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val errorText = getStackTraceText("UI 线程崩溃 [${thread.name}]", throwable)
            rootLogger.severe(errorText)
            fatalError = errorText
        }

        val fileDropState = rememberDesktopFileDropState()

        DesktopTray()

        if (showMainWindow) {
            // 主窗口
            DecoratedWindow(
                onCloseRequest = {
                    showMainWindow = false
                },
                state = mainWindowState,
                title = stringResource(Res.string.app_name),
                icon = painterResource(Res.drawable.ic_launcher)
            ) {
                DesktopWindowThemeWrapper {
                    val isDark = rememberSystemDark()

                    WindowAppearance(
                        if (isDark) WindowAppearanceMode.Dark else WindowAppearanceMode.Light
                    )

                    WindowBackground(Color.Transparent)

                    WindowScaffold(
                        titleBar = {
                            val titleBarBg = MaterialTheme.colorScheme.surfaceContainer

                            val viewModel: SettingsViewModel = koinViewModel()
                            val state by viewModel.uiState.collectAsState()
                            val isDevMode = state.appSettings.developerModeEnabled

                            val targetColor =
                                if (isDevMode) DeveloperIconBgColor else NormalIconBgColor
                            val animatedIconBgColor by animateColorAsState(
                                targetValue = targetColor,
                                animationSpec = tween(durationMillis = 400)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(titleBarBg)
                                    .windowDragArea(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(animatedIconBgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(Res.drawable.ic_menu_logo),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(3.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(Res.string.app_name),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }

                                WindowControls(
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                            }
                        }
                    ) { contentPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding)
                                .dragAndDropTarget(
                                    shouldStartDragAndDrop = { true },
                                    target = fileDropState.target
                                )
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

                        Popup(
                            alignment = Alignment.BottomEnd,
                            offset = IntOffset(-24, -24),
                            properties = PopupProperties(
                                focusable = false,
                                dismissOnBackPress = false,
                                dismissOnClickOutside = false,
                                clippingEnabled = false
                            )
                        ) {
                            DesktopToastHost()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopWindowThemeWrapper(content: @Composable () -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()

    val langVersion by PlatformLocaleManager.languageVersion.collectAsState()

    if (state.isReady) {
        ShiguangScheduleTheme(settings = state.appSettings) {
            key(langVersion) {
                content()
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            key(langVersion) {
                content()
            }
        }
    }
}

private fun setupLogging() {
    try {
        val appStorage = GlobalContext.get().get<AppStorage>()

        val logDir = java.io.File(appStorage.cacheDir.toString()).apply { mkdirs() }
        val logFile = java.io.File(logDir, "shiguangschedule-runtime.log").absolutePath

        val fileHandler = FileHandler(
            logFile,
            5 * 1024 * 1024,
            10,
            true
        ).apply {
            formatter = SimpleFormatter()
            level = Level.INFO
        }

        val rootLogger = Logger.getLogger("")
        rootLogger.addHandler(fileHandler)
        rootLogger.level = Level.INFO

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun getStackTraceText(prefix: String, throwable: Throwable): String {
    val sw = StringWriter()
    throwable.printStackTrace(PrintWriter(sw))
    return "[$prefix]\n${sw}"
}