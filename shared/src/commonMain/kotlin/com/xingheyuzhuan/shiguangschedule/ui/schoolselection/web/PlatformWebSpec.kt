package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.mp.KoinPlatform

/** 跨平台 Web 控制器接口，仅暴露纯粹的 UI 及脚本执行能力 */
interface WebViewController {
    val currentUrl: String
    fun reload()
    fun goBack(): Boolean
    fun canGoBack(): Boolean
    fun setDevToolsEnabled(enabled: Boolean)

    /** 普通单向执行脚本 */
    fun executeScript(jsCode: String)

    /** 评估 JS 脚本并支持异步获取返回值 */
    fun evaluateJavascript(script: String, callback: ((String?) -> Unit)? = null)
}

/** 平台内置 WebView 能力：只有支持的平台注册实现，未注册时由包装函数降级。 */
interface WebViewPlatform {
    @Composable
    fun BackHandler(enabled: Boolean, onBack: () -> Unit)

    @Composable
    fun rememberController(): WebViewController

    @Composable
    fun WebView(
        modifier: Modifier,
        url: String,
        isDesktopMode: Boolean,
        isDevToolsEnabled: Boolean,
        controller: WebViewController,
        bridgeHandler: WebBridgeHandler,
        onProgressChange: (Float) -> Unit,
        onTitleChange: (String) -> Unit,
        onSslError: (failingUrl: String, onProceed: () -> Unit, onCancel: () -> Unit) -> Unit = { _, _, onCancel -> onCancel() }
    )
}

private object UnsupportedWebViewController : WebViewController {
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

private fun currentWebViewPlatform(): WebViewPlatform? =
    KoinPlatform.getKoinOrNull()?.getOrNull<WebViewPlatform>()

@Composable
fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    currentWebViewPlatform()?.BackHandler(enabled, onBack)
}

@Composable
fun rememberWebViewController(): WebViewController =
    currentWebViewPlatform()?.rememberController() ?: UnsupportedWebViewController

@Composable
fun PlatformWebView(
    modifier: Modifier = Modifier,
    url: String,
    isDesktopMode: Boolean,
    isDevToolsEnabled: Boolean,
    controller: WebViewController,
    bridgeHandler: WebBridgeHandler,
    onProgressChange: (Float) -> Unit,
    onTitleChange: (String) -> Unit,
    onSslError: (failingUrl: String, onProceed: () -> Unit, onCancel: () -> Unit) -> Unit = { _, _, onCancel -> onCancel() }
) {
    val platform = currentWebViewPlatform()
    if (platform != null) {
        platform.WebView(
            modifier = modifier,
            url = url,
            isDesktopMode = isDesktopMode,
            isDevToolsEnabled = isDevToolsEnabled,
            controller = controller,
            bridgeHandler = bridgeHandler,
            onProgressChange = onProgressChange,
            onTitleChange = onTitleChange,
            onSslError = onSslError
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            Text("当前平台不支持内置网页视图")
        }
    }
}
