package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

actual val isDesktopPlatform: Boolean = true

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop 桌面端无硬件返回键，空实现
}

/**
 * 桌面端占位控制器，所有操作均为空实现。
 */
class DesktopWebViewController : WebViewController {
    override val currentUrl: String = ""

    override fun reload() = Unit
    override fun goBack(): Boolean = false
    override fun canGoBack(): Boolean = false
    override fun setDevToolsEnabled(enabled: Boolean) = Unit
    override fun executeScript(jsCode: String) = Unit
    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        callback?.invoke(null)
    }
}

@Composable
actual fun rememberWebViewController(): WebViewController {
    return remember { DesktopWebViewController() }
}

@Composable
actual fun PlatformWebView(
    modifier: Modifier,
    url: String,
    isDesktopMode: Boolean,
    isDevToolsEnabled: Boolean,
    controller: WebViewController,
    bridgeHandler: WebBridgeHandler,
    onProgressChange: (Float) -> Unit,
    onTitleChange: (String) -> Unit,
    onNavigateToSchedule: () -> Unit,
    onSslError: (failingUrl: String, onProceed: () -> Unit, onCancel: () -> Unit) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "桌面端网页功能暂未实现",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "请在 Android 或 iOS 设备上使用此功能",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}