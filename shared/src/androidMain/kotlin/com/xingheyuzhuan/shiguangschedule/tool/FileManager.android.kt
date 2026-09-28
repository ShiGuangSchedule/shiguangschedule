package com.xingheyuzhuan.shiguangschedule.tool

import android.content.Intent
import android.graphics.BitmapFactory
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AndroidFileManager(
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 使用 rememberUpdatedState 保证在重绘时始终能拿到最新的回调
    val currentCallbacks by rememberUpdatedState(callbacks)

    // 1. 图片选择器 (PickVisualMedia)
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) {
            currentCallbacks.onImagePicked?.invoke(null)
            return@rememberLauncherForActivityResult
        }
        scope.launch(Dispatchers.IO) {
            val bitmap = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            withContext(Dispatchers.Main) {
                currentCallbacks.onImagePicked?.invoke(bitmap)
            }
        }
    }

    // 2. 文件导入器 (GetContent)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            currentCallbacks.onFileImported?.invoke(null, null)
            return@rememberLauncherForActivityResult
        }
        val uri = result.data?.data
        if (uri == null) {
            currentCallbacks.onFileImported?.invoke(null, null)
            return@rememberLauncherForActivityResult
        }
        scope.launch(Dispatchers.IO) {
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            withContext(Dispatchers.Main) {
                currentCallbacks.onFileImported?.invoke(bytes, uri.lastPathSegment)
            }
        }
    }

    // 3. 文件导出器 (CreateDocument)
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val bytesToWrite = pendingExportBytes
        if (uri == null || bytesToWrite == null) {
            currentCallbacks.onFileExported?.invoke(false)
            return@rememberLauncherForActivityResult
        }
        scope.launch(Dispatchers.IO) {
            val success = try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(bytesToWrite)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            pendingExportBytes = null
            withContext(Dispatchers.Main) {
                currentCallbacks.onFileExported?.invoke(success)
            }
        }
    }

    return remember {
        AndroidFileManager(
            onPickImage = {
                imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onImportFile = { extensions ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)

                    if (extensions.isNotEmpty()) {
                        val mimeTypes = extensions.mapNotNull { ext ->
                            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
                        }.toTypedArray()

                        if (mimeTypes.isNotEmpty()) {
                            type = mimeTypes.first()
                            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                        } else {
                            type = "*/*"
                        }
                    } else {
                        type = "*/*"
                    }

                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                }
                importLauncher.launch(intent)
            },
            onExportFile = { fileName, bytes ->
                pendingExportBytes = bytes
                exportLauncher.launch(fileName)
            }
        )
    }
}