package com.xingheyuzhuan.shiguangschedule.tool

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

class FileKitManager(
    private val pickImageLauncher: () -> Unit,
    private val importFileLauncher: (List<String>) -> Unit,
    private val exportFileLauncher: (String, ByteArray) -> Unit
) : FileManager {
    override fun pickImage() = pickImageLauncher()
    override fun importFile(allowedExtensions: List<String>) = importFileLauncher(allowedExtensions)
    override fun exportFile(defaultFileName: String, bytes: ByteArray) = exportFileLauncher(defaultFileName, bytes)
}

@Composable
actual fun rememberFileManager(callbacks: FileManagerCallbacks): FileManager {
    val coroutineScope = rememberCoroutineScope()

    return remember {
        FileKitManager(
            pickImageLauncher = {
                coroutineScope.launch(Dispatchers.IO) {
                    val file = FileKit.openFilePicker(type = FileKitType.Image)
                    val imageBitmap = file?.readBytes()?.let { bytes ->
                        try {
                            Image.makeFromEncoded(bytes).toComposeImageBitmap()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
                    withContext(Dispatchers.Main) {
                        callbacks.onImagePicked?.invoke(imageBitmap)
                    }
                }
            },

            importFileLauncher = { allowedExtensions ->
                coroutineScope.launch(Dispatchers.IO) {
                    val file = if (allowedExtensions.isNotEmpty()) {
                        FileKit.openFilePicker(
                            type = FileKitType.File(extensions = allowedExtensions.toSet())
                        )
                    } else {
                        FileKit.openFilePicker()
                    }

                    val bytes = file?.readBytes()
                    val fileName = file?.name

                    withContext(Dispatchers.Main) {
                        callbacks.onFileImported?.invoke(bytes, fileName)
                    }
                }
            },

            exportFileLauncher = { defaultFileName, bytes ->
                coroutineScope.launch(Dispatchers.IO) {
                    val baseName = defaultFileName.substringBeforeLast(".")
                    val extension = defaultFileName.substringAfterLast(".", "")

                    val file = FileKit.openFileSaver(
                        suggestedName = baseName,
                        defaultExtension = extension.ifEmpty { null }
                    )

                    var success = false
                    if (file != null) {
                        try {
                            file.write(bytes)
                            success = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        callbacks.onFileExported?.invoke(success)
                    }
                }
            }
        )
    }
}