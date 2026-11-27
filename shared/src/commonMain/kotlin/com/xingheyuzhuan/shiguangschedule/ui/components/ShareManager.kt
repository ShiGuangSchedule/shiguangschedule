package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.xingheyuzhuan.shiguangschedule.di.platform.ShareFileSupport
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_share
import shiguangschedule.shared.generated.resources.dialog_text_file_saved_share_prompt
import shiguangschedule.shared.generated.resources.dialog_title_file_saved

/** 文件保存后的分享确认弹窗 */
@Composable
fun ShareDialog(
    filePath: String,
    onDismiss: () -> Unit
) {
    // 未注册分享能力的平台（如桌面端）不弹窗
    val shareSupport = KoinPlatform.getKoin().getOrNull<ShareFileSupport>() ?: return
    val shareFile = shareSupport.rememberShareAction()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_title_file_saved)) },
        text = { Text(stringResource(Res.string.dialog_text_file_saved_share_prompt)) },
        confirmButton = {
            TextButton(
                onClick = {
                    shareFile(filePath)
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
