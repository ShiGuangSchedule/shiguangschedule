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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport
import com.xingheyuzhuan.shiguangschedule.tool.PlatformUpdateStrategy
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDeps
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop 桌面端无硬件返回键，空实现
}

/**
 * 桌面端本地浏览器桥接控制器：
 * 通过轻量级本地回环 HTTP 服务 (127.0.0.1) 将系统浏览器控制台执行的教务适配脚本与客户端 WebBridgeHandler 双向打通，
 * 无需打包体积庞大的 Chromium/CEF 内核即可完成教务系统课表导入。
 */
class DesktopWebViewController : WebViewController {
    var currentUrlState by mutableStateOf("")
    override val currentUrl: String
        get() = currentUrlState

    var generatedBrowserScript by mutableStateOf<String?>(null)
        internal set

    var bridgePort by mutableIntStateOf(0)
        internal set

    val bridgeLogs = mutableStateListOf<String>()

    internal var activeBridgeHandler: WebBridgeHandler? = null
    private var httpServer: HttpServer? = null
    private val pendingCallbacks = ConcurrentHashMap<String, CompletableFuture<BridgeCallbackResponse>>()

    data class BridgeCallbackResponse(
        val isSuccess: Boolean,
        val resultRawJs: String
    )

    fun startLocalBridgeServer() {
        if (httpServer != null) return
        try {
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            server.executor = Executors.newCachedThreadPool { runnable ->
                Thread(runnable, "Shiguang-Desktop-WebBridge").apply { isDaemon = true }
            }
            server.createContext("/bridge") { exchange ->
                handleBridgeExchange(exchange)
            }
            server.start()
            httpServer = server
            bridgePort = server.address.port
            appendLog("本地导入桥接服务已启动: http://127.0.0.1:$bridgePort/bridge")
        } catch (e: Exception) {
            appendLog("本地桥接服务启动失败: ${e.message}")
        }
    }

    fun stopLocalBridgeServer() {
        try {
            httpServer?.stop(0)
        } catch (_: Exception) {
        } finally {
            httpServer = null
            bridgePort = 0
            pendingCallbacks.values.forEach {
                it.complete(BridgeCallbackResponse(false, "\"Bridge closed\""))
            }
            pendingCallbacks.clear()
        }
    }

