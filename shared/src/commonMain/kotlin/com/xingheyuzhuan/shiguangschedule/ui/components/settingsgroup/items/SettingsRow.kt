@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.LocalItemShapes

/**
 * 标准设置行。
 *
 * @param shapes 列表项形状，未指定时从所在分组读取
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
    shapes: ListItemShapes? = null,
) {
    val colors = ListItemDefaults.segmentedColors()
    val content: @Composable () -> Unit = { Text(text = title) }
    val supporting: (@Composable () -> Unit)? = subtitle?.let { { Text(text = it) } }
    val itemShapes = shapes ?: LocalItemShapes.current
        ?: ListItemDefaults.segmentedShapes(index = 0, count = 1)

    if (onClick != null) {
        SegmentedListItem(
            onClick = onClick,
            shapes = itemShapes,
            modifier = modifier,
            enabled = enabled,
            leadingContent = leading,
            trailingContent = trailing,
            supportingContent = supporting,
            verticalAlignment = Alignment.CenterVertically,
            colors = colors,
            elevation = ListItemDefaults.elevation(elevation = 2.dp),
            content = content
        )
    } else {
        ListItem(
            headlineContent = content,
            modifier = modifier,
            supportingContent = supporting,
            leadingContent = leading,
            trailingContent = trailing,
            colors = colors,
            shadowElevation = 2.dp
        )
    }
}

/** 设置行专用标准图标（24dp）。 */
@Composable
fun SettingsIcon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(24.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
