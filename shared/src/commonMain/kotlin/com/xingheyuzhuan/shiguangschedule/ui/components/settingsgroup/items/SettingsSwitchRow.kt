package com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.LocalCardShape

/**
 * 带开关的设置行卡片。
 *
 * @param modifier 修饰符
 * @param title 主标题
 * @param checked 当前开关状态
 * @param onCheckedChange 开关状态变化回调
 * @param subtitle 可选副标题
 * @param leading 可选左侧图标插槽
 * @param enabled 是否启用交互
 * @param shape 默认优先读取上下文 [LocalCardShape]，也支持外部显式传入（如在 LazyColumn 中）
 */
@Composable
fun SettingsSwitchRow(
    modifier: Modifier = Modifier,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    shape: RoundedCornerShape = LocalCardShape.current,
) {
    SettingsRow(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        leading = leading,
        enabled = enabled,
        shape = shape,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}