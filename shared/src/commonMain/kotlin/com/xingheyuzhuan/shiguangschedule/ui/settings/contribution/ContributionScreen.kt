package com.xingheyuzhuan.shiguangschedule.ui.settings.contribution

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedule.data.model.ContributionList
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.SettingsGroup
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.SettingsGroupLazyColumn
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsIcon
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsRow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back_to_previous
import shiguangschedule.shared.generated.resources.a11y_contributor_avatar
import shiguangschedule.shared.generated.resources.action_retry
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.github_24px
import shiguangschedule.shared.generated.resources.tab_adapter_development
import shiguangschedule.shared.generated.resources.tab_app_development
import shiguangschedule.shared.generated.resources.text_loading_failed
import shiguangschedule.shared.generated.resources.text_no_contributors
import shiguangschedule.shared.generated.resources.title_contribution_list

/**
 * 贡献者列表主界面
 *
 * @param onBack 返回上一页回调
 * @param viewModel 状态管理 ViewModel
 */
@Composable
fun ContributionScreen(
    onBack: () -> Unit,
    viewModel: ContributionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_contribution_list)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = stringResource(Res.string.a11y_back_to_previous)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    end = paddingValues.calculateEndPadding(layoutDirection)
                )
        ) {
            ContributionTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = viewModel::selectTab
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    ContributionUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    is ContributionUiState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ErrorMessage(
                                message = state.message,
                                onRetry = viewModel::loadContributions
                            )
                        }
                    }

                    is ContributionUiState.Success -> {
                        val listToShow = when (selectedTabIndex) {
                            0 -> state.data.jiaowuadapter
                            1 -> state.data.appDev
                            else -> emptyList()
                        }
                        ContributorListContent(
                            list = listToShow,
                            onContributorClick = { url ->
                                uriHandler.openUri(url)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 分类选项卡栏
 */
@Composable
fun ContributionTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        stringResource(Res.string.tab_adapter_development),
        stringResource(Res.string.tab_app_development)
    )

    PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        style = if (selectedTabIndex == index) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.bodyMedium
                        }
                    )
                }
            )
        }
    }
}

/**
 * 贡献者列表内容。每位贡献者单独成组，卡片四周均为大圆角。
 */
@Composable
fun ContributorListContent(
    list: List<ContributionList.Contributor>,
    onContributorClick: (String) -> Unit
) {
    if (list.isEmpty()) {
        Text(
            text = stringResource(Res.string.text_no_contributors),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
        return
    }

    val navigationBarBottom = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    SettingsGroupLazyColumn(
        groups = list.map { contributor -> SettingsGroup(items = listOf(contributor)) },
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = navigationBarBottom
        )
    ) { contributor, shape ->
        ContributorRow(
            contributor = contributor,
            shape = shape,
            onContributorClick = onContributorClick
        )
    }
}

/**
 * 单个贡献者行，左右均为图标：左侧头像，右侧 GitHub 图标。
 */
@Composable
fun ContributorRow(
    contributor: ContributionList.Contributor,
    shape: RoundedCornerShape,
    onContributorClick: (String) -> Unit
) {
    val a11yAvatar = stringResource(Res.string.a11y_contributor_avatar, contributor.name)

    val avatarBytes by produceState<ByteArray?>(initialValue = null, contributor.avatar) {
        value = try {
            Res.readBytes("files/contributors_data/${contributor.avatar}")
        } catch (_: Exception) {
            null
        }
    }

    SettingsRow(
        title = contributor.name,
        leading = {
            AsyncImage(
                model = avatarBytes,
                contentDescription = a11yAvatar,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        },
        trailing = {
            SettingsIcon(imageVector = vectorResource(Res.drawable.github_24px))
        },
        onClick = { onContributorClick(contributor.url) },
        shape = shape
    )
}

/**
 * 加载失败的错误提示页面
 */
@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    val textLoadingFailed = stringResource(Res.string.text_loading_failed, message)
    val actionRetry = stringResource(Res.string.action_retry)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = textLoadingFailed,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(onClick = onRetry) {
            Text(actionRetry)
        }
    }
}