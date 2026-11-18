package com.xingheyuzhuan.shiguangschedule.data.api.network

import com.xingheyuzhuan.shiguangschedule.data.repository.JvmAppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single(createdAtStart = true)
class NetworkServerManager(
    private val networkEngine: NetworkEngine,
    private val browserExtensionHandler: BrowserExtensionHandler,
    private val appSettingsRepository: JvmAppSettingsRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            runCatching { applyBridgeSettings() }.onFailure {
                println("[桥接服务]: 启动失败 -> ${it.message}")
            }
        }
    }

    /**
     * 按持久化配置同步桥接服务：刷新握手密钥，端口变化时重启服务
     */
    suspend fun applyBridgeSettings() {
        val bridge = appSettingsRepository.ensureExtensionBridge()
        browserExtensionHandler.updateExpectedApiKey(bridge.apiKey)

        if (networkEngine.boundPort == bridge.port) return

        networkEngine.stop()
        runCatching {
            networkEngine.start(bridge.port) {
                browserExtensionHandler.registerRoute(this)
            }
        }.onFailure {
            println("[桥接服务]: 端口 ${bridge.port} 启动失败 -> ${it.message}")
        }
    }

    fun isServerRunning(): Boolean = networkEngine.isRunning()

    fun stopServer() {
        networkEngine.stop()
    }
}