    private fun handleBridgeExchange(exchange: HttpExchange) {
        exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
        exchange.responseHeaders.add("Access-Control-Allow-Methods", "POST, OPTIONS")
        exchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")
        exchange.responseHeaders.add("Access-Control-Allow-Private-Network", "true")
        exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")

        if (exchange.requestMethod.equals("OPTIONS", ignoreCase = true)) {
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
            return
        }

        if (!exchange.requestMethod.equals("POST", ignoreCase = true)) {
            val resp = """{"ok":false,"error":"Method not allowed"}""".toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(405, resp.size.toLong())
            exchange.responseBody.use { it.write(resp) }
            return
        }

        try {
            val body = exchange.requestBody.bufferedReader(Charsets.UTF_8).readText()
            val message = bridgeJson.decodeFromString<JsBridgeMessage>(body)
            val callbackId = message.callbackId
            appendLog("收到浏览器桥接指令: ${message.action}")

            val handler = activeBridgeHandler
            if (handler == null) {
                val resp = """{"ok":false,"error":"Bridge handler not ready"}""".toByteArray(Charsets.UTF_8)
                exchange.sendResponseHeaders(503, resp.size.toLong())
                exchange.responseBody.use { it.write(resp) }
                return
            }

            if (callbackId != null && message.action != "showToast" && message.action != "notifyTaskCompletion") {
                val future = CompletableFuture<BridgeCallbackResponse>()
                pendingCallbacks[callbackId] = future
                handler.onMessageReceived(body)

                val callbackResp = try {
                    future.get(180, TimeUnit.SECONDS)
                } catch (_: Exception) {
                    BridgeCallbackResponse(false, "\"操作等待超时\"")
                } finally {
                    pendingCallbacks.remove(callbackId)
                }

                val responseJson = """{"ok":${callbackResp.isSuccess},"result":${callbackResp.resultRawJs}}"""
                val bytes = responseJson.toByteArray(Charsets.UTF_8)
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } else {
                handler.onMessageReceived(body)
                val bytes = """{"ok":true,"result":true}""".toByteArray(Charsets.UTF_8)
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
        } catch (e: Exception) {
            appendLog("处理桥接消息异常: ${e.message}")
            val errBytes = """{"ok":false,"error":"Invalid bridge message"}""".toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(400, errBytes.size.toLong())
            exchange.responseBody.use { it.write(errBytes) }
        }
    }

    override fun reload() {
        if (currentUrlState.isNotBlank() && currentUrlState != "about:blank") {
            PlatformUpdateStrategy.openUrl(currentUrlState)
        }
    }

    override fun goBack(): Boolean = false
    override fun canGoBack(): Boolean = false
    override fun setDevToolsEnabled(enabled: Boolean) = Unit

    override fun executeScript(jsCode: String) {
        val fullBrowserScript = buildDesktopBrowserImportScript(jsCode, bridgePort)
        generatedBrowserScript = fullBrowserScript
        if (copyToClipboard(fullBrowserScript)) {
            appendLog("已生成并复制浏览器导入脚本到剪贴板（长度: ${fullBrowserScript.length} 字符）")
            ToastManager.show("导入脚本已复制到剪贴板！请切换到浏览器按 F12 在控制台粘贴执行。")
        } else {
            appendLog("已生成浏览器导入脚本，请点击下方按钮手动复制")
        }
    }

    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        val parsed = parseNativeCallbackScript(script)
        if (parsed != null) {
            val (callbackId, isSuccess, resultRawJs) = parsed
            pendingCallbacks[callbackId]?.complete(BridgeCallbackResponse(isSuccess, resultRawJs))
        }
        // 对于 showPrompt 校验器函数调用，默认放行校验并在浏览器端做前置校验
        callback?.invoke("false")
    }

    internal fun appendLog(msg: String) {
        if (bridgeLogs.size >= 20) {
            bridgeLogs.removeAt(0)
        }
        bridgeLogs.add(msg)
    }

    /**
     * 按序执行手动粘贴的 JSON 数据导入，等待每一步异步存储完成后再触发下一阶段与 notifyTaskCompletion，
     * 避免 notifyTaskCompletion 提前清空 importTableId 导致课程落盘失败的竞态问题。
     */
    internal suspend fun executeManualImportJson(
        rawJson: String,
        bridgeHandler: WebBridgeHandler
    ) {
        val messages = parseManualImportPayload(rawJson)
        for (msg in messages) {
            val cbId = msg.callbackId
            val encodedMsg = bridgeJson.encodeToString(msg)
            if (!cbId.isNullOrBlank() && msg.action != "showToast" && msg.action != "notifyTaskCompletion") {
                val future = CompletableFuture<BridgeCallbackResponse>()
                pendingCallbacks[cbId] = future
                withContext(Dispatchers.Main) {
                    bridgeHandler.onMessageReceived(encodedMsg)
                }
                val resp = withContext(Dispatchers.IO) {
                    try {
                        future.get(15, TimeUnit.SECONDS)
                    } catch (_: Exception) {
                        BridgeCallbackResponse(false, "\"导入步骤等待超时\"")
                    } finally {
                        pendingCallbacks.remove(cbId)
                    }
                }
                if (!resp.isSuccess) {
                    throw IllegalStateException("导入步骤 [${msg.action}] 失败: ${resp.resultRawJs}")
                }
            } else {
                withContext(Dispatchers.Main) {
                    bridgeHandler.onMessageReceived(encodedMsg)
                }
            }
        }
    }

