package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsIcon
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsSwitchRow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_app_icon
import shiguangschedule.shared.generated.resources.build_24px
import shiguangschedule.shared.generated.resources.developer_mode_24px
import shiguangschedule.shared.generated.resources.ic_launcher
import shiguangschedule.shared.generated.resources.item_developer_options

/**
 * 动态 App 图标头部组件（包含连续 5 次点击解锁开发者模式逻辑）
 */
@Composable
fun DynamicAppIconHeader(
    isDeveloperModeEnabled: Boolean,
    onTriggerDeveloperMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var clickCount by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.size(120.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    clickCount++
                    if (clickCount >= 5) {
                        clickCount = 0
                        onTriggerDeveloperMode()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_launcher),
                contentDescription = stringResource(Res.string.a11y_app_icon),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 开发者模式徽标：右上角
        AnimatedVisibility(
            visible = isDeveloperModeEnabled,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .offset(x = 6.dp, y = (-6).dp)
                .align(Alignment.TopEnd)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.build_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * 开发者模式开关行，带展开/收起动画。
 * 条件渲染，须单独放在一个 SettingsGroupCard 内。
 */
@Composable
fun DeveloperModeSettingItem(
    isDeveloperModeEnabled: Boolean,
    onDeveloperModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isDeveloperModeEnabled,
        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
        exit = fadeOut(tween(300)) + shrinkVertically(tween(300)),
        modifier = modifier
    ) {
        SettingsSwitchRow(
            title = stringResource(Res.string.item_developer_options),
            checked = isDeveloperModeEnabled,
            onCheckedChange = { onDeveloperModeChanged(it) },
            leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.developer_mode_24px)) }
        )
    }
}
