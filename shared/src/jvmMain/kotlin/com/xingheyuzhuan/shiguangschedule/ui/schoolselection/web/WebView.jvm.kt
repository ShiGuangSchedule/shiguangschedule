package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.xingheyuzhuan.shiguangschedule.tool.PlatformUpdateStrategy
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.Executors

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop 桌面端无硬件返回键，空实现
}

/**
 * 桌面端 Web 控制器：维护当前教务 URL 与最近一次注入的适配脚本，
 * 供桌面端本地浏览器桥接助手生成并复制完整导入脚本。
 */
class DesktopWebViewController : WebViewController {
    override var currentUrl: String by mutableStateOf("")
        internal set

    var latestInjectedScript: String? by mutableStateOf(null)
        private set

    var scriptExecutionCount: Int by mutableIntStateOf(0)
        private set

    var reloadCount: Int by mutableIntStateOf(0)
        private set

    fun updateCurrentUrl(url: String) {
        currentUrl = url
    }

    override fun reload() {
        reloadCount++
    }

    override fun goBack(): Boolean = false

    override fun canGoBack(): Boolean = false

    override fun setDevToolsEnabled(enabled: Boolean) = Unit

    override fun executeScript(jsCode: String) {
        latestInjectedScript = jsCode
        scriptExecutionCount++
    }

    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        callback?.invoke(null)
    }
}

@Composable
actual fun rememberWebViewController(): WebViewController {
    return remember { DesktopWebViewController() }
}

/**
 * 桌面端本地回环桥接负载解析器：
 * 支持解析单条 JsBridgeMessage、JsBridgeMessage 数组、带 messages 字段的打包对象，
 * 或直接粘贴的课程 JSON 数组（自动包装为 saveImportedCourses + notifyTaskCompletion）。
 */
object DesktopBridgePayloadParser {