    companion object {
        /**
         * 将手动粘贴的 JSON 字符串（支持课程数组 [...]、离线导出包 {"courses":...}、单条或多条 JsBridgeMessage）
         * 统一转换为有序的 JsBridgeMessage 序列。
         */
        internal fun parseManualImportPayload(rawJson: String): List<JsBridgeMessage> {
            val trimmed = rawJson.trim()
            require(trimmed.isNotEmpty()) { "导入 JSON 内容不能为空" }

            val element: JsonElement = try {
                CourseImportExport.json.parseToJsonElement(trimmed)
            } catch (e: Exception) {
                throw IllegalArgumentException("JSON 语法格式无效: ${e.message}", e)
            }

            return when (element) {
                is JsonObject -> {
                    if (element.containsKey("action") && element["action"]?.jsonPrimitive?.isString == true) {
                        listOf(bridgeJson.decodeFromString<JsBridgeMessage>(trimmed))
                    } else if (element.containsKey("courses")) {
                        val model = CourseImportExport.json.decodeFromString<CourseImportExport.CourseTableImportModel>(trimmed)
                        buildList {
                            if (!model.timeSlots.isNullOrEmpty()) {
                                val slotsJson = CourseImportExport.json.encodeToString(model.timeSlots)
                                add(
                                    JsBridgeMessage(
                                        action = "savePresetTimeSlots",
                                        callbackId = "manual_timeslots",
                                        payload = bridgeJson.encodeToString(SaveTimeSlotsPayload(slotsJson))
                                    )
                                )
                            }
                            if (model.comboSchedule != null) {
                                val comboJson = CourseImportExport.json.encodeToString(model.comboSchedule)
                                add(
                                    JsBridgeMessage(
                                        action = "saveComboSchedule",
                                        callbackId = "manual_combo",
                                        payload = bridgeJson.encodeToString(SaveComboSchedulePayload(comboJson))
                                    )
                                )
                            }
                            if (model.config != null) {
                                val configJson = CourseImportExport.json.encodeToString(model.config)
                                add(
                                    JsBridgeMessage(
                                        action = "saveCourseConfig",
                                        callbackId = "manual_config",
                                        payload = bridgeJson.encodeToString(SaveConfigPayload(configJson))
                                    )
                                )
                            }
                            val coursesJson = CourseImportExport.json.encodeToString(model.courses)
                            add(
                                JsBridgeMessage(
                                    action = "saveImportedCourses",
                                    callbackId = "manual_courses",
                                    payload = bridgeJson.encodeToString(SaveCoursesPayload(coursesJson))
                                )
                            )
                            add(JsBridgeMessage(action = "notifyTaskCompletion"))
                        }
                    } else {
                        throw IllegalArgumentException("无法识别的 JSON 对象，需包含 courses 或 action 字段")
                    }
                }

                is JsonArray -> {
                    val firstObj = element.firstOrNull() as? JsonObject
                    if (firstObj != null && firstObj.containsKey("action")) {
                        bridgeJson.decodeFromString<List<JsBridgeMessage>>(trimmed)
                    } else {
                        val courses = CourseImportExport.json.decodeFromString<List<CourseImportExport.ImportCourseJsonModel>>(trimmed)
                        val coursesJson = CourseImportExport.json.encodeToString(courses)
                        listOf(
                            JsBridgeMessage(
                                action = "saveImportedCourses",
                                callbackId = "manual_courses",
                                payload = bridgeJson.encodeToString(SaveCoursesPayload(coursesJson))
                            ),
                            JsBridgeMessage(action = "notifyTaskCompletion")
                        )
                    }
                }

                else -> throw IllegalArgumentException("不支持的 JSON 根节点类型")
            }
        }

        internal fun readFromClipboard(): String? {
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
        /**
         * 解析 buildJsCallbackScript 生成的回调脚本：
         * window._shiguangNativeCallback('cb_1_...', true, ...);
         */
        internal fun parseNativeCallbackScript(script: String): Triple<String, Boolean, String>? {
            val prefix = "window._shiguangNativeCallback('"
            if (!script.startsWith(prefix)) return null
            val afterPrefix = script.removePrefix(prefix)
            val idEnd = afterPrefix.indexOf("'")
            if (idEnd < 0) return null
            val callbackId = afterPrefix.substring(0, idEnd)
            val rest = afterPrefix.substring(idEnd + 1).trimStart().removePrefix(",").trimStart()
            val isSuccess: Boolean
            val afterSuccess: String
            when {
                rest.startsWith("true") -> {
                    isSuccess = true
                    afterSuccess = rest.removePrefix("true").trimStart().removePrefix(",").trimStart()
                }
                rest.startsWith("false") -> {
                    isSuccess = false
                    afterSuccess = rest.removePrefix("false").trimStart().removePrefix(",").trimStart()
                }
                else -> return null
            }
            val resultRawJs = afterSuccess.removeSuffix(");").trim()
            return Triple(callbackId, isSuccess, resultRawJs)
        }

        internal fun buildDesktopBrowserImportScript(wrappedAdapterJs: String, port: Int): String {
            val endpoint = if (port > 0) "http://127.0.0.1:$port/bridge" else ""
            return """
(function() {
    var BRIDGE_ENDPOINT = "$endpoint";
    var callbackCounter = 0;
    var exportedBundle = {};

    function sendToDesktop(action, payload, needCallback) {
        var callbackId = needCallback ? ('cb_' + (++callbackCounter) + '_' + Date.now()) : null;
        var msgObj = {
            action: action,
            callbackId: callbackId,
            payload: payload ? JSON.stringify(payload) : null
        };
        if (!BRIDGE_ENDPOINT) {
            return Promise.reject(new Error("Local bridge endpoint unavailable"));
        }
        return fetch(BRIDGE_ENDPOINT, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(msgObj)
        }).then(function(res) { return res.json(); }).then(function(data) {
            if (data && data.ok) return data.result;
            throw new Error((data && data.error) || String(data && data.result) || "Bridge rejected");
        });
    }

    var shiguangBridgePromise = {
        showAlert: function(titleText, contentText, confirmText) {
            return sendToDesktop('showAlert', {
                titleText: titleText || '',
                contentText: contentText || '',
                confirmText: confirmText || null
            }, true).catch(function() {
                return window.confirm((titleText ? (titleText + "\n\n") : "") + (contentText || ""));
            });
        },
        showPrompt: function(titleText, tipText, defaultText, validatorJsFunction) {
            return sendToDesktop('showPrompt', {
                titleText: titleText || '',
                tipText: tipText || '',
                defaultText: defaultText || '',
                validatorJsFunction: validatorJsFunction || ''
            }, true).catch(function() {
                var val = window.prompt((titleText || "") + (tipText ? ("\n" + tipText) : ""), defaultText || "");
                return val;
            });
        },
        showSingleSelection: function(titleText, items, defaultSelectedIndex) {
            var itemsArr = (typeof items === 'string') ? JSON.parse(items || '[]') : (items || []);
            var itemsJson = (typeof items === 'string') ? items : JSON.stringify(itemsArr);
            return sendToDesktop('showSingleSelection', {
                titleText: titleText || '',
                itemsJsonString: itemsJson,
                defaultSelectedIndex: defaultSelectedIndex !== undefined ? defaultSelectedIndex : -1
            }, true).catch(function() {
                var listText = itemsArr.map(function(it, idx) { return idx + ": " + it; }).join("\n");
                var defIdx = (defaultSelectedIndex !== undefined && defaultSelectedIndex >= 0) ? String(defaultSelectedIndex) : "0";
                var picked = window.prompt((titleText || "请选择序号") + "\n" + listText, defIdx);
                return picked !== null ? parseInt(picked, 10) : null;
            });
        },
        saveImportedCourses: function(coursesJsonString) {
            var str = (typeof coursesJsonString === 'string') ? coursesJsonString : JSON.stringify(coursesJsonString || []);
            exportedBundle.courses = JSON.parse(str);
            return sendToDesktop('saveImportedCourses', { coursesJsonString: str }, true).catch(function() {
                console.log("[ShiguangDesktopFallback] 请复制以下 JSON 到拾光课程表桌面端粘贴导入:");
                console.log(JSON.stringify(exportedBundle));
                return true;
            });
        },
        saveCourseConfig: function(configJsonString) {
            var str = (typeof configJsonString === 'string') ? configJsonString : JSON.stringify(configJsonString || {});
            exportedBundle.config = JSON.parse(str);
            return sendToDesktop('saveCourseConfig', { configJsonString: str }, true).catch(function() {
                return true;
            });
        },
        savePresetTimeSlots: function(timeSlotsJsonString) {
            var str = (typeof timeSlotsJsonString === 'string') ? timeSlotsJsonString : JSON.stringify(timeSlotsJsonString || []);
            exportedBundle.timeSlots = JSON.parse(str);
            return sendToDesktop('savePresetTimeSlots', { timeSlotsJsonString: str }, true).catch(function() {
                return true;
            });
        },
        saveComboSchedule: function(comboScheduleJsonString) {
            var str = (typeof comboScheduleJsonString === 'string') ? comboScheduleJsonString : JSON.stringify(comboScheduleJsonString || {});
            exportedBundle.comboSchedule = JSON.parse(str);
            return sendToDesktop('saveComboSchedule', { comboScheduleJsonString: str }, true).catch(function() {
                return true;
            });
        }
    };

    var shiguangBridge = {
        showToast: function(message) {
            console.log("[拾光课程表] " + message);
            sendToDesktop('showToast', { message: String(message || '') }, false).catch(function() {});
        },
        notifyTaskCompletion: function() {
            sendToDesktop('notifyTaskCompletion', null, false).catch(function() {
                var fallbackJson = JSON.stringify(exportedBundle);
                console.log("====== 拾光课程表导出数据 (若未自动同步请复制下行 JSON 到桌面端粘贴) ======");
                console.log(fallbackJson);
            });
        }
    };

    window.shiguangBridgePromise = shiguangBridgePromise;
    window.shiguangBridge = shiguangBridge;
    window.AndroidBridgePromise = shiguangBridgePromise;
    window.AndroidBridge = shiguangBridge;
})();
$wrappedAdapterJs
            """.trimIndent()
        }

        internal fun copyToClipboard(text: String): Boolean {
            return try {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(text), null)
                true
            } catch (_: Exception) {
                false
            }
        }
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
    val desktopController = controller as? DesktopWebViewController
    val coroutineScope = rememberCoroutineScope()
    val pickerDeps: CourseTablePickerDeps = koinViewModel()
    val courseTables by pickerDeps.courseTableRepository.getAllCourseTables().collectAsState(initial = emptyList())
    val appSettings by pickerDeps.appSettingsRepository.getAppSettings().collectAsState(initial = null)

    var selectedTableId by remember { mutableStateOf<String?>(null) }
    var showTablePickerDialog by remember { mutableStateOf(false) }
    var manualJsonInput by remember { mutableStateOf("") }
    var isImportingManualJson by remember { mutableStateOf(false) }

    val selectedTableName = remember(courseTables, selectedTableId) {
        courseTables.find { it.id == selectedTableId }?.name ?: "默认当前课表"
    }

    LaunchedEffect(appSettings?.currentCourseTableId, courseTables) {
        if (selectedTableId == null) {
            val defaultId = appSettings?.currentCourseTableId ?: courseTables.firstOrNull()?.id
            if (defaultId != null) {
                selectedTableId = defaultId
                bridgeHandler.setImportTableId(defaultId)
            }
        }
    }

    LaunchedEffect(url) {
        desktopController?.currentUrlState = url
        onProgressChange(1.0f)
        if (url.isNotBlank() && url != "about:blank") {
            onTitleChange(url)
        }
    }

    DisposableEffect(desktopController, bridgeHandler) {
        desktopController?.activeBridgeHandler = bridgeHandler
        desktopController?.startLocalBridgeServer()
        onDispose {
            desktopController?.stopLocalBridgeServer()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "电脑端教务系统浏览器桥接导入",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "为保持极小安装包体积并直接复用系统浏览器登录态，电脑端通过本地回环桥接服务 (127.0.0.1:${desktopController?.bridgePort ?: 0}) 与系统浏览器联动完成课表导入。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "步骤 1：在系统浏览器中打开并登录教务系统",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (url.isNotBlank() && url != "about:blank") "目标网址：$url" else "您可在顶部地址栏输入教务系统网址，或直接打开学校教务系统网页。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val target = if (url.isNotBlank() && url != "about:blank") url else "https://"
                            PlatformUpdateStrategy.openUrl(target)
                            desktopController?.appendLog("已在系统浏览器打开: $target")
                        },
                        enabled = url.isNotBlank() && url != "about:blank"
                    ) {
                        Text("在系统浏览器中打开教务网页")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    FilledTonalButton(
                        onClick = { showTablePickerDialog = true }
                    ) {
                        Text("导入目标：$selectedTableName")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "步骤 2：生成导入脚本并在浏览器控制台执行",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "1. 点击本页面底部右下角的「执行导入」按钮并选择要导入的目标课表（系统会自动将适配脚本复制到剪贴板）。\n" +
                            "2. 切换到浏览器已登录的课表页面，按 F12 打开开发者工具 -> 切换到 Console（控制台）面板。\n" +
                            "3. 按 Ctrl+V 粘贴脚本并按回车执行，课表数据将通过本地桥接自动同步回拾光课程表！",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val script = desktopController?.generatedBrowserScript
                if (!script.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (DesktopWebViewController.copyToClipboard(script)) {
                                    ToastManager.show("导入脚本已重新复制到剪贴板")
                                }
                            }
                        ) {
                            Text("重新复制浏览器导入脚本")
                        }
                        Text(
                            text = "脚本已就绪 (${script.length} 字符)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "备用通道：手动粘贴课程 JSON 数据直接导入",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "若部分高校教务网站启用了严格的浏览器安全策略阻止本地回环连接，您可将控制台输出的课程 JSON 粘贴至下方直接导入：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = manualJsonInput,
                    onValueChange = { manualJsonInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    placeholder = { Text("粘贴课程数组 JSON ([{...}]) 或导出包 JSON") }
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val clip = DesktopWebViewController.readFromClipboard()
                            if (!clip.isNullOrBlank()) {
                                manualJsonInput = clip
                                ToastManager.show("已从剪贴板粘贴内容")
                            } else {
                                ToastManager.show("剪贴板中无可用文本")
                            }
                        }
                    ) {
                        Text("从剪贴板粘贴")
                    }

                    Button(
                        onClick = {
                            val trimmed = manualJsonInput.trim()
                            if (trimmed.isNotBlank()) {
                                val targetId = selectedTableId
                                    ?: appSettings?.currentCourseTableId
                                    ?: courseTables.firstOrNull()?.id
                                if (targetId == null) {
                                    showTablePickerDialog = true
                                    ToastManager.show("请先选择要导入的目标课表")
                                    return@Button
                                }
                                coroutineScope.launch {
                                    isImportingManualJson = true
                                    try {
                                        bridgeHandler.setImportTableId(targetId)
                                        if (desktopController != null) {
                                            desktopController.executeManualImportJson(trimmed, bridgeHandler)
                                        } else {
                                            val msgs = DesktopWebViewController.parseManualImportPayload(trimmed)
                                            msgs.forEach { msg ->
                                                bridgeHandler.onMessageReceived(bridgeJson.encodeToString(msg))
                                            }
                                        }
                                    } catch (e: Exception) {
                                        ToastManager.show("JSON 解析导入失败: ${e.message}")
                                    } finally {
                                        isImportingManualJson = false
                                    }
                                }
                            }
                        },
                        enabled = manualJsonInput.isNotBlank() && !isImportingManualJson
                    ) {
                        Text(if (isImportingManualJson) "正在导入..." else "解析并导入粘贴的 JSON")
                    }
                }
            }
        }

        if (desktopController != null && desktopController.bridgeLogs.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "桥接状态日志",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    desktopController.bridgeLogs.forEach { logLine ->
                        Text(
                            text = "• $logLine",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showTablePickerDialog) {
        CourseTablePickerDialog(
            title = "选择导入目标课表",
            onDismissRequest = { showTablePickerDialog = false },
            onTableSelected = { table ->
                selectedTableId = table.id
                bridgeHandler.setImportTableId(table.id)
                showTablePickerDialog = false
                ToastManager.show("已将导入目标课表切换为：${table.name}")
            }
        )
    }
}
