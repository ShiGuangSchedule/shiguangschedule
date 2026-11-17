package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.item_language_settings
import shiguangschedule.shared.generated.resources.language_follow_system
import shiguangschedule.shared.generated.resources.language_names
import shiguangschedule.shared.generated.resources.language_tags

/**
 * 跨平台语言设置契约声明
 */
expect object PlatformLocaleManager {
    fun setLanguageTag(tag: String)
    fun getCurrentLanguageTag(): String
}

/**
 * 语言数据实体（仅限本文件内部解析使用）
 */
private data class LanguageItem(
    val name: String,
    val tag: String
)

/**
 * 独立语言设置页面组件
 */
@Composable
fun LanguageSettingScreen(
    onBack: () -> Unit
) {
    // 获取底层平台当前生效的语言 Tag
    var currentTag by remember {
        mutableStateOf(PlatformLocaleManager.getCurrentLanguageTag())
    }

    // 动态读取 languages.xml 中的两个 string-array 资源并组装
    val tags = stringArrayResource(Res.array.language_tags)
    val names = stringArrayResource(Res.array.language_names)
    val followSystemText = stringResource(Res.string.language_follow_system)
    val scrollState = rememberScrollState()

    val languageList = remember(tags, names, followSystemText) {
        buildList {
            add(LanguageItem(followSystemText, ""))
            addAll(names.zip(tags) { name, tag -> LanguageItem(name, tag) })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.item_language_settings)) },
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
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            languageList.forEach { item ->
                // 判断单选选中状态
                val isSelected = if (item.tag.isEmpty()) {
                    currentTag.isEmpty()
                } else {
                    currentTag.startsWith(item.tag)
                }

                ListItem(
                    modifier = Modifier.clickable {
                        if (!isSelected) {
                            currentTag = item.tag
                            // 平台方法更新系统/平台语言
                            PlatformLocaleManager.setLanguageTag(item.tag)
                        }
                    },
                    headlineContent = { Text(text = item.name) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        RadioButton(
                            selected = isSelected,
                            onClick = null
                        )
                    }
                )
            }
        }
    }
}