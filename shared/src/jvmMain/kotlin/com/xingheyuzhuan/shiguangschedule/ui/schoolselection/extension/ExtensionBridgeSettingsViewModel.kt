package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.extension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.api.network.BrowserExtensionHandler
import com.xingheyuzhuan.shiguangschedule.data.api.network.NetworkServerManager
import com.xingheyuzhuan.shiguangschedule.data.model.JvmAppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.repository.JvmAppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.SchoolRepository
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.Named
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.toast_adapter_not_found
import shiguangschedule.shared.generated.resources.toast_code_not_found
import shiguangschedule.shared.generated.resources.toast_no_plugin_connected
import shiguangschedule.shared.generated.resources.toast_push_failed_format
import shiguangschedule.shared.generated.resources.toast_push_success_format
import shiguangschedule.shared.generated.resources.toast_server_not_running
import kotlin.time.Duration.Companion.seconds

/**
 * 浏览器插件桥接配置页 UI 状态
 */
data class ExtensionBridgeUiState(
    val port: Int = JvmAppSettingsModel.ExtensionBridge.DEFAULT_PORT,
    val apiKey: String = "",
    val isReady: Boolean = false
)

/**
 * 待推送到浏览器插件的适配器信息
 */
data class BridgeAdapterUiState(
    val adapterName: String = "",
    val description: String = "",
    val maintainer: String = "",
    val importUrl: String? = null,
    val isPushing: Boolean = false
)

/**
 * 浏览器插件桥接配置页 ViewModel
 */
@KoinViewModel
class ExtensionBridgeSettingsViewModel(
    private val appSettingsRepository: JvmAppSettingsRepository,
    private val networkServerManager: NetworkServerManager,
    private val schoolRepository: SchoolRepository,
    private val browserExtensionHandler: BrowserExtensionHandler,
    @Named("FilesDir") private val filesDir: Path,
    private val fileSystem: FileSystem
) : ViewModel() {

    val uiState: StateFlow<ExtensionBridgeUiState> = appSettingsRepository
        .getSettings()
        .map { settings ->
            ExtensionBridgeUiState(
                port = settings.extensionBridge.port,
                apiKey = settings.extensionBridge.apiKey,
                isReady = true
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = ExtensionBridgeUiState()
        )

    private val _adapterState = MutableStateFlow<BridgeAdapterUiState?>(null)

    /**
     * 当前待推送的适配器，未传入学校/适配器 id 时为 null
     */
    val adapterState: StateFlow<BridgeAdapterUiState?> = _adapterState.asStateFlow()

    private var adapterCodePath: Path? = null

    /**
     * 按学校 id 与适配器 id 加载适配信息
     */
    fun loadAdapter(schoolId: String?, adapterId: String?) {
        if (schoolId.isNullOrBlank() || adapterId.isNullOrBlank()) {
            _adapterState.value = null
            adapterCodePath = null
            return
        }

        viewModelScope.launch {
            val school = schoolRepository.getSchoolById(schoolId)
            val adapter = schoolRepository.getAdaptersForSchool(schoolId)
                .find { it.adapter_id == adapterId }

            if (adapter == null) {
                _adapterState.value = null
                adapterCodePath = null
                val msg = getString(Res.string.toast_adapter_not_found)
                ToastManager.show(msg)
                return@launch
            }

            val fileName = adapter.asset_js_path.ifBlank { "$adapterId.js" }
            adapterCodePath = filesDir / "repo" / "schools" / "resources" / school?.resource_folder.orEmpty() / fileName

            _adapterState.value = BridgeAdapterUiState(
                adapterName = adapter.adapter_name,
                description = adapter.description,
                maintainer = adapter.maintainer,
                importUrl = adapter.import_url
            )
        }
    }

    /**
     * 把适配器数据推送给所有已连接的浏览器插件
     */
    fun pushToExtension() {
        val adapter = _adapterState.value ?: return
        if (adapter.isPushing) return

        if (!networkServerManager.isServerRunning()) {
            viewModelScope.launch {
                ToastManager.show(getString(Res.string.toast_server_not_running))
            }
            return
        }

        val pluginCount = browserExtensionHandler.connectedExtensionCount()
        if (pluginCount == 0) {
            viewModelScope.launch {
                ToastManager.show(getString(Res.string.toast_no_plugin_connected))
            }
            return
        }

        viewModelScope.launch {
            val codePath = adapterCodePath
            val code = codePath?.let { path ->
                runCatching {
                    withContext(Dispatchers.IO) {
                        fileSystem.read(path) { readUtf8() }
                    }
                }.getOrNull()
            }

            if (code.isNullOrBlank()) {
                ToastManager.show(getString(Res.string.toast_code_not_found))
                return@launch
            }

            _adapterState.value = adapter.copy(isPushing = true)

            runCatching {
                browserExtensionHandler.dispatchExecuteCommand(
                    targetUrl = adapter.importUrl,
                    adapterCode = code,
                    adapterName = adapter.adapterName,
                    maintainer = adapter.maintainer
                )
            }
                .onSuccess {
                    val msg = getString(Res.string.toast_push_success_format, pluginCount)
                    ToastManager.show(msg)
                }
                .onFailure {
                    val msg = getString(Res.string.toast_push_failed_format, it.message ?: "未知错误")
                    ToastManager.show(msg)
                }

            _adapterState.value = _adapterState.value?.copy(isPushing = false) ?: adapter.copy(isPushing = false)
        }
    }

    /**
     * 修改监听端口，端口变化会重启桥接服务
     */
    fun onPortChanged(port: Int) {
        if (!JvmAppSettingsModel.ExtensionBridge.isValidPort(port)) return

        viewModelScope.launch {
            appSettingsRepository.updateSettings { current ->
                current.copy(extensionBridge = current.extensionBridge.copy(port = port))
            }
            networkServerManager.applyBridgeSettings()
        }
    }

    /**
     * 修改握手密钥，已连接的插件需要按新密钥重新握手
     */
    fun onApiKeyChanged(apiKey: String) {
        val newKey = apiKey.trim()
        if (newKey.isBlank()) return

        viewModelScope.launch {
            appSettingsRepository.updateSettings { current ->
                current.copy(extensionBridge = current.extensionBridge.copy(apiKey = newKey))
            }
            networkServerManager.applyBridgeSettings()
        }
    }
}
