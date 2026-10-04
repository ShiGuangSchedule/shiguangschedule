package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 是否为桌面平台（Desktop/JVM），用于控制模式切换菜单的显隐
 */
expect val isDesktopPlatform: Boolean

/**
 * 跨平台系统返回键响应 (Android 端绑定 BackHandler，iOS/Desktop 端为空实现)
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/**
 * 跨平台 Web 控制器接口，仅暴露纯粹的 UI 及脚本执行能力
 */
interface WebViewController {
    val currentUrl: String
    fun reload()
    fun goBack(): Boolean
    fun canGoBack(): Boolean
    fun setDevToolsEnabled(enabled: Boolean)

    /**
     * 普通单向执行脚本
     */
    fun executeScript(jsCode: String)

    /**
     * 评估 JS 脚本并支持异步获取返回值
     * @param script 待评估的 JS 代码
     * @param callback 执行结果回调
     */
    fun evaluateJavascript(script: String, callback: ((String?) -> Unit)? = null)
}

/**
 * 记住并返回跨平台 WebViewController 实例
 */
@Composable
expect fun rememberWebViewController(): WebViewController

/**
 * 跨平台 Native WebView 渲染组件
 */
@Composable
expect fun PlatformWebView(
    modifier: Modifier = Modifier,
    url: String,
    isDesktopMode: Boolean,
    isDevToolsEnabled: Boolean,
    controller: WebViewController,
    bridgeHandler: WebBridgeHandler,
    onProgressChange: (Float) -> Unit,
    onTitleChange: (String) -> Unit,
    onNavigateToSchedule: () -> Unit,
    onSslError: (failingUrl: String, onProceed: () -> Unit, onCancel: () -> Unit) -> Unit = { _, _, onCancel -> onCancel() }
)