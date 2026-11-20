package com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.SettingsGroupCard
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsIcon
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsRow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.adjust_24px
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.chevron_right_24px
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.desc_schedule_tweak
import shiguangschedule.shared.generated.resources.item_quick_actions
import shiguangschedule.shared.generated.resources.item_quick_delete
import shiguangschedule.shared.generated.resources.item_schedule_tweak
import shiguangschedule.shared.generated.resources.label_quick_action_category_schedule
import shiguangschedule.shared.generated.resources.quick_delete_subtitle

/**
 * 快捷操作二级页面
 * 用于收纳高频使用的工具类功能，如调课、临时改动等
 */
@Composable
fun QuickActionsScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit
) {
    val settingPadding = 16.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.item_quick_actions)) },
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = settingPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsGroupCard(
                modifier = Modifier.padding(top = 8.dp),
                title = stringResource(Res.string.label_quick_action_category_schedule)
            ) {
                // 调课功能项
                item {
                    SettingsRow(
                        title = stringResource(Res.string.item_schedule_tweak),
                        subtitle = stringResource(Res.string.desc_schedule_tweak),
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.adjust_24px)) },
                        trailing = { SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px)) },
                        onClick = { onNavigate(Destination.TweakSchedule) }
                    )
                }
                // 快速删除功能项
                item {
                    SettingsRow(
                        title = stringResource(Res.string.item_quick_delete),
                        subtitle = stringResource(Res.string.quick_delete_subtitle),
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.delete_24px)) },
                        trailing = { SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px)) },
                        onClick = { onNavigate(Destination.QuickDelete) }
                    )
                }
            }

            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}
