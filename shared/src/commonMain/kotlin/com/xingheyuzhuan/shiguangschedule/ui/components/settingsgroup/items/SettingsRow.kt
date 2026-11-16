package com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.LocalCardShape
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.SettingsDefaults

/**
 * 标准设置行卡片。
 *
 * @param shape 默认优先读取上下文 [LocalCardShape]，也支持外部显式传入（如在 LazyColumn 中）
 */
@Composable
fun SettingsRow(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: RoundedCornerShape = LocalCardShape.current,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SettingsDefaults.ItemMinHeight)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(enabled = enabled, onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .padding(
                    horizontal = SettingsDefaults.ItemHorizontalPadding,
                    vertical = SettingsDefaults.ItemVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(SettingsDefaults.SlotSpacing))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(SettingsDefaults.SlotSpacing))
                trailing()
            }
        }
    }
}

/**
 * 设置行专用的标准图标组件（24dp）。
 */
@Composable
fun SettingsIcon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = modifier.size(SettingsDefaults.IconSize),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}