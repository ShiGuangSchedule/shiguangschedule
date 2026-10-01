package com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedule.navigation.PresetCourseData
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_cancel_selection
import shiguangschedule.shared.generated.resources.a11y_delete
import shiguangschedule.shared.generated.resources.a11y_enter_selection_mode
import shiguangschedule.shared.generated.resources.a11y_exit_selection_mode
import shiguangschedule.shared.generated.resources.action_add
import shiguangschedule.shared.generated.resources.action_deselect_all
import shiguangschedule.shared.generated.resources.action_select_all
import shiguangschedule.shared.generated.resources.add_24px
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.close_24px
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.done_all_24px
import shiguangschedule.shared.generated.resources.item_course_management
import shiguangschedule.shared.generated.resources.menu_open_24px
import shiguangschedule.shared.generated.resources.text_no_unique_courses_hint
import shiguangschedule.shared.generated.resources.title_selected_items_count

/**
 * 一级页面：展示所有不重复的课程名称列表 (Master View)。
 * 现使用两列网格 (LazyVerticalGrid)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseNameListScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: CourseNameListViewModel = koinViewModel()
) {
    val uniqueCourseNames by viewModel.uniqueCourseNames.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedCourseNames = remember { mutableStateListOf<String>() }

    val exitSelectionMode: () -> Unit = {
        isSelectionMode = false
        selectedCourseNames.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSelectionMode) {
                            stringResource(Res.string.title_selected_items_count, selectedCourseNames.size)
                        } else {
                            stringResource(Res.string.item_course_management)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSelectionMode) {
                                exitSelectionMode()
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        val icon = if (isSelectionMode) vectorResource(Res.drawable.close_24px) else vectorResource(Res.drawable.arrow_back_24px)
                        val description = if (isSelectionMode) {
                            stringResource(Res.string.a11y_cancel_selection)
                        } else {
                            stringResource(Res.string.a11y_back)
                        }
                        Icon(icon, contentDescription = description)
                    }
                },
                actions = {
                    val totalCount = uniqueCourseNames.size
                    val selectedCount = selectedCourseNames.size
                    val isAllSelected = totalCount > 0 && selectedCount == totalCount

                    // 1. 多选模式下显示 全选/取消全选 按钮
                    if (isSelectionMode) {
                        IconButton(
                            onClick = {
                                if (isAllSelected) {
                                    // 取消全选
                                    selectedCourseNames.clear()
                                } else {
                                    // 全选
                                    selectedCourseNames.clear()
                                    selectedCourseNames.addAll(uniqueCourseNames.map { it.name })
                                }
                            },
                            enabled = totalCount > 0
                        ) {
                            val selectAllStringRes = if (isAllSelected) Res.string.action_deselect_all else Res.string.action_select_all
                            Icon(vectorResource(Res.drawable.done_all_24px), contentDescription = stringResource(selectAllStringRes))
                        }
                    }

                    // 2. 多选模式下显示删除按钮
                    if (isSelectionMode) {
                        IconButton(
                            onClick = {
                                if (selectedCourseNames.isNotEmpty()) {
                                    coroutineScope.launch {
                                        // 调用 ViewModel 执行批量删除
                                        viewModel.deleteSelectedCourses(selectedCourseNames)
                                        // 删除成功后退出多选模式
                                        exitSelectionMode()
                                    }
                                }
                            },
                            // 选中数量为 0 时禁用删除按钮
                            enabled = selectedCourseNames.isNotEmpty()
                        ) {
                            Icon(vectorResource(Res.drawable.delete_24px), contentDescription = stringResource(Res.string.a11y_delete))
                        }
                    }

                    // 3. 常驻的切换多选模式按钮 (MenuOpen)
                    IconButton(
                        onClick = {
                            if (isSelectionMode) {
                                // 当前是多选模式，点击退出
                                exitSelectionMode()
                            } else {
                                // 当前是正常模式，点击进入多选模式
                                isSelectionMode = true
                            }
                        }
                    ) {
                        val descriptionRes = if (isSelectionMode) {
                            Res.string.a11y_exit_selection_mode
                        } else {
                            Res.string.a11y_enter_selection_mode
                        }
                        Icon(vectorResource(Res.drawable.menu_open_24px), contentDescription = stringResource(descriptionRes))
                    }
                }
            )
        },
        floatingActionButton = {
            // 多选模式下隐藏 FAB
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        // 启动协程，先发送默认数据，再导航
                        coroutineScope.launch {
                            // 1. 创建包含默认节次 (1-2) 的预设数据
                            val defaultPresetData = PresetCourseData(
                                startSection = 1,
                                endSection = 2
                            )

                            // 2. 发送数据到 Channel，解除 AddEditCourseViewModel 的阻塞
                            AddEditCourseChannel.sendEvent(defaultPresetData)

                            onNavigate(Destination.AddEditCourse(courseId = null))
                        }
                    }
                ) {
                    Icon(vectorResource(Res.drawable.add_24px), contentDescription = stringResource(Res.string.action_add))
                }
            }
        }
    ) { paddingValues ->
        if (uniqueCourseNames.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.text_no_unique_courses_hint), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            // LazyVerticalGrid 实现两列网格布局
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uniqueCourseNames, key = { it.name }) { item ->
                    val isSelected = item.name in selectedCourseNames

                    CourseNameCard(
                        name = item.name,
                        instanceCount = item.count,
                        isSelected = isSelected,
                        onCourseClick = { clickedName ->
                            if (isSelectionMode) {
                                if (selectedCourseNames.contains(clickedName)) {
                                    selectedCourseNames.remove(clickedName)
                                } else {
                                    selectedCourseNames.add(clickedName)
                                }
                            } else {
                                onNavigate(Destination.CourseManagementDetail(courseName = clickedName))
                            }
                        },
                        onCourseLongClick = { clickedName ->
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedCourseNames.add(clickedName)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 课程名称卡片 Composable
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CourseNameCard(
    name: String,
    instanceCount: Int,
    isSelected: Boolean,
    onCourseClick: (String) -> Unit,
    onCourseLongClick: (String) -> Unit
) {
    val cardColors = if (isSelected) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    val cardShape = MaterialTheme.shapes.medium

    Card(
        shape = cardShape,
        colors = cardColors,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(cardShape)
            .combinedClickable(
                onClick = { onCourseClick(name) },
                onLongClick = { onCourseLongClick(name) }
            )
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, cardShape)
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // 课程名称和实例数量
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 32.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            // 右下角：实例数量 Badge
            Badge(
                content = { Text(instanceCount.toString(), style = MaterialTheme.typography.labelSmall) },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .sizeIn(minWidth = 20.dp, minHeight = 20.dp)
            )
        }
    }
}