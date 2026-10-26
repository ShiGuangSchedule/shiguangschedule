package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.xingheyuzhuan.shiguangschedule.data.di.TargetPlatform
import com.xingheyuzhuan.shiguangschedule.data.di.isPlatformSupported
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_share
import shiguangschedule.shared.generated.resources.dialog_text_file_saved_share_prompt
import shiguangschedule.shared.generated.resources.dialog_title_file_saved

/**
 * 平台底层文件分享执行逻辑
 */
expect fun platformShareFile(filePath: String, mimeType: String)

/**
 * 文件保存成功后的分享确认弹窗（分享管理器的 UI 组件之一）
 */
@Composable
fun ShareDialog(
    filePath: String,
    mimeType: String,
    onDismiss: () -> Unit
) {
    if (!isPlatformSupported(TargetPlatform.mobile)) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_title_file_saved)) },
        text = { Text(stringResource(Res.string.dialog_text_file_saved_share_prompt)) },
        confirmButton = {
            TextButton(
                onClick = {
                    platformShareFile(filePath, mimeType)
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.action_share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}