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
import dev.nucleusframework.window.tao.TaoDragAndDropPayload
import java.awt.datatransfer.DataFlavor
import java.io.File

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

            var consumed = false
            files.forEach { file ->
                if (file.name.endsWith(".json", ignoreCase = true)) {
                    try {
                        ExternalFileManager.onFileReceived(file.name, file.readBytes())
                        consumed = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return consumed
        }
    }
}

@Composable
fun rememberDesktopFileDropState(): DesktopFileDropState {
    return remember { DesktopFileDropState() }
}