package com.xingheyuzhuan.shiguangschedule.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import com.xingheyuzhuan.shiguangschedule.tool.ExternalFileManager
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import dev.nucleusframework.window.tao.TaoDragAndDropPayload
import java.awt.datatransfer.DataFlavor
import java.io.File

/**
 * 批量检验并投递外部课表 .json 文件到 ExternalFileManager，返回成功接收的文件数。
 */
internal fun importExternalJsonFiles(files: List<File>, notifyUnsupportedFormat: Boolean = false): Int {
    if (files.isEmpty()) return 0
    var importedCount = 0
    var hasNonJsonFile = false

    files.forEach { file ->
        if (file.isFile && file.name.endsWith(".json", ignoreCase = true)) {
            try {
                val bytes = file.readBytes()
                if (bytes.isNotEmpty()) {
                    ExternalFileManager.onFileReceived(file.name, bytes)
                    importedCount++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            hasNonJsonFile = true
        }
    }

    if (notifyUnsupportedFormat && importedCount == 0 && hasNonJsonFile) {
        ToastManager.show("仅支持拖拽导入 .json 格式的课表文件")
    }
    return importedCount
}

/**
 * 解析命令行启动参数中包含的 .json 课表文件路径（支持双击关联打开或命令行传参导入）。
 */
fun importJsonFilesFromArgs(args: Array<String>): Int {
    if (args.isEmpty()) return 0
    val candidateFiles = args
        .filter { !it.startsWith("-") && it.endsWith(".json", ignoreCase = true) }
        .map { File(it) }
        .filter { it.exists() && it.isFile }
    return importExternalJsonFiles(candidateFiles, notifyUnsupportedFormat = false)
}

@OptIn(ExperimentalComposeUiApi::class)
class DesktopFileDropState {
    // 悬停状态：当文件被拖到窗口上方时为 true，可用于后续扩展导入状态 UI
    var isDraggingOver by mutableStateOf(false)
        internal set

    val target: DragAndDropTarget = object : DragAndDropTarget {
        override fun onEntered(event: DragAndDropEvent) {
            isDraggingOver = true
            super.onEntered(event)
        }

        override fun onExited(event: DragAndDropEvent) {
            isDraggingOver = false
            super.onExited(event)
        }

        override fun onDrop(event: DragAndDropEvent): Boolean {
            isDraggingOver = false

            val files: List<File> = when {
                // 路径一：Tao 原生载荷
                event.nativeEvent is TaoDragAndDropPayload -> {
                    val payload = event.nativeEvent as TaoDragAndDropPayload
                    payload.files.map { File(it) }
                }
                // 路径二：AWT 兼容载荷
                else -> {
                    val transferable = event.awtTransferable
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @Suppress("UNCHECKED_CAST")
                        transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File> ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
            }

            return importExternalJsonFiles(files, notifyUnsupportedFormat = true) > 0
        }
    }
}

@Composable
fun rememberDesktopFileDropState(): DesktopFileDropState {
    return remember { DesktopFileDropState() }
}