package com.xingheyuzhuan.shiguangschedule.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.xingheyuzhuan.shiguangschedule.service.DesktopAppController
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.PlatformLocaleManager
import dev.nucleusframework.composenativetray.tray.api.Tray
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.app_name
import shiguangschedule.shared.generated.resources.ic_launcher
import shiguangschedule.shared.generated.resources.tray_close_app
import shiguangschedule.shared.generated.resources.tray_open_main_window

@Composable
fun DesktopTray() {
    val langVersion by PlatformLocaleManager.languageVersion.collectAsState()
    val appController = koinInject<DesktopAppController>()

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
            Item(label = stringResource(Res.string.tray_close_app)) {
                appController.exitApp()
            }
        }
    }
}