    fun extractBridgeMessages(rawInput: String): List<String> {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return emptyList()

        return try {
            val element = bridgeJson.parseToJsonElement(trimmed)
            when (element) {
                is JsonObject -> {
                    when {
                        "action" in element -> listOf(trimmed)
                        "messages" in element -> {
                            val messagesArray = element["messages"] as? JsonArray ?: return emptyList()
                            messagesArray.map { it.toString() }
                        }
                        else -> emptyList()
                    }
                }
                is JsonArray -> {
                    if (element.isEmpty()) return emptyList()
                    val firstObj = element.firstOrNull() as? JsonObject
                    if (firstObj != null && "action" in firstObj) {
                        element.map { it.toString() }
                    } else if (firstObj != null && ("name" in firstObj || "courses" in firstObj || "day" in firstObj)) {
                        val saveCoursesMsg = JsBridgeMessage(
                            action = "saveImportedCourses",
                            payload = bridgeJson.encodeToString(
                                SaveCoursesPayload.serializer(),
                                SaveCoursesPayload(coursesJsonString = trimmed)
                            )
                        )
                        val completeMsg = JsBridgeMessage(action = "notifyTaskCompletion")
                        listOf(
                            bridgeJson.encodeToString(JsBridgeMessage.serializer(), saveCoursesMsg),
                            bridgeJson.encodeToString(JsBridgeMessage.serializer(), completeMsg)
                        )
                    } else {
                        emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun buildBrowserConsoleBridgeScript(endpointUrl: String, wrappedAdapterScript: String): String {
        return """
(function() {
    var BRIDGE_ENDPOINT = "$endpointUrl";
    var recordedMessages = [];
    var hasNetworkError = false;

    function sendBridgeMessage(action, payloadObj, callbackId) {
        var msg = {
            action: action,
            callbackId: callbackId || null,
            payload: payloadObj ? JSON.stringify(payloadObj) : null
        };
        recordedMessages.push(msg);
        return fetch(BRIDGE_ENDPOINT, {
            method: "POST",
            mode: "cors",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(msg)
        }).then(function(resp) {
            if (!resp.ok) throw new Error("HTTP " + resp.status);
            return true;
        }).catch(function(err) {
            hasNetworkError = true;
            console.warn("[ShiguangDesktopBridge] 本地回环直连受限，将在完成时导出剪贴板数据包:", err);
            return true;
        });
    }

    function exportFallbackBundleIfNeeded() {
        if (!hasNetworkError || recordedMessages.length === 0) return;
        var bundleText = JSON.stringify({ messages: recordedMessages });
        console.log("[ShiguangDesktopBridge] 请复制以下 JSON 数据到拾光课程表桌面端粘贴导入:\n" + bundleText);
        try {
            if (typeof copy === "function") {
                copy(bundleText);
            } else if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(bundleText);
            }
        } catch (_) {}
        alert("检测到浏览器安全策略拦截了本地端口直连。\n导入数据包已输出到控制台（并尝试复制到剪贴板），请切回拾光课程表桌面端点击【从剪贴板导入数据】完成导入！");
    }

    var shiguangBridgePromise = {
        showAlert: function(titleText, contentText, confirmText) {
            return new Promise(function(resolve) {
                var text = (titleText ? "【" + titleText + "】\n\n" : "") + (contentText || "");
                var confirmed = window.confirm(text);
                resolve(confirmed);
            });
        },
        showPrompt: function(titleText, tipText, defaultText, validatorJsFunction) {
            return new Promise(function(resolve) {
                var promptLabel = (titleText ? "【" + titleText + "】\n" : "") + (tipText || "");
                var currentDefault = defaultText || "";
                while (true) {
                    var input = window.prompt(promptLabel, currentDefault);
                    if (input === null) {
                        resolve(null);
                        return;
                    }
                    if (validatorJsFunction && typeof window[validatorJsFunction] === "function") {
                        try {
                            var err = window[validatorJsFunction](input);
                            if (err && String(err) !== "false") {
                                window.alert(String(err));
                                currentDefault = input;
                                continue;
                            }
                        } catch (e) {
                            console.warn("[ShiguangDesktopBridge] 校验函数异常:", e);
                        }
                    }
                    resolve(input);
                    return;
                }
            });
        },
        showSingleSelection: function(titleText, items, defaultSelectedIndex) {
            return new Promise(function(resolve) {
                var list = [];
                try {
                    list = (typeof items === "string") ? JSON.parse(items) : (items || []);
                } catch (_) {
                    list = [];
                }
                var optionsText = list.map(function(item, idx) {
                    return idx + ": " + item;
                }).join("\n");
                var defIdx = (typeof defaultSelectedIndex === "number" && defaultSelectedIndex >= 0) ? String(defaultSelectedIndex) : "0";
                var input = window.prompt((titleText || "请选择序号") + "\n\n" + optionsText, defIdx);
                if (input === null) {
                    resolve(null);
                    return;
                }
                var parsed = parseInt(input.trim(), 10);
                if (!isNaN(parsed) && parsed >= 0 && parsed < list.length) {
                    resolve(parsed);
                } else {
                    resolve(0);
                }
            });
        },
        saveImportedCourses: function(coursesJsonString) {
            var str = (typeof coursesJsonString === "string") ? coursesJsonString : JSON.stringify(coursesJsonString || []);
            return sendBridgeMessage("saveImportedCourses", { coursesJsonString: str });
        },
        saveCourseConfig: function(configJsonString) {
            var str = (typeof configJsonString === "string") ? configJsonString : JSON.stringify(configJsonString || {});
            return sendBridgeMessage("saveCourseConfig", { configJsonString: str });
        },
        savePresetTimeSlots: function(timeSlotsJsonString) {
            var str = (typeof timeSlotsJsonString === "string") ? timeSlotsJsonString : JSON.stringify(timeSlotsJsonString || []);
            return sendBridgeMessage("savePresetTimeSlots", { timeSlotsJsonString: str });
        },
        saveComboSchedule: function(comboScheduleJsonString) {
            var str = (typeof comboScheduleJsonString === "string") ? comboScheduleJsonString : JSON.stringify(comboScheduleJsonString || {});
            return sendBridgeMessage("saveComboSchedule", { comboScheduleJsonString: str });
        }
    };

    var shiguangBridge = {
        showToast: function(message) {
            console.log("[ShiguangToast]", message);
            sendBridgeMessage("showToast", { message: String(message || "") });
        },
        notifyTaskCompletion: function() {
            sendBridgeMessage("notifyTaskCompletion", null).then(function() {
                exportFallbackBundleIfNeeded();
            });
        }
    };

    window.shiguangBridgePromise = shiguangBridgePromise;
    window.shiguangBridge = shiguangBridge;
    window.AndroidBridgePromise = shiguangBridgePromise;
    window.AndroidBridge = shiguangBridge;
})();
$wrappedAdapterScript
        """.trimIndent()
    }
}

/**
 * 轻量级本地回环 HTTP 桥接服务（仅监听 127.0.0.1 随机空闲端口），
 * 接收系统浏览器控制台脚本发回的 JsBridgeMessage 并路由至 WebBridgeHandler。
 */
internal class DesktopWebBridgeServer(
    private val onMessageReceived: (String) -> Unit,
    private val onRequestCountChanged: (Int) -> Unit
) {
    private var server: HttpServer? = null
    private var receivedCount = 0

    var port: Int = 0
        private set

    val endpointUrl: String
        get() = "http://127.0.0.1:$port/bridge"

    fun start() {
        if (server != null) return
        try {
            val loopback = InetAddress.getLoopbackAddress()
            val httpServer = HttpServer.create(InetSocketAddress(loopback, 0), 0)
            httpServer.executor = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "shiguang-desktop-web-bridge").apply { isDaemon = true }
            }
            httpServer.createContext("/bridge") { exchange ->
                handleBridgeExchange(exchange)
            }
            httpServer.start()
            server = httpServer
            port = httpServer.address.port
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleBridgeExchange(exchange: HttpExchange) {
        try {
            exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
            exchange.responseHeaders.add("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
            exchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type, Accept")
            exchange.responseHeaders.add("Access-Control-Allow-Private-Network", "true")
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")

            when (exchange.requestMethod.uppercase()) {
                "OPTIONS" -> {
                    exchange.sendResponseHeaders(204, -1)
                }
                "POST" -> {
                    val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val messages = DesktopBridgePayloadParser.extractBridgeMessages(body)
                    messages.forEach { msg ->
                        onMessageReceived(msg)
                        receivedCount++
                    }
                    if (messages.isNotEmpty()) {
                        onRequestCountChanged(receivedCount)
                    }
                    val respBytes = """{"ok":true,"dispatched":${messages.size}}""".toByteArray(Charsets.UTF_8)
                    exchange.sendResponseHeaders(200, respBytes.size.toLong())
                    exchange.responseBody.use { it.write(respBytes) }
                }
                else -> {
                    val respBytes = """{"status":"ready","port":$port}""".toByteArray(Charsets.UTF_8)
                    exchange.sendResponseHeaders(200, respBytes.size.toLong())
                    exchange.responseBody.use { it.write(respBytes) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                exchange.sendResponseHeaders(500, -1)
            } catch (_: Exception) {
            }
        } finally {
            exchange.close()
        }
    }

    fun stop() {
        try {
            server?.stop(0)
        } catch (_: Exception) {
        } finally {
            server = null
        }
    }
}

private fun copyTextToClipboard(text: String): Boolean {
    return try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun readTextFromClipboard(): String? {
    return try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
            clipboard.getData(DataFlavor.stringFlavor) as? String
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
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
    val desktopController = controller as? DesktopWebViewController
    var bridgePort by remember { mutableIntStateOf(0) }
    var bridgeReceivedCount by remember { mutableIntStateOf(0) }
    var manualJsonInput by remember { mutableStateOf("") }
    var isScriptCopied by remember { mutableStateOf(false) }

    val bridgeServer = remember(bridgeHandler) {
        DesktopWebBridgeServer(
            onMessageReceived = { msg -> bridgeHandler.onMessageReceived(msg) },
            onRequestCountChanged = { count -> bridgeReceivedCount = count }
        )
    }

    DisposableEffect(bridgeServer) {
        bridgeServer.start()
        bridgePort = bridgeServer.port
        onDispose {
            bridgeServer.stop()
        }
    }

    LaunchedEffect(url) {
        desktopController?.updateCurrentUrl(url)
        onProgressChange(1.0f)
        val displayTitle = try {
            val host = URI(url).host
            if (!host.isNullOrBlank()) "桌面端教务导入助手 ($host)" else "桌面端教务导入助手"
        } catch (_: Exception) {
            "桌面端教务导入助手"
        }
        onTitleChange(displayTitle)
    }

    // 当用户点击底栏“执行导入”并选择目标课表后，WebViewScreen 会调用 controller.executeScript(wrappedJsScript)
    val scriptExecCount = desktopController?.scriptExecutionCount ?: 0
    val latestScript = desktopController?.latestInjectedScript
    LaunchedEffect(scriptExecCount, latestScript, bridgePort) {
        if (scriptExecCount > 0 && !latestScript.isNullOrBlank() && bridgePort > 0) {
            val fullConsoleScript = DesktopBridgePayloadParser.buildBrowserConsoleBridgeScript(
                endpointUrl = bridgeServer.endpointUrl,
                wrappedAdapterScript = latestScript
            )
            if (copyTextToClipboard(fullConsoleScript)) {
                isScriptCopied = true
                ToastManager.show("已生成并复制浏览器导入脚本，请切换到浏览器按 F12 在 Console 粘贴执行")
            }
        }
    }

    val validExternalUrl = remember(url) {
        url.isNotBlank() && url != "about:blank" && (url.startsWith("http://") || url.startsWith("https://"))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶部状态卡片
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "桌面端教务系统浏览器桥接导入",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (bridgePort > 0) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (bridgePort > 0) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            },
                                            shape = CircleShape
                                        )
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (bridgePort > 0) {
                                        "本地桥接已就绪 (127.0.0.1:$bridgePort)"
                                    } else {
                                        "本地桥接未启动"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (bridgePort > 0) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = "为保持桌面版轻量与高性能，桌面端通过系统默认浏览器（Edge / Chrome / Firefox）配合本地回环桥接服务完成教务课表抓取，无需内置百兆级 Chromium 内核。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (bridgeReceivedCount > 0) {
                        Text(
                            text = "已接收来自浏览器的桥接消息：$bridgeReceivedCount 条",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 步骤引导卡片
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "操作步骤",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 步骤 1
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "第一步：在系统浏览器中打开教务网页并登录到课表页面",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (validExternalUrl) "当前教务地址：$url" else "当前未指定初始网址，可在顶部地址栏输入教务网址或在浏览器中自行打开教务系统。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    if (validExternalUrl) {
                                        PlatformUpdateStrategy.openUrl(url)
                                    } else {
                                        ToastManager.show("请先在顶部输入有效的教务系统网址")
                                    }
                                },
                                enabled = validExternalUrl
                            ) {
                                Text("在系统浏览器中打开教务网页")
                            }
                            if (validExternalUrl) {
                                OutlinedButton(
                                    onClick = {
                                        if (copyTextToClipboard(url)) {
                                            ToastManager.show("教务网址已复制到剪贴板")
                                        }
                                    }
                                ) {
                                    Text("复制网址")
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // 步骤 2
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "第二步：点击右下角【执行导入】选择目标课表并生成桥接脚本",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "点击右下角【执行导入】按钮并选择要导入到的课表后，应用会自动将包含本地桥接通道的适配脚本复制到剪贴板。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!latestScript.isNullOrBlank() && bridgePort > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        val fullConsoleScript = DesktopBridgePayloadParser.buildBrowserConsoleBridgeScript(
                                            endpointUrl = bridgeServer.endpointUrl,
                                            wrappedAdapterScript = latestScript
                                        )
                                        if (copyTextToClipboard(fullConsoleScript)) {
                                            isScriptCopied = true
                                            ToastManager.show("导入脚本已重新复制到剪贴板")
                                        }
                                    }
                                ) {
                                    Text(if (isScriptCopied) "重新复制浏览器导入脚本" else "复制浏览器导入脚本")
                                }
                                Text(
                                    text = "目标课表已绑定，脚本已就绪",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // 步骤 3
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "第三步：在浏览器控制台 (Console) 粘贴并回车执行",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "在浏览器课表页面按下 F12（或 Ctrl+Shift+I）打开开发者工具，切换到 Console（控制台）面板，粘贴刚复制的脚本并按 Enter 回车。脚本会自动将课程与作息数据回传至本应用并跳转回课表页。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 备用手动粘贴/剪贴板导入卡片
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "备用方案：剪贴板 / JSON 数据直接导入",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "若部分高校 HTTPS 教务网启用了严格的浏览器安全策略拦截本地回环请求，请先点击右下角【执行导入】绑定课表，再粘贴控制台输出的 JSON 数据或课程数组完成导入：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = manualJsonInput,
                        onValueChange = { manualJsonInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = {
                            Text(
                                text = "在此粘贴桥接 JSON 数据包或课程 JSON 数组...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val clipText = readTextFromClipboard()?.trim()
                                if (clipText.isNullOrEmpty()) {
                                    ToastManager.show("剪贴板为空")
                                } else {
                                    manualJsonInput = clipText
                                    val messages = DesktopBridgePayloadParser.extractBridgeMessages(clipText)
                                    if (messages.isEmpty()) {
                                        ToastManager.show("剪贴板内容不是有效的课表导入 JSON 数据")
                                    } else {
                                        messages.forEach { bridgeHandler.onMessageReceived(it) }
                                    }
                                }
                            }
                        ) {
                            Text("从剪贴板读取并导入")
                        }

                        Button(
                            onClick = {
                                val messages = DesktopBridgePayloadParser.extractBridgeMessages(manualJsonInput)
                                if (messages.isEmpty()) {
                                    ToastManager.show("未识别到有效的桥接 JSON 或课程列表格式")
                                } else {
                                    messages.forEach { bridgeHandler.onMessageReceived(it) }
                                }
                            },
                            enabled = manualJsonInput.isNotBlank()
                        ) {
                            Text("立即解析导入")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
