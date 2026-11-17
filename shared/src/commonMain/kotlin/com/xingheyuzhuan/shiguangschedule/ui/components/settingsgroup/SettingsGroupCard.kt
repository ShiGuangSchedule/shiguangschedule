package com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class SettingsGroupScope {
    internal val items = mutableListOf<@Composable () -> Unit>()

    /** 允许在分组内自由混入任意组件（Row、Switch、自定义卡片等） */
    fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}

/**
 * 短分组卡片容器（适用于常规设置页，条目较少时使用）。
 * 自动计算首尾大圆角、中间小圆角，并通过隐式上下文下发。
 *
 * @param title 分组标题（可空，若为 null 或空白则不显示标题栏）
 */
@Composable
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: SettingsGroupScope.() -> Unit
) {
    val scope = remember(content) {
        SettingsGroupScope().apply(content)
    }
    val itemCount = scope.items.size

    Column(modifier = modifier.fillMaxWidth()) {
        // 仅在标题不为空时渲染标题
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsDefaults.CardSpacing)
        ) {
            scope.items.forEachIndexed { index, itemComposable ->
                val shape = when {
                    itemCount == 1 -> RoundedCornerShape(SettingsDefaults.OuterCornerRadius)
                    index == 0 -> RoundedCornerShape(
                        topStart = SettingsDefaults.OuterCornerRadius,
                        topEnd = SettingsDefaults.OuterCornerRadius,
                        bottomStart = SettingsDefaults.InnerCornerRadius,
                        bottomEnd = SettingsDefaults.InnerCornerRadius
                    )
                    index == itemCount - 1 -> RoundedCornerShape(
                        topStart = SettingsDefaults.InnerCornerRadius,
                        topEnd = SettingsDefaults.InnerCornerRadius,
                        bottomStart = SettingsDefaults.OuterCornerRadius,
                        bottomEnd = SettingsDefaults.OuterCornerRadius
                    )
                    else -> RoundedCornerShape(SettingsDefaults.InnerCornerRadius)
                }

                CompositionLocalProvider(LocalCardShape provides shape) {
                    itemComposable()
                }
            }
        }
    }
}