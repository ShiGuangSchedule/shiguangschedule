package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseConversionRepository
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_cancel_editing
import shiguangschedule.shared.generated.resources.a11y_devtools
import shiguangschedule.shared.generated.resources.a11y_enter_url
import shiguangschedule.shared.generated.resources.a11y_load
import shiguangschedule.shared.generated.resources.a11y_more_options
import shiguangschedule.shared.generated.resources.a11y_refresh
import shiguangschedule.shared.generated.resources.action_execute_import
import shiguangschedule.shared.generated.resources.action_refresh
import shiguangschedule.shared.generated.resources.action_switch_to_desktop_mode
import shiguangschedule.shared.generated.resources.action_switch_to_phone_mode
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.arrow_forward_24px
import shiguangschedule.shared.generated.resources.build_24px
import shiguangschedule.shared.generated.resources.desktop_windows_24px
import shiguangschedule.shared.generated.resources.dialog_title_select_table_for_import
import shiguangschedule.shared.generated.resources.item_devtools_debug
import shiguangschedule.shared.generated.resources.link_24px
import shiguangschedule.shared.generated.resources.more_vert_24px
import shiguangschedule.shared.generated.resources.phone_android_24px
import shiguangschedule.shared.generated.resources.placeholder_enter_url_full
import shiguangschedule.shared.generated.resources.refresh_24px
import shiguangschedule.shared.generated.resources.status_disabled
import shiguangschedule.shared.generated.resources.status_enabled
import shiguangschedule.shared.generated.resources.text_import_guide
import shiguangschedule.shared.generated.resources.title_enter_url
import shiguangschedule.shared.generated.resources.title_loading
import shiguangschedule.shared.generated.resources.toast_devtools_enabled_format
import shiguangschedule.shared.generated.resources.toast_executing_import_script
import shiguangschedule.shared.generated.resources.toast_import_script_not_found
import shiguangschedule.shared.generated.resources.toast_load_import_script_failed
import shiguangschedule.shared.generated.resources.toast_no_script_manual_import
import shiguangschedule.shared.generated.resources.toast_switched_to_desktop
import shiguangschedule.shared.generated.resources.toast_switched_to_phone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    initialUrl: String?,
    assetJsPath: String?,
    viewModel: WebViewModel = koinViewModel()
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val isDeveloperModeEnabled by viewModel.isDeveloperModeEnabled.collectAsState()
    val startedEmpty = remember { initialUrl.isNullOrBlank() || initialUrl == "about:blank" }
    val showAddressBarToggleButton = startedEmpty || isDeveloperModeEnabled

    val titleEnterUrl = stringResource(Res.string.title_enter_url)
    val titleLoading = stringResource(Res.string.title_loading)
    val toastSwitchedToDesktop = stringResource(Res.string.toast_switched_to_desktop)
    val toastSwitchedToPhone = stringResource(Res.string.toast_switched_to_phone)
    val toastNoManualImport = stringResource(Res.string.toast_no_script_manual_import)
    val toastExecutingImport = stringResource(Res.string.toast_executing_import_script)
    val toastImportNotFoundFmt = stringResource(Res.string.toast_import_script_not_found, "%s")
    val toastLoadImportFailedFmt = stringResource(Res.string.toast_load_import_script_failed, "%s")
    val statusEnabled = stringResource(Res.string.status_enabled)
    val statusDisabled = stringResource(Res.string.status_disabled)
    val toastDevToolsEnabled = stringResource(Res.string.toast_devtools_enabled_format, statusEnabled)
    val toastDevToolsDisabled = stringResource(Res.string.toast_devtools_enabled_format, statusDisabled)

    var currentUrl by remember { mutableStateOf(initialUrl ?: "about:blank") }
    var inputUrl by remember { mutableStateOf(if (startedEmpty) "" else (initialUrl ?: "")) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var pageTitle by remember { mutableStateOf(if (startedEmpty) titleEnterUrl else titleLoading) }

    var expanded by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var isEditingUrl by remember { mutableStateOf(startedEmpty) }
    var isDevToolsEnabled by remember { mutableStateOf(false) }
    var showCourseTablePicker by remember { mutableStateOf(false) }

    val webViewController = rememberWebViewController()

    val coroutineScope = rememberCoroutineScope()
    val courseConversionRepository: CourseConversionRepository = koinInject()
    val uiEventChannel = remember { Channel<WebUiEvent>(Channel.UNLIMITED) }
    val uiEventsFlow = remember(uiEventChannel) { uiEventChannel.receiveAsFlow() }

    val bridgeHandler = remember(coroutineScope, courseConversionRepository, webViewController) {
        WebBridgeHandler(
            coroutineScope = coroutineScope,
            uiEventChannel = uiEventChannel,
            courseConversionRepository = courseConversionRepository,
            onTaskCompleted = { onNavigate(Destination.CourseSchedule) },
            evaluateJs = { script, callback ->
                webViewController.evaluateJavascript(script, callback)
            }
        )
    }

    val handleBackAction: () -> Unit = {
        if (isEditingUrl) {
            isEditingUrl = false
            val rawUrl = webViewController.currentUrl
            inputUrl = if (rawUrl.isBlank() || rawUrl == "about:blank") "" else rawUrl
            keyboardController?.hide()
        } else {
            if (webViewController.canGoBack()) {
                webViewController.goBack()
            } else {
                onBack()
            }
        }
    }

    PlatformBackHandler(enabled = true, onBack = handleBackAction)

    val onSearch: (String) -> Unit = { query ->
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            val formattedUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
            keyboardController?.hide()
            currentUrl = formattedUrl
            isEditingUrl = false
            pageTitle = titleLoading
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = handleBackAction) {
                        Icon(
                            vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = stringResource(
                                if (isEditingUrl) Res.string.a11y_cancel_editing else Res.string.a11y_back
                            )
                        )
                    }
                },
                title = {
                    if (isEditingUrl) {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            placeholder = { Text(stringResource(Res.string.placeholder_enter_url_full)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    onSearch(inputUrl)
                                }
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        Text(
                            text = pageTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isEditingUrl) {
                            IconButton(
                                onClick = { onSearch(inputUrl) },
                                enabled = inputUrl.trim().isNotBlank() && inputUrl.trim() != "https://"
                            ) {
                                Icon(vectorResource(Res.drawable.arrow_forward_24px), contentDescription = stringResource(Res.string.a11y_load))
                            }
                        } else if (showAddressBarToggleButton) {
                            IconButton(onClick = {
                                isEditingUrl = true
                                val rawUrl = webViewController.currentUrl
                                inputUrl = if (rawUrl.isBlank() || rawUrl == "about:blank") "" else rawUrl
                                keyboardController?.show()
                            }) {
                                Icon(vectorResource(Res.drawable.link_24px), contentDescription = stringResource(Res.string.a11y_enter_url))
                            }
                        }

                        IconButton(onClick = { expanded = true }) {
                            Icon(vectorResource(Res.drawable.more_vert_24px), contentDescription = stringResource(Res.string.a11y_more_options))
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.action_refresh)) },
                                onClick = {
                                    webViewController.reload()
                                    expanded = false
                                },
                                leadingIcon = { Icon(vectorResource(Res.drawable.refresh_24px), contentDescription = stringResource(Res.string.a11y_refresh)) }
                            )

                            if (!isDesktopPlatform) {
                                val switchTextId = if (isDesktopMode) Res.string.action_switch_to_phone_mode else Res.string.action_switch_to_desktop_mode
                                val switchIcon = if (isDesktopMode) vectorResource(Res.drawable.phone_android_24px) else vectorResource(Res.drawable.desktop_windows_24px)

                                DropdownMenuItem(
                                    text = { Text(stringResource(switchTextId)) },
                                    onClick = {
                                        val realUrl = webViewController.currentUrl
                                        if (realUrl.isNotBlank() && realUrl != "about:blank") {
                                            currentUrl = realUrl
                                        }
                                        isDesktopMode = !isDesktopMode
                                        val tText = if (isDesktopMode) toastSwitchedToDesktop else toastSwitchedToPhone
                                        ToastManager.show(tText)
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            switchIcon,
                                            contentDescription = stringResource(switchTextId)
                                        )
                                    }
                                )
                            }

                            if (isDeveloperModeEnabled) {
                                DropdownMenuItem(
                                    onClick = {
                                        isDevToolsEnabled = !isDevToolsEnabled
                                        webViewController.setDevToolsEnabled(isDevToolsEnabled)

                                        val tText = if (isDevToolsEnabled) toastDevToolsEnabled else toastDevToolsDisabled
                                        ToastManager.show(tText)
                                    },
                                    leadingIcon = { Icon(vectorResource(Res.drawable.build_24px), contentDescription = stringResource(Res.string.a11y_devtools)) },
                                    text = { Text(stringResource(Res.string.item_devtools_debug)) },
                                    trailingIcon = { Switch(checked = isDevToolsEnabled, onCheckedChange = null) }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.text_import_guide),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.width(12.dp))

                        Button(
                            onClick = {
                                if (assetJsPath != null) {
                                    showCourseTablePicker = true
                                } else {
                                    ToastManager.show(toastNoManualImport)
                                }
                            },
                            enabled = assetJsPath != null
                        ) {
                            Text(stringResource(Res.string.action_execute_import))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            PlatformWebView(
                modifier = Modifier.fillMaxSize(),
                url = currentUrl,
                isDesktopMode = isDesktopMode,
                isDevToolsEnabled = isDevToolsEnabled,
                controller = webViewController,
                bridgeHandler = bridgeHandler,
                onProgressChange = { loadingProgress = it },
                onTitleChange = { pageTitle = it },
                onNavigateToSchedule = { onNavigate(Destination.CourseSchedule) },
                onSslError = { failingUrl, onProceed, onCancel ->
                    uiEventChannel.trySend(
                        WebUiEvent.ShowSslWarning(
                            SslErrorData(
                                primaryErrorUrl = failingUrl,
                                onProceed = onProceed,
                                onCancel = onCancel
                            )
                        )
                    )
                }
            )

            if (loadingProgress < 1.0f) {
                LinearProgressIndicator(
                    progress = { loadingProgress },
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            if (showCourseTablePicker && assetJsPath != null) {
                CourseTablePickerDialog(
                    title = stringResource(Res.string.dialog_title_select_table_for_import),
                    onDismissRequest = { showCourseTablePicker = false },
                    onTableSelected = { selectedTable ->
                        showCourseTablePicker = false
                        val tableId = selectedTable.id

                        try {
                            val fileSystem = viewModel.fileSystem
                            val jsFilePath = viewModel.filesDir / "repo" / "schools" / "resources" / assetJsPath

                            if (fileSystem.exists(jsFilePath)) {
                                val jsCode = fileSystem.read(jsFilePath) { readUtf8() }
                                bridgeHandler.setImportTableId(tableId)

                                val fullJsScript = "window.currentTableId = '$tableId';\n$jsCode"
                                webViewController.executeScript(fullJsScript)

                                ToastManager.show(toastExecutingImport)
                            } else {
                                ToastManager.show(toastImportNotFoundFmt.replace("%s", jsFilePath.toString()))
                            }
                        } catch (e: Exception) {
                            ToastManager.show(toastLoadImportFailedFmt.replace("%s", e.message ?: ""))
                        }
                    }
                )
            }
            WebDialogHost(uiEvents = uiEventsFlow)
        }
    }
}