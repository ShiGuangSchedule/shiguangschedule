package com.xingheyuzhuan.shiguangschedule.tool

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * JVM / Desktop 平台的文件管理器实现
 */
class JvmFileManager(
    private val onPickImage: () -> Unit,
    private val onImportFile: (List<String>) -> Unit,
    private val onExportFile: (String, ByteArray) -> Unit
) : FileManager {
    override fun pickImage() = onPickImage()
    override fun importFile(allowedExtensions: List<String>) = onImportFile(allowedExtensions)
    override fun exportFile(defaultFileName: String, bytes: ByteArray) = onExportFile(defaultFileName, bytes)
}

@Composable
actual fun rememberFileManager(callbacks: FileManagerCallbacks): FileManager {
    val scope = rememberCoroutineScope()
    val currentCallbacks by rememberUpdatedState(callbacks)

    return remember {
        JvmFileManager(
            // 1. 选择图片
            onPickImage = {
                scope.launch(Dispatchers.IO) {
                    val fileDialog = FileDialog(null as Frame?, "选择图片", FileDialog.LOAD).apply {
                        // 设置只允许常见的图片后缀
                        filenameFilter = java.io.FilenameFilter { _, name ->
                            val lower = name.lowercase()
                            lower.endsWith(".png") || lower.endsWith(".jpg") ||
                                    lower.endsWith(".jpeg") || lower.endsWith(".webp") ||
                                    lower.endsWith(".bmp")
                        }
                        isVisible = true
                    }

                    val directory = fileDialog.directory
                    val file = fileDialog.file

                    val imageBitmap: ImageBitmap? = if (directory != null && file != null) {
                        try {
                            val selectedFile = File(directory, file)
                            val bytes = selectedFile.readBytes()
                            // 使用 Skia 将字节转为 Compose 支持的 ImageBitmap
                            Image.makeFromEncoded(bytes).toComposeImageBitmap()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    } else {
                        null
                    }

                    withContext(Dispatchers.Main) {
                        currentCallbacks.onImagePicked?.invoke(imageBitmap)
                    }
                }
            },

            // 2. 导入文件 (.json, .ics 等)
            onImportFile = { allowedExtensions ->
                scope.launch(Dispatchers.IO) {
                    val fileDialog = FileDialog(null as Frame?, "选择文件", FileDialog.LOAD).apply {
                        if (allowedExtensions.isNotEmpty()) {
                            filenameFilter = java.io.FilenameFilter { _, name ->
                                allowedExtensions.any { ext ->
                                    name.endsWith(".$ext", ignoreCase = true)
                                }
                            }
                        }
                        isVisible = true
                    }

                    val directory = fileDialog.directory
                    val fileName = fileDialog.file

                    val bytes: ByteArray?
                    if (directory != null && fileName != null) {
                        val selectedFile = File(directory, fileName)
                        bytes = try {
                            selectedFile.readBytes()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    } else {
                        bytes = null
                    }

                    withContext(Dispatchers.Main) {
                        currentCallbacks.onFileImported?.invoke(bytes, fileName)
                    }
                }
            },

            // 3. 导出/保存文件
            onExportFile = { defaultFileName, bytes ->
                scope.launch(Dispatchers.IO) {
                    val fileDialog = FileDialog(null as Frame?, "导出保存文件", FileDialog.SAVE).apply {
                        file = defaultFileName
                        isVisible = true
                    }

                    val directory = fileDialog.directory
                    val fileName = fileDialog.file

                    var success = false
                    if (directory != null && fileName != null) {
                        try {
                            val saveFile = File(directory, fileName)
                            saveFile.writeBytes(bytes)
                            success = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        currentCallbacks.onFileExported?.invoke(success)
                    }
                }
            }
        )
    }
}