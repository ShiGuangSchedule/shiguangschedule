package com.xingheyuzhuan.shiguangschedule.data.api.network

import com.xingheyuzhuan.shiguangschedule.data.di.PlatformEnvironment
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.CourseTableImportModel
import com.xingheyuzhuan.shiguangschedule.tool.ExternalFileManager
import io.ktor.server.routing.Routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.util.UUID

@Single
class BrowserExtensionHandler(
    private val networkEngine: NetworkEngine,
    @Named("AppVersionName") private val appVersionName: String,
    val extensionPath: String = EXTENSION_PATH
) {
    companion object {
        const val EXTENSION_PATH = "/shiguang-bridge"
    }

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /** 当前握手密钥，由 NetworkServerManager 按配置刷新 */
    @Volatile
    private var expectedApiKey: String = ""

    /**
     * 更新握手密钥，密钥为空时拒绝所有连接
     */
    fun updateExpectedApiKey(apiKey: String) {
        expectedApiKey = apiKey
    }

    private fun isAuthorized(apiKey: String?): Boolean =
        expectedApiKey.isNotBlank() && apiKey == expectedApiKey

    /**
     * 将浏览器插件路由挂载到网络引擎中（含 Query 参数握手鉴权与首次连接环境同步）
     */
    fun registerRoute(routing: Routing) {
        with(routing) {
            webSocket(extensionPath) {
                if (!isAuthorized(call.request.queryParameters["apiKey"])) {
                    close(
                        CloseReason(
                            CloseReason.Codes.VIOLATED_POLICY,
                            "Unauthorized: Invalid API Key"
                        )
                    )
                    return@webSocket
                }

                networkEngine.registerSession(extensionPath, this)

                sendEnvironmentToSingleSession(this)

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            handleIncomingMessage(frame.readText())
                        }
                    }
                } finally {
                    networkEngine.unregisterSession(extensionPath, this)
                }
            }
        }
    }

    /**
     * 向单个刚连入的会话定向发送环境信息
     */
    private suspend fun sendEnvironmentToSingleSession(session: DefaultWebSocketSession) {
        try {
            val payloadObj = buildJsonObject {
                put("appVersion", appVersionName)
                put("platform", PlatformEnvironment.currentPlatform.name)
            }

            val message = ExtensionBridgeMessage(
                id = UUID.randomUUID().toString(),
                action = "sync_app_environment",
                apiKey = null,
                payload = payloadObj
            )
            session.send(Frame.Text(jsonConfig.encodeToString(message)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 当前已连接的浏览器插件数量
     */
    fun connectedExtensionCount(): Int = networkEngine.activeSessionCount(extensionPath)

    /**
     * 下发执行适配脚本的指令
     *
     * 指令会广播给所有已连接的插件
     *
     * @param targetUrl 目标页面地址，为空时由插件在当前页面执行
     */
    suspend fun dispatchExecuteCommand(
        targetUrl: String?,
        adapterCode: String,
        adapterName: String = "",
        maintainer: String = ""
    ) {
        val payloadObj = buildJsonObject {
            put("targetUrl", targetUrl)
            put("adapterCode", adapterCode)
            put("adapterName", adapterName)
            put("maintainer", maintainer)
        }

        val message = ExtensionBridgeMessage(
            id = UUID.randomUUID().toString(),
            action = "execute_adapter",
            apiKey = null,
            payload = payloadObj
        )
        networkEngine.broadcastToPath(extensionPath, message)
    }

    /**
     * 主动向所有在线插件广播同步桌面端运行环境与版本信息
     */
    suspend fun dispatchAppEnvironment() {
        val payloadObj = buildJsonObject {
            put("appVersion", appVersionName)
            put("platform", PlatformEnvironment.currentPlatform.name)
        }

        val message = ExtensionBridgeMessage(
            id = UUID.randomUUID().toString(),
            action = "sync_app_environment",
            apiKey = null,
            payload = payloadObj
        )
        networkEngine.broadcastToPath(extensionPath, message)
    }

    /**
     * 处理插件上报的消息
     */
    private fun handleIncomingMessage(jsonString: String) {
        try {
            val message = jsonConfig.decodeFromString<ExtensionBridgeMessage>(jsonString)

            if (!isAuthorized(message.apiKey)) {
                println("[安全警告]: 收到来自插件的消息，但 API Key 校验不通过！")
                return
            }

            when (message.action) {
                "course_table_result" -> {
                    println("[课表导入]: 收到插件上报的课表数据，开始处理")

                    val payload = message.payload
                    if (payload == null) {
                        println("[课表导入失败]: 消息的 payload 为空！")
                        return
                    }

                    val importModel = jsonConfig.decodeFromJsonElement<CourseTableImportModel>(payload)

                    val jsonBytes = jsonConfig.encodeToString(importModel).encodeToByteArray()
                    ExternalFileManager.onFileReceived("BrowserImport.json", jsonBytes)
                    println("[课表导入]: 已成功将课表数据投递")
                }

                "report_log" -> {
                    val logContent = message.message ?: "无详细日志"
                    println("[浏览器插件日志]: $logContent")
                }

                "sync_extension_environment" -> {
                    val envData = message.payload?.toString() ?: "{}"
                    println("[浏览器插件环境同步]: $envData")
                }

                else -> {
                    println("[未知动作]: 收到未处理的 action -> ${message.action}")
                }
            }
        } catch (e: Exception) {
            println("[消息处理异常]: 解析或执行插件消息时发生错误")
            e.printStackTrace()
        }
    }
}
