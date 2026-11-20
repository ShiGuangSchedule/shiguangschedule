package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.tool.UpdateChecker
import com.xingheyuzhuan.shiguangschedule.tool.UpdatePlatform
import com.xingheyuzhuan.shiguangschedule.tool.UpdateStatus
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.SettingsGroupCard
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsIcon
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsRow
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.app_name
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.chevron_right_24px
import shiguangschedule.shared.generated.resources.favorite_24px
import shiguangschedule.shared.generated.resources.github_24px
import shiguangschedule.shared.generated.resources.home_24px
import shiguangschedule.shared.generated.resources.item_check_software_update
import shiguangschedule.shared.generated.resources.item_contributors
import shiguangschedule.shared.generated.resources.item_github_repo
import shiguangschedule.shared.generated.resources.item_language_settings
import shiguangschedule.shared.generated.resources.item_open_source_licenses
import shiguangschedule.shared.generated.resources.item_start_screen_settings
import shiguangschedule.shared.generated.resources.item_update_repo
import shiguangschedule.shared.generated.resources.label_special_thanks
import shiguangschedule.shared.generated.resources.label_version_prefix
import shiguangschedule.shared.generated.resources.language_24px
import shiguangschedule.shared.generated.resources.list_alt_24px
import shiguangschedule.shared.generated.resources.palette_24px
import shiguangschedule.shared.generated.resources.people_alt_24px
import shiguangschedule.shared.generated.resources.text_acknowledgment_body
import shiguangschedule.shared.generated.resources.theme_settings_title
import shiguangschedule.shared.generated.resources.title_more_options
import shiguangschedule.shared.generated.resources.update_24px

private const val GITHUB_REPO_URL = "https://github.com/ShiGuangSchedule/shiguangschedule"

@Composable
fun MoreOptionsScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    updateChecker: UpdateChecker = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    // 从 Koin 动态获取注入的版本号
    val appVersionName: String = koinInject(named("AppVersionName"))

    // 状态观察
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDeveloperModeEnabled = uiState.appSettings.developerModeEnabled

    // 更新逻辑相关状态
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var selectedPlatform by remember { mutableStateOf(UpdatePlatform.GITEE) }

    // 弹窗可见性控制
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf(false) }
    var showStartScreenDialog by remember { mutableStateOf(false) }

    // 逻辑：执行更新检查
    val startUpdateCheck: (UpdatePlatform) -> Unit = { platform ->
        selectedPlatform = platform
        showChannelDialog = false
        updateStatus = UpdateStatus.Checking
        showUpdateDialog = true
        coroutineScope.launch {
            updateStatus = updateChecker.checkUpdate(platform, appVersionName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.title_more_options)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = stringResource(Res.string.a11y_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 应用信息头部
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DynamicAppIconHeader(
                    isDeveloperModeEnabled = isDeveloperModeEnabled,
                    onTriggerDeveloperMode = { viewModel.onDeveloperModeChanged(true) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = stringResource(Res.string.label_version_prefix, appVersionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 开发者模式开关，条件渲染，需单独成组
            SettingsGroupCard {
                item {
                    DeveloperModeSettingItem(
                        isDeveloperModeEnabled = isDeveloperModeEnabled,
                        onDeveloperModeChanged = { viewModel.onDeveloperModeChanged(it) }
                    )
                }
            }

            // 设置列表
            SettingsGroupCard {
                // 检查更新
                item {
                    SettingsRow(
                        title = stringResource(Res.string.item_check_software_update),
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.update_24px)) },
                        trailing = { SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px)) },
                        onClick = { showChannelDialog = true }
                    )
                }

                // 语言切换
                item {
                    SettingsRow(
                        title = stringResource(Res.string.item_language_settings),
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.language_24px)) },
                        trailing = { SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px)) },
                        onClick = { onNavigate(Destination.LanguageSettings) }
                    )
                }

                // 主题设置
                item {
                    SettingsRow(
                        title = stringResource(Res.string.theme_settings_title),
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.palette_24px)) },
                        trailing = { SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px)) },
                        onClick = { onNavigate(Destination.ThemeSettings) }
                    )
                }

                // 启动页面设置
                item {
                    SettingsRow(
                        title = stringResource(Res.string.item_start_screen_settings),
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.home_24px)) },
                        trailing = {
                            Text(
                                text = stringResource(uiState.appSettings.startScreen.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = { showStartScreenDialog = true }
                    )
                }

                // GitHub 仓库
                item {
                    SettingsRow(
                        title = stringResource(Res.string.item_github_repo),
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.github_24px)) },
                        trailing = { SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px)) },
                        onClick = { uriHandler.openUri(GITHUB_REPO_URL) }
                    )
                }

                // 开源许可证
                item {
                    SettingsRow(
                        title = stringResource(Res.string.item_open_source_licenses),
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.list_alt_24px)) },
                        trailing = { SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px)) },
                        onClick = { onNavigate(Destination.OpenSourceLicenses) }
                    )
                }

                // 更新适配仓库
                item {
                    SettingsRow(
                        title = stringResource(Res.string.item_update_repo),
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.update_24px)) },
                        trailing = { SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px)) },
                        onClick = { onNavigate(Destination.UpdateRepo) }
                    )
                }

                // 贡献者
                item {
                    SettingsRow(
                        title = stringResource(Res.string.item_contributors),
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.people_alt_24px)) },
                        trailing = { SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px)) },
                        onClick = { onNavigate(Destination.ContributionList) }
                    )
                }
            }

            // 特别鸣谢
            SettingsGroupCard {
                item {
                    SettingsRow(
                        title = stringResource(Res.string.label_special_thanks),
                        subtitle = stringResource(Res.string.text_acknowledgment_body),
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.favorite_24px)) }
                    )
                }
            }

            // 底部导航栏避让，作为可滚动列表的一部分
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    // --- 弹窗逻辑 ---

    // 启动页切换弹窗
    StartScreenSelectionDialog(
        showDialog = showStartScreenDialog,
        currentSelected = uiState.appSettings.startScreen,
        onDismiss = { showStartScreenDialog = false },
        onConfirm = {
            viewModel.onStartScreenChanged(it)
            showStartScreenDialog = false
        }
    )

    // 检查更新结果弹窗
    UpdateResultDialog(
        showDialog = showUpdateDialog,
        updateStatus = updateStatus,
        onDismiss = {
            showUpdateDialog = false
            if (updateStatus !is UpdateStatus.Found) updateStatus = UpdateStatus.Idle
        },
        onDownloadClick = { targetUrl ->
            updateChecker.launchUpdate(targetUrl)
        }
    )

    // 更新渠道选择弹窗
    ChannelSelectionDialog(
        showDialog = showChannelDialog,
        currentSelected = selectedPlatform,
        onDismiss = { showChannelDialog = false },
        onConfirm = startUpdateCheck
    )
}