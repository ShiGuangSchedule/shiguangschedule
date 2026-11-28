@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.extension

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.model.JvmAppSettingsModel
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.SettingsGroupCard
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsRow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_copy_key
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.action_generate_random_key
import shiguangschedule.shared.generated.resources.action_push_to_extension
import shiguangschedule.shared.generated.resources.action_reset_default_port
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.content_copy_24px
import shiguangschedule.shared.generated.resources.desc_bridge_restart_on_change
import shiguangschedule.shared.generated.resources.desc_key_requirement
import shiguangschedule.shared.generated.resources.desc_port_range
import shiguangschedule.shared.generated.resources.error_input_port
import shiguangschedule.shared.generated.resources.error_key_empty
import shiguangschedule.shared.generated.resources.info_24px
import shiguangschedule.shared.generated.resources.item_browser_extension_bridge
import shiguangschedule.shared.generated.resources.label_contributor_format
import shiguangschedule.shared.generated.resources.label_contributor_unknown
import shiguangschedule.shared.generated.resources.label_handshake_key
import shiguangschedule.shared.generated.resources.label_key
import shiguangschedule.shared.generated.resources.label_listen_port
import shiguangschedule.shared.generated.resources.label_port
import shiguangschedule.shared.generated.resources.reset_settings_24px
import shiguangschedule.shared.generated.resources.section_bridge_service
import shiguangschedule.shared.generated.resources.shuffle_24px
import shiguangschedule.shared.generated.resources.text_no_detailed_description
import shiguangschedule.shared.generated.resources.title_set_api_key
import shiguangschedule.shared.generated.resources.title_set_port
import shiguangschedule.shared.generated.resources.toast_key_copied
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.uuid.Uuid

/**
 * 浏览器插件桥接配置页
 *
 * 只暴露两个可修改项：监听端口与握手密钥。
 * 卡片描述字段直接展示当前值，点击卡片弹出输入弹窗修改对应项，
 * 密钥卡片右侧提供复制按钮。
 *
 * 传入 [schoolId] 与 [adapterId] 时，页面额外展示对应适配器的信息卡片，
 * 并提供把适配代码推送到浏览器插件的按钮。
 */
@Composable
fun ExtensionBridgeSettingsScreen(
    onBack: () -> Unit,
    schoolId: String? = null,
    adapterId: String? = null,
    viewModel: ExtensionBridgeSettingsViewModel = koinViewModel()
) {
    val settingPadding = 16.dp

    val uiState by viewModel.uiState.collectAsState()
    val adapterState by viewModel.adapterState.collectAsState()

    var showPortDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    val keyCopiedText = stringResource(Res.string.toast_key_copied)

    LaunchedEffect(schoolId, adapterId) {
        viewModel.loadAdapter(schoolId, adapterId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.item_browser_extension_bridge)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = settingPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsGroupCard(
                modifier = Modifier.padding(top = 8.dp),
                title = stringResource(Res.string.section_bridge_service)
            ) {
                item {
                    SettingsRow(
                        title = stringResource(Res.string.label_listen_port),
                        subtitle = if (uiState.isReady) uiState.port.toString() else "",
                        enabled = uiState.isReady,
                        onClick = { showPortDialog = true }
                    )
                }

                item {
                    SettingsRow(
                        title = stringResource(Res.string.label_handshake_key),
                        subtitle = uiState.apiKey,
                        enabled = uiState.isReady,
                        onClick = { showApiKeyDialog = true },
                        trailing = {
                            IconButton(
                                onClick = {
                                    if (copyToClipboard(uiState.apiKey)) {
                                        ToastManager.show(keyCopiedText)
                                    }
                                },
                                enabled = uiState.apiKey.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.content_copy_24px),
                                    contentDescription = stringResource(Res.string.a11y_copy_key),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }

            adapterState?.let { adapter ->
                AdapterPushCard(
                    adapter = adapter,
                    onPush = { viewModel.pushToExtension() }
                )
            }

            // 底部留白，避开系统导航栏，避免滚到底时被遮挡
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }

        if (showPortDialog && uiState.isReady) {
            PortInputDialog(
                initialPort = uiState.port,
                onDismiss = { showPortDialog = false },
                onConfirm = {
                    viewModel.onPortChanged(it)
                    showPortDialog = false
                }
            )
        }

        if (showApiKeyDialog && uiState.isReady) {
            ApiKeyInputDialog(
                initialApiKey = uiState.apiKey,
                onDismiss = { showApiKeyDialog = false },
                onConfirm = {
                    viewModel.onApiKeyChanged(it)
                    showApiKeyDialog = false
                }
            )
        }
    }
}

/**
 * 适配器信息卡片，承载把适配代码推送到浏览器插件的按钮
 */
@Composable
private fun AdapterPushCard(
    adapter: BridgeAdapterUiState,
    onPush: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = adapter.adapterName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = adapter.description.ifBlank {
                    stringResource(Res.string.text_no_detailed_description)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = vectorResource(Res.drawable.info_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(
                        Res.string.label_contributor_format,
                        adapter.maintainer.ifBlank {
                            stringResource(Res.string.label_contributor_unknown)
                        }
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onPush,
                enabled = !adapter.isPushing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.action_push_to_extension))
            }
        }
    }
}

/**
 * 端口输入弹窗
 */
@Composable
private fun PortInputDialog(
    initialPort: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(initialPort.toString()) }

    val portValue = text.toIntOrNull()
    val isValid = portValue != null && JvmAppSettingsModel.ExtensionBridge.isValidPort(portValue)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.title_set_port)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    if (input.length <= 5 && input.all { it.isDigit() }) {
                        text = input
                    }
                },
                label = { Text(stringResource(Res.string.label_port)) },
                singleLine = true,
                isError = text.isNotEmpty() && !isValid,
                supportingText = {
                    when {
                        text.isEmpty() -> Text(stringResource(Res.string.error_input_port))
                        !isValid -> Text(
                            stringResource(Res.string.desc_port_range) + " " +
                                    "${JvmAppSettingsModel.ExtensionBridge.MIN_PORT} – " +
                                    "${JvmAppSettingsModel.ExtensionBridge.MAX_PORT}"
                        )
                        else -> Text(stringResource(Res.string.desc_bridge_restart_on_change))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            text = JvmAppSettingsModel.ExtensionBridge.DEFAULT_PORT.toString()
                        }
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.reset_settings_24px),
                            contentDescription = stringResource(Res.string.action_reset_default_port),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { portValue?.let { onConfirm(it) } },
                enabled = isValid
            ) {
                Text(stringResource(Res.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}

/**
 * 握手密钥输入弹窗
 */
@Composable
private fun ApiKeyInputDialog(
    initialApiKey: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialApiKey) }

    val isValid = text.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.title_set_api_key)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(Res.string.label_key)) },
                singleLine = true,
                isError = text.isEmpty(),
                supportingText = {
                    if (text.isEmpty()) {
                        Text(stringResource(Res.string.error_key_empty))
                    } else {
                        Text(stringResource(Res.string.desc_key_requirement))
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { text = Uuid.random().toHexString() }) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.shuffle_24px),
                            contentDescription = stringResource(Res.string.action_generate_random_key),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = isValid
            ) {
                Text(stringResource(Res.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}

/**
 * 把文本写入系统剪贴板
 */
private fun copyToClipboard(text: String): Boolean {
    return text.isNotBlank() && runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }.isSuccess
}