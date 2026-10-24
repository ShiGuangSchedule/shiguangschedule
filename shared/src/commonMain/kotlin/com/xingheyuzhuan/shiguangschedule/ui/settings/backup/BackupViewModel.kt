package com.xingheyuzhuan.shiguangschedule.ui.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.api.webdav.WebDavConfig
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport
import com.xingheyuzhuan.shiguangschedule.data.repository.ApiConfigRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.AppBackupPackage
import com.xingheyuzhuan.shiguangschedule.data.repository.BackupMeta
import com.xingheyuzhuan.shiguangschedule.data.repository.BackupModule
import com.xingheyuzhuan.shiguangschedule.data.repository.BackupRepository
import com.xingheyuzhuan.shiguangschedule.tool.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.openZip
import okio.use
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.KoinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.backup_err_connect_failed
import shiguangschedule.shared.generated.resources.backup_err_corrupted
import shiguangschedule.shared.generated.resources.backup_err_empty
import shiguangschedule.shared.generated.resources.backup_err_local_export_failed
import shiguangschedule.shared.generated.resources.backup_err_restore_failed_prefix
import shiguangschedule.shared.generated.resources.backup_err_upload_failed
import shiguangschedule.shared.generated.resources.error_webdav_unconfigured
import kotlin.random.Random

/**
 * 备份与恢复界面的 UI 状态定义
 *
 * @property baseUrl WebDAV 服务器地址
 * @property username WebDAV 账号用户名
 * @property rootPath 备份文件存储的根目录路径，默认为 "ShiguangSchedule"
 * @property hasSavedPassword 是否已保存密码
 * @property isTesting 是否正在执行 WebDAV 连接测试
 * @property isBusy 是否正在执行备份或恢复等耗时异步任务
 * @property testResult 连接或操作的结果状态
 */
data class BackupUiState(
    val baseUrl: String = "",
    val username: String = "",
    val rootPath: String = "ShiguangSchedule",
    val hasSavedPassword: Boolean = false,
    val isTesting: Boolean = false,
    val isBusy: Boolean = false,
    val testResult: TestResult = TestResult.Idle
)

/**
 * 备份与恢复相关异步操作的结果密封接口
 */
sealed interface TestResult {
    data object Idle : TestResult
    data object Success : TestResult
    data class Error(val message: String) : TestResult
}

/**
 * 管理应用程序数据备份与恢复逻辑的 ViewModel
 *
 * 支持通过 WebDAV 进行云端备份/还原，以及通过 Okio 流进行本地 Zip 包的导出/导入。
 */
