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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.xingheyuzhuan.shiguangschedule.component.DesktopToastHost
import com.xingheyuzhuan.shiguangschedule.data.di.SharedModule
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsViewModel
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
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.compose.viewmodel.koinViewModel
import org.koin.plugin.module.dsl.startKoin
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.app_name
import shiguangschedule.shared.generated.resources.ic_launcher_foreground
import java.io.PrintWriter
import java.io.StringWriter

@Module(includes = [SharedModule::class])
@ComponentScan("com.xingheyuzhuan.shiguangschedule")
@KoinApplication(modules = [SharedModule::class])
class DesktopAppConfig

fun main() {
    try {
        startKoin<DesktopAppConfig>()
    } catch (e: Throwable) {
        e.printStackTrace()
    }

    nucleusApplication(backend = NucleusBackend.Tao) {
        var fatalError by mutableStateOf<String?>(null)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            fatalError = getStackTraceText("UI 线程崩溃 [${thread.name}]", throwable)
        }

        val mainWindowState = rememberWindowState(
            width = 1000.dp,
            height = 700.dp
        )
        val toastWindowState = rememberWindowState(
            width = mainWindowState.size.width,
            height = mainWindowState.size.height,
            position = mainWindowState.position
        )

        LaunchedEffect(mainWindowState.position, mainWindowState.size, mainWindowState.isMinimized, mainWindowState.placement) {
            toastWindowState.position = mainWindowState.position
            toastWindowState.size = mainWindowState.size
            toastWindowState.isMinimized = mainWindowState.isMinimized
            toastWindowState.placement = mainWindowState.placement
        }

        DecoratedWindow(
            onCloseRequest = ::exitApplication,
            state = mainWindowState,
            title = "shiguangschedule",
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

                        val targetColor = if (isDevMode) DeveloperIconBgColor else NormalIconBgColor
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
                                        painter = painterResource(Res.drawable.ic_launcher_foreground),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .scale(1.4f),
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
                }
            }
        }

        DecoratedWindow(
            onCloseRequest = {},
            state = toastWindowState,
            title = "ToastOverlay",
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            focusable = false,
            clickThrough = true,
            hiddenFromDock = true,
            visibleOnAllWorkspaces = true,
            forceX11 = true
        ) {
            DesktopWindowThemeWrapper {
                WindowBackground(Color.Transparent)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    DesktopToastHost()
                }
            }
        }
    }
}

@Composable
fun DesktopWindowThemeWrapper(content: @Composable () -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()

    if (state.isReady) {
        ShiguangScheduleTheme(settings = state.appSettings) {
            content()
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

private fun getStackTraceText(prefix: String, throwable: Throwable): String {
    val sw = StringWriter()
    throwable.printStackTrace(PrintWriter(sw))
    return "[$prefix]\n${sw}"
}