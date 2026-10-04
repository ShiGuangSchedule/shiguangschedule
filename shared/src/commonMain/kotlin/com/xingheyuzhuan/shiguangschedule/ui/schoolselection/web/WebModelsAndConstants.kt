package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import kotlinx.coroutines.flow.Flow

// --- 常量 ---
const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

// --- UI 数据与事件模型 ---
data class AlertDialogData(
    val title: String,
    val content: String,
    val confirmText: String
)

data class PromptDialogData(
    val title: String,
    val tip: String,
    val defaultText: String,
    val validatorJsFunction: String?
)

data class SingleSelectionDialogData(
    val title: String,
    val items: List<String>,
    val defaultSelectedIndex: Int = -1
)

/**
 * SSL 证书错误传递数据模型
 */
data class SslErrorData(
    val primaryErrorUrl: String,
    val onProceed: () -> Unit,
    val onCancel: () -> Unit
)

sealed interface WebUiEvent {
    data class ShowAlert(
        val data: AlertDialogData,
        val callback: (confirmed: Boolean) -> Unit
    ) : WebUiEvent

    data class ShowPrompt(
        val data: PromptDialogData,
        val onRequestValidation: (input: String, onSuccess: () -> Unit) -> Unit,
        val errorFeedbackFlow: Flow<String?>,
        val onCancel: () -> Unit
    ) : WebUiEvent

    data class ShowSingleSelection(
        val data: SingleSelectionDialogData,
        val callback: (selectedIndex: Int?) -> Unit
    ) : WebUiEvent

    /**
     * 显示 SSL 证书安全警告弹窗事件
     */
    data class ShowSslWarning(
        val data: SslErrorData
    ) : WebUiEvent
}