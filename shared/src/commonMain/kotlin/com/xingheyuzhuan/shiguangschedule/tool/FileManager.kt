package com.xingheyuzhuan.shiguangschedule.tool

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.launch

/** 全局文件与平台资源管理器 */
interface FileManager {
    /** 唤起系统图片选择器 */
    fun pickImage()

    /** 唤起系统文件导入器，allowedExtensions 为允许的后缀名 */
    fun importFile(allowedExtensions: List<String> = emptyList())

    /** 唤起系统文件保存器 */
    fun exportFile(defaultFileName: String, bytes: ByteArray)
}

/** 文件操作结果回调 */
data class FileManagerCallbacks(
    val onImagePicked: ((ImageBitmap?) -> Unit)? = null,
    val onFileImported: ((bytes: ByteArray?, fileName: String?) -> Unit)? = null,
    val onFileExported: ((success: Boolean) -> Unit)? = null
)

private class FileKitFileManager(
    private val pickImageAction: () -> Unit,
    private val importFileAction: (List<String>) -> Unit,
    private val exportFileAction: (String, ByteArray) -> Unit
) : FileManager {
    override fun pickImage() = pickImageAction()
    override fun importFile(allowedExtensions: List<String>) = importFileAction(allowedExtensions)
    override fun exportFile(defaultFileName: String, bytes: ByteArray) = exportFileAction(defaultFileName, bytes)
}

/** 在 Composable 中获取 FileManager 实例 */
@Composable
fun rememberFileManager(callbacks: FileManagerCallbacks): FileManager {
    val scope = rememberCoroutineScope()
    // 重组时始终使用最新回调
    val currentCallbacks by rememberUpdatedState(callbacks)
    // 保存器只返回目标文件，字节数据需暂存
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingExtensions by remember { mutableStateOf(emptyList<String>()) }

    // 图片选择：FileKit 图片类型 → Compose ImageBitmap
    val imagePicker = rememberFilePickerLauncher(
        type = FileKitType.Image,
        onResult = { file: PlatformFile? ->
            if (file == null) {
                currentCallbacks.onImagePicked?.invoke(null)
                return@rememberFilePickerLauncher
            }
            scope.launch {
                val bitmap = try {
                    file.toImageBitmap()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                currentCallbacks.onImagePicked?.invoke(bitmap)
            }
        }
    )

    // 文件导入：读取字节与文件名，按调用方后缀名校验
    val importPicker = rememberFilePickerLauncher(
        type = FileKitType.File(),
        onResult = { file: PlatformFile? ->
            if (file == null) {
                currentCallbacks.onFileImported?.invoke(null, null)
                return@rememberFilePickerLauncher
            }
            val allowed = pendingExtensions
            if (allowed.isNotEmpty() && allowed.none { it.equals(file.extension, ignoreCase = true) }) {
                currentCallbacks.onFileImported?.invoke(null, null)
                return@rememberFilePickerLauncher
            }
            scope.launch {
                val bytes = try {
                    file.readBytes()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                currentCallbacks.onFileImported?.invoke(bytes, file.name)
            }
        }
    )

    // 文件保存：将暂存字节写入用户选择的文件
    val exportSaver = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onResult = { file: PlatformFile? ->
            val bytes = pendingExportBytes
            pendingExportBytes = null
            if (file == null || bytes == null) {
                currentCallbacks.onFileExported?.invoke(false)
                return@rememberFileSaverLauncher
            }
            scope.launch {
                val success = try {
                    file.write(bytes)
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
                currentCallbacks.onFileExported?.invoke(success)
            }
        }
    )

    // 保持实例稳定，避免重组导致业务侧引用失效
    return remember(imagePicker, importPicker, exportSaver) {
        FileKitFileManager(
            pickImageAction = { imagePicker.launch() },
            importFileAction = { allowedExtensions ->
                pendingExtensions = allowedExtensions
                importPicker.launch()
            },
            exportFileAction = { defaultFileName, bytes ->
                pendingExportBytes = bytes
                val baseName = defaultFileName.substringBeforeLast(".")
                val extension = defaultFileName.substringAfterLast(".", "").ifEmpty { null }
                exportSaver.launch(suggestedName = baseName, defaultExtension = extension)
            }
        )
    }
}