@KoinViewModel
class BackupViewModel(
    private val apiConfigRepository: ApiConfigRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private var cachedConfig: WebDavConfig? = null
    private val FIXED_BACKUP_DIR = "Backup"

    init {
        // 监听并同步本地存储的 WebDAV 配置变化
        viewModelScope.launch {
            apiConfigRepository.webDavConfigFlow.collectLatest { config ->
                cachedConfig = config
                _uiState.update { state ->
                    state.copy(
                        baseUrl = config?.baseUrl ?: "",
                        username = config?.username ?: "",
                        rootPath = config?.rootPath ?: "ShiguangSchedule",
                        hasSavedPassword = !config?.password.isNullOrBlank()
                    )
                }
            }
        }
    }

    /**
     * 测试 WebDAV 服务器连接状况
     *
     * 连接成功后会自动持久化保存该配置。
     *
     * @param baseUrl 服务器 URL
     * @param username 用户名
     * @param pwd 密码（留空时优先使用已缓存的密码）
     * @param rootPath 备份根路径
     */
    fun testWebDavConnection(baseUrl: String, username: String, pwd: String, rootPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = TestResult.Idle) }
            val finalPassword = if (pwd.isEmpty()) cachedConfig?.password ?: "" else pwd.trim()
            val processedRootPath = rootPath.trim().removeSuffix("/")

            val testConfig = WebDavConfig(baseUrl.trim(), username.trim(), finalPassword, processedRootPath)
            val testClient = apiConfigRepository.createWebDavClient(testConfig)

            val isConnected = testClient?.ensureRootDirectoryExists() ?: false
            testClient?.close()

            if (isConnected) {
                apiConfigRepository.saveWebDavConfig(testConfig)
                _uiState.update { it.copy(isTesting = false, testResult = TestResult.Success) }
            } else {
                val errMsg = getString(Res.string.backup_err_connect_failed)
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testResult = TestResult.Error(errMsg)
                    )
                }
            }
        }
    }

    /**
     * 断开并清除保存的 WebDAV 配置
     */
    fun disconnectWebDav() {
        viewModelScope.launch { apiConfigRepository.clearWebDavConfig() }
    }

    /**
     * 将全量应用数据打包并上传备份至 WebDAV 服务器
     */
    fun backupToWebDav() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, testResult = TestResult.Idle) }

            val client = apiConfigRepository.createWebDavClient() ?: run {
                val errMsg = getString(Res.string.error_webdav_unconfigured)
                _uiState.update { it.copy(isBusy = false, testResult = TestResult.Error(errMsg)) }
                return@launch
            }

            val backupPackage = backupRepository.createFullSoftwareBackup(BackupModule.entries)
            if (backupPackage == null) {
                client.close()
                val errMsg = getString(Res.string.backup_err_empty)
                _uiState.update { it.copy(isBusy = false, testResult = TestResult.Error(errMsg)) }
                return@launch
            }

            val success = withContext(Dispatchers.IO) {
                try {
                    val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY

                    // 1. 生成并上传元数据 meta.json
                    val metaPath = tempDir / "meta.json"
                    FileSystem.SYSTEM.write(metaPath) {
                        writeUtf8(Json.encodeToString(BackupMeta.serializer(), backupPackage.meta))
                    }
                    if (!client.uploadFile(metaPath, "$FIXED_BACKUP_DIR/meta.json")) return@withContext false

                    // 2. 逐模块生成并上传 cbor 数据文件
                    for ((key, bytes) in backupPackage.payloadMap) {
                        val modulePath = tempDir / "$key.cbor"
                        FileSystem.SYSTEM.write(modulePath) {
                            write(bytes)
                        }
                        if (!client.uploadFile(modulePath, "$FIXED_BACKUP_DIR/$key.cbor")) return@withContext false
                    }
                    true
                } catch (_: Exception) {
                    false
                }
            }

            client.close()
            val resultErrorMsg = getString(Res.string.backup_err_upload_failed)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    testResult = if (success) TestResult.Success else TestResult.Error(resultErrorMsg)
                )
            }
        }
    }

    /**
     * 从 WebDAV 服务器拉取备份数据并恢复应用数据
     */
    fun restoreFromWebDav() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, testResult = TestResult.Idle) }

            val client = apiConfigRepository.createWebDavClient() ?: run {
                val errMsg = getString(Res.string.error_webdav_unconfigured)
                _uiState.update { it.copy(isBusy = false, testResult = TestResult.Error(errMsg)) }
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                try {
                    val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
                    val metaPath = tempDir / "meta_restore.json"

                    val corruptedMsg = getString(Res.string.backup_err_corrupted)
                    if (!client.downloadFile("$FIXED_BACKUP_DIR/meta.json", metaPath)) {
                        return@withContext Result.failure(Exception(corruptedMsg))
                    }

                    // 读取并解析元数据
                    val metaText = FileSystem.SYSTEM.read(metaPath) { readUtf8() }
                    val meta = CourseImportExport.json.decodeFromString(BackupMeta.serializer(), metaText)

                    // 批量下载并校验各模块数据文件
                    val payloadMap = mutableMapOf<String, ByteArray>()
                    for (module in meta.modules) {
                        val modulePath = tempDir / "${module.key}_restore.cbor"
                        if (client.downloadFile("$FIXED_BACKUP_DIR/${module.key}.cbor", modulePath)) {
                            val bytes = FileSystem.SYSTEM.read(modulePath) { readByteArray() }
                            payloadMap[module.key] = bytes
                        }
                    }

                    if (payloadMap.isEmpty()) {
                        return@withContext Result.failure(Exception(corruptedMsg))
                    }

                    val availableModules = meta.modules.filter { payloadMap.containsKey(it.key) }
                    val safeMeta = meta.copy(modules = availableModules)

                    val packageObj = AppBackupPackage(safeMeta, payloadMap)
                    backupRepository.restoreFullSoftwareBackup(packageObj)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            client.close()
            val exception = result.exceptionOrNull()
            val failPrefix = getString(Res.string.backup_err_restore_failed_prefix, exception?.message ?: "")
            _uiState.update {
                it.copy(
                    isBusy = false,
                    testResult = if (result.isSuccess) TestResult.Success else TestResult.Error(failPrefix)
                )
            }
        }
    }

    /**
     * 将软件数据全量导出至本地 Zip 压缩流
     *
     * @param sink 用于接收导出的 Zip 字节数据的输出流（BufferedSink）
     * @return 导出成功返回 `true`，失败或无数据返回 `false`
     */
    suspend fun exportToLocalZip(sink: BufferedSink): Boolean = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(isBusy = true, testResult = TestResult.Idle) }

        val backupPackage = backupRepository.createFullSoftwareBackup(BackupModule.entries)
        if (backupPackage == null) {
            val emptyMsg = getString(Res.string.backup_err_empty)
            _uiState.update { it.copy(isBusy = false, testResult = TestResult.Error(emptyMsg)) }
            return@withContext false
        }

        try {
            val entries = mutableMapOf<String, ByteArray>()
            val metaJson = Json.encodeToString(BackupMeta.serializer(), backupPackage.meta)
            entries["meta.json"] = metaJson.encodeToByteArray()

            for ((key, bytes) in backupPackage.payloadMap) {
                entries["$key.cbor"] = bytes
            }

            val zipBytes = ZipUtils.createZip(entries)
            if (zipBytes.isEmpty()) {
                val exportFailMsg = getString(Res.string.backup_err_local_export_failed)
                _uiState.update { it.copy(isBusy = false, testResult = TestResult.Error(exportFailMsg)) }
                return@withContext false
            }

            sink.write(zipBytes)
            sink.flush()

            _uiState.update { it.copy(isBusy = false, testResult = TestResult.Success) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            val exportFailMsg = getString(Res.string.backup_err_local_export_failed)
            _uiState.update { it.copy(isBusy = false, testResult = TestResult.Error(exportFailMsg)) }
            false
        }
    }

    /**
     * 从本地 Zip 压缩流中导入并恢复应用数据
     *
     * @param source 包含备份 Zip 字节数据的输入流（BufferedSource）
     * @return 恢复成功返回 `true`，失败返回 `false`
     */
    suspend fun importFromLocalZip(source: BufferedSource): Boolean = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(isBusy = true, testResult = TestResult.Idle) }

        try {
            val tempDir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
            val randomSuffix = Random.nextLong(100000, 999999)
            val tempZipPath = tempDir / "backup_import_$randomSuffix.zip"

            // 将输入流转存为本地临时压缩包文件以便读取 ZipFS
            FileSystem.SYSTEM.sink(tempZipPath).buffer().use { sink ->
                sink.writeAll(source)
            }

            var meta: BackupMeta? = null
            val payloadMap = mutableMapOf<String, ByteArray>()

            // 使用 Okio ZipFileSystem 读取解压
            val zipFs = FileSystem.SYSTEM.openZip(tempZipPath)
            val filesInZip = zipFs.list("/".toPath())

            for (filePath in filesInZip) {
                val fileName = filePath.name
                when {
                    fileName == "meta.json" -> {
                        val content = zipFs.read(filePath) { readUtf8() }
                        meta = CourseImportExport.json.decodeFromString(BackupMeta.serializer(), content)
                    }
                    fileName.endsWith(".cbor") -> {
                        val key = fileName.removeSuffix(".cbor")
                        val bytes = zipFs.read(filePath) { readByteArray() }
                        payloadMap[key] = bytes
                    }
                }
            }

            // 清理临时文件
            FileSystem.SYSTEM.delete(tempZipPath)

            val corruptedMsg = getString(Res.string.backup_err_corrupted)
            val finalMeta = meta ?: throw Exception(corruptedMsg)

            if (payloadMap.isEmpty()) {
                throw Exception(corruptedMsg)
            }

            val availableModules = finalMeta.modules.filter { payloadMap.containsKey(it.key) }
            val safeMeta = finalMeta.copy(modules = availableModules)

            val packageObj = AppBackupPackage(safeMeta, payloadMap)
            backupRepository.restoreFullSoftwareBackup(packageObj).getOrThrow()

            _uiState.update { it.copy(isBusy = false, testResult = TestResult.Success) }
            true
        } catch (e: Exception) {
            val errMsg = e.message ?: ""
            val failPrefix = getString(Res.string.backup_err_restore_failed_prefix, errMsg)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    testResult = TestResult.Error(failPrefix)
                )
            }
            false
        }
    }
}