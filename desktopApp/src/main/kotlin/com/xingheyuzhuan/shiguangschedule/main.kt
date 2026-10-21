package com.xingheyuzhuan.shiguangschedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.xingheyuzhuan.shiguangschedule.data.di.SharedModule
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsViewModel
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.PlatformLocaleManager
import com.xingheyuzhuan.shiguangschedule.ui.theme.DeveloperIconBgColor
import com.xingheyuzhuan.shiguangschedule.ui.theme.NormalIconBgColor
import com.xingheyuzhuan.shiguangschedule.ui.theme.ShiguangScheduleTheme
import com.xingheyuzhuan.shiguangschedule.ui.theme.rememberSystemDark
import dev.nucleusframework.application.DecoratedWindow
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.window.WindowAppearance
import dev.nucleusframework.window.WindowAppearanceMode
import dev.nucleusframework.window.WindowBackground
import dev.nucleusframework.window.WindowControls
import dev.nucleusframework.window.WindowScaffold
import dev.nucleusframework.window.windowDragArea
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.plugin.module.dsl.startKoin
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.app_name
import shiguangschedule.shared.generated.resources.ic_menu_logo
import java.io.PrintWriter
import java.io.StringWriter

@Module(includes = [SharedModule::class])
@ComponentScan("com.xingheyuzhuan.shiguangschedule")
@KoinApplication(modules = [SharedModule::class])
class DesktopAppConfig

fun main() {

    System.setProperty("app.version", "2.1.0")
    System.setProperty("app.version.code", "36")

    try {
        startKoin<DesktopAppConfig>()
    } catch (e: Throwable) {
        e.printStackTrace()
    }

    PlatformLocaleManager.initialize()

    nucleusApplication(backend = NucleusBackend.Tao) {
        var fatalError by remember { mutableStateOf<String?>(null) }
        var showMainWindow by remember { mutableStateOf(true) }

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            fatalError = getStackTraceText("UI 线程崩溃 [${thread.name}]", throwable)
        }

        DesktopTray(
            onOpenMainWindow = {
                showMainWindow = true
            }
        )

        if (showMainWindow) {
            val mainWindowState = rememberWindowState(
                width = 1000.dp,
                height = 700.dp
            )

            // 主窗口
            DecoratedWindow(
                onCloseRequest = {
                    showMainWindow = false
                },
                state = mainWindowState,
                title = stringResource(Res.string.app_name),
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

private fun getStackTraceText(prefix: String, throwable: Throwable): String {
    val sw = StringWriter()
    throwable.printStackTrace(PrintWriter(sw))
    return "[$prefix]\n${sw}"
}