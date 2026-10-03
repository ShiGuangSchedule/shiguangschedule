package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.title_course_notification_settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_course_notification_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(vectorResource(Res.drawable.arrow_back_24px), contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 常规卡片由于各平台差异巨大，采用 expect 隔离由各平台自行实现
            item {
                PlatformGeneralSettingsSection(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }

    // 平台专属的弹窗派发器也通过 expect 隔离
    PlatformNotificationDialogDispatcher(
        uiState = uiState,
        viewModel = viewModel
    )
}

/**
 * 平台专属的常规设置区域声明
 */
expect @Composable
fun PlatformGeneralSettingsSection(
    uiState: NotificationSettingsUiState,
    viewModel: NotificationSettingsViewModel
)

/**
 * 平台专属的弹窗派发器声明
 */
expect @Composable
fun PlatformNotificationDialogDispatcher(
    uiState: NotificationSettingsUiState,
    viewModel: NotificationSettingsViewModel
)