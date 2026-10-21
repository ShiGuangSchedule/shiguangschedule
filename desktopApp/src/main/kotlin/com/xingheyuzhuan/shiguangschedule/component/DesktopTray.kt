package com.xingheyuzhuan.shiguangschedule.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.PlatformLocaleManager
import dev.nucleusframework.composenativetray.tray.api.Tray
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.app_name
import shiguangschedule.shared.generated.resources.ic_menu_logo
import shiguangschedule.shared.generated.resources.tray_close_app
import shiguangschedule.shared.generated.resources.tray_open_main_window
import kotlin.system.exitProcess

@Composable
fun DesktopTray(
    onOpenMainWindow: () -> Unit
) {
    val langVersion by PlatformLocaleManager.languageVersion.collectAsState()

    key(langVersion) {
        Tray(
            icon = painterResource(Res.drawable.ic_menu_logo),
            tooltip = stringResource(Res.string.app_name),
            primaryAction = {
                onOpenMainWindow()
            }
        ) {
            Item(label = stringResource(Res.string.tray_open_main_window)) {
                onOpenMainWindow()
            }
            Divider()
            Item(label = stringResource(Res.string.tray_close_app)) {
                exitProcess(0)
            }
        }
    }
}