package com.xingheyuzhuan.shiguangschedule.data.api.network

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.util.Collections

@Single
class NetworkEngine {
    private var server: EmbeddedServer<*, *>? = null
    private val routeSessions = Collections.synchronizedMap(mutableMapOf<String, MutableSet<DefaultWebSocketSession>>())

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /** 当前监听端口，未启动时为 null */
    var boundPort: Int? = null
        private set

    /**
     * 启动底层网络服务器引擎
     */
    fun start(
        port: Int = 59508,
        configureRouting: Routing.() -> Unit
    ) {
        if (server != null) return

        server = embeddedServer(CIO, port = port) {
            install(WebSockets)
            routing {
                configureRouting()
            }
        }.start(wait = false)
        boundPort = port
    }

    /**
     * 服务是否已启动
     */
    fun isRunning(): Boolean = server != null

    /**
     * 注册指定路径的会话
     */
    fun registerSession(path: String, session: DefaultWebSocketSession) {
        routeSessions.getOrPut(path) { Collections.synchronizedSet(mutableSetOf()) }.add(session)
    }

    /**
     * 注销指定路径的会话
     */
    fun unregisterSession(path: String, session: DefaultWebSocketSession) {
        routeSessions[path]?.remove(session)
    }

    /**
     * 指定路径上当前的在线会话数量
     */
    fun activeSessionCount(path: String): Int {
        val sessions = routeSessions[path] ?: return 0
        return synchronized(sessions) { sessions.size }
    }

    /**
     * 向指定路径广播消息
     */
    suspend fun broadcastToPath(path: String, message: ExtensionBridgeMessage) {
        val jsonPayload = jsonConfig.encodeToString(message)

        val sessionsSnapshot = routeSessions[path]?.let { sessions ->
            synchronized(sessions) {
                sessions.toList()
            }
        } ?: return

        for (session in sessionsSnapshot) {
            try {
                session.send(Frame.Text(jsonPayload))
            } catch (e: Exception) {
                // 发送失败的会话已断开，同时从在线列表里移除
                routeSessions[path]?.remove(session)
                e.printStackTrace()
            }
        }
    }

    /**
     * 停止网络引擎并清空会话
     */
    fun stop() {
        server?.stop(1000, 2000)
        server = null
        boundPort = null
        routeSessions.clear()
    }
}
