package com.xingheyuzhuan.shiguangschedule.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.xingheyuzhuan.shiguangschedule.service.DesktopAppController
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.PlatformLocaleManager
import dev.nucleusframework.autolaunch.AutoLaunch
import dev.nucleusframework.autolaunch.AutoLaunchResult
import dev.nucleusframework.autolaunch.AutoLaunchState
import dev.nucleusframework.composenativetray.tray.api.Tray
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.app_name
import shiguangschedule.shared.generated.resources.ic_launcher
import shiguangschedule.shared.generated.resources.tray_auto_launch
import shiguangschedule.shared.generated.resources.tray_auto_launch_disabled
import shiguangschedule.shared.generated.resources.tray_close_app
import shiguangschedule.shared.generated.resources.tray_open_main_window
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DesktopTray() {
    val langVersion by PlatformLocaleManager.languageVersion.collectAsState()
    val appController = koinInject<DesktopAppController>()

    var autoLaunchState by remember { mutableStateOf(AutoLaunch.state()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000.milliseconds)
            val newState = AutoLaunch.state()
            if (newState != autoLaunchState) {
                autoLaunchState = newState
            }
        }
    }

    key(langVersion) {
        Tray(
            icon = painterResource(Res.drawable.ic_launcher),
            tooltip = stringResource(Res.string.app_name),
            primaryAction = {
                appController.requestShowWindow()
            }
        ) {
            Item(label = stringResource(Res.string.tray_open_main_window)) {
                appController.requestShowWindow()
            }

            Divider()

            // 自启动开关
            val isUserLocked = autoLaunchState == AutoLaunchState.DISABLED_BY_USER
            val isUnsupported = autoLaunchState == AutoLaunchState.UNSUPPORTED

            if (isUserLocked) {
                Item(label = stringResource(Res.string.tray_auto_launch_disabled)) {
                    AutoLaunch.openSystemSettings()
                }
            } else {
                CheckableItem(
                    label = stringResource(Res.string.tray_auto_launch),
                    checked = autoLaunchState == AutoLaunchState.ENABLED ||
                            autoLaunchState == AutoLaunchState.ENABLED_BY_POLICY,
                    isEnabled = !isUnsupported,
                    onCheckedChange = { checked ->
                        if (checked) {
                            when (AutoLaunch.enable()) {
                                AutoLaunchResult.BLOCKED_BY_USER -> {
                                    AutoLaunch.openSystemSettings()
                                }
                                else -> Unit
                            }
                        } else {
                            AutoLaunch.disable()
                        }
                        autoLaunchState = AutoLaunch.state()
                    }
                )
            }

            Divider()

            Item(label = stringResource(Res.string.tray_close_app)) {
                appController.exitApp()
            }
        }
    }
}