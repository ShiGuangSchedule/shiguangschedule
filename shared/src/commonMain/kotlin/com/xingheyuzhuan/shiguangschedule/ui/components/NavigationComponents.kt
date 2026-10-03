package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.Destination
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.account_circle_24px
import shiguangschedule.shared.generated.resources.account_circle_filled_24px
import shiguangschedule.shared.generated.resources.nav_course_schedule
import shiguangschedule.shared.generated.resources.nav_settings
import shiguangschedule.shared.generated.resources.nav_today_schedule
import shiguangschedule.shared.generated.resources.view_agenda_24px
import shiguangschedule.shared.generated.resources.view_agenda_filled_24px
import shiguangschedule.shared.generated.resources.view_week_24px
import shiguangschedule.shared.generated.resources.view_week_filled_24px

@Immutable
private data class NavItemData(
    val label: String,
    val destination: Destination,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * 自适应导航栏组件
 */
@Composable
fun AdaptiveNavigationScaffold(
    currentDestination: Destination,
    onTabSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    showNavigation: Boolean = true,
    navHideFractionProvider: () -> Float = { 0f },
    isTransparent: Boolean = false,
    navigationModifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val todayLabel = stringResource(Res.string.nav_today_schedule)
    val courseLabel = stringResource(Res.string.nav_course_schedule)
    val settingsLabel = stringResource(Res.string.nav_settings)

    val todaySelectedIcon = vectorResource(Res.drawable.view_agenda_filled_24px)
    val todayUnselectedIcon = vectorResource(Res.drawable.view_agenda_24px)
    val courseSelectedIcon = vectorResource(Res.drawable.view_week_filled_24px)
    val courseUnselectedIcon = vectorResource(Res.drawable.view_week_24px)
    val settingsSelectedIcon = vectorResource(Res.drawable.account_circle_filled_24px)
    val settingsUnselectedIcon = vectorResource(Res.drawable.account_circle_24px)

    val navItems = remember(
        todayLabel, courseLabel, settingsLabel,
        todaySelectedIcon, todayUnselectedIcon,
        courseSelectedIcon, courseUnselectedIcon,
        settingsSelectedIcon, settingsUnselectedIcon
    ) {
        listOf(
            NavItemData(
                label = todayLabel,
                destination = Destination.TodaySchedule,
                selectedIcon = todaySelectedIcon,
                unselectedIcon = todayUnselectedIcon
            ),
            NavItemData(
                label = courseLabel,
                destination = Destination.CourseSchedule,
                selectedIcon = courseSelectedIcon,
                unselectedIcon = courseUnselectedIcon
            ),
            NavItemData(
                label = settingsLabel,
                destination = Destination.Settings,
                selectedIcon = settingsSelectedIcon,
                unselectedIcon = settingsUnselectedIcon
            )
        )
    }

    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())

    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            NavigationSuiteType.NavigationRail -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = showNavigation,
                        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(tween(300)),
                        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(tween(300))
                    ) {
                        NavigationRail(
                            containerColor = if (isTransparent) Color.Transparent else NavigationRailDefaults.ContainerColor,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            navItems.forEach { item ->
                                key(item.destination) {
                                    val isSelected = currentDestination::class == item.destination::class
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = { if (!isSelected) onTabSelected(item.destination) },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = { Text(item.label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        content(PaddingValues(0.dp))
                    }
                }
            }

            NavigationSuiteType.NavigationBar -> {
                val density = LocalDensity.current
                val navBarBottomInsetPx = with(density) {
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().toPx()
                }
                val navBarTotalHeightPx = with(density) { 88.dp.toPx() } + navBarBottomInsetPx

                Box(modifier = Modifier.fillMaxSize()) {
                    content(PaddingValues(0.dp))

                    AnimatedVisibility(
                        visible = showNavigation,
                        enter = slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(tween(300)),
                        exit = slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(tween(300)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .then(navigationModifier)
                                .graphicsLayer {
                                    val fraction = navHideFractionProvider()
                                    translationY = navBarTotalHeightPx * fraction
                                    alpha = (1f - fraction).coerceIn(0f, 1f)
                                }
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isTransparent) Color.Transparent else NavigationBarDefaults.containerColor,
                                shadowElevation = 0.dp,
                                tonalElevation = 3.dp
                            ) {
                                FixedWidthNavBarContainer(
                                    navItems = navItems,
                                    currentDestination = currentDestination,
                                    onTabSelected = onTabSelected,
                                    minSpacing = 8.dp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                content(PaddingValues(0.dp))
            }
        }
    }
}

/**
 * 保持总宽度固定的导航栏容器布局
 */
@Composable
private fun FixedWidthNavBarContainer(
    navItems: List<NavItemData>,
    currentDestination: Destination,
    onTabSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    minSpacing: Dp = 8.dp
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val minSpacingPx = minSpacing.roundToPx()

        // 测量各 Tab 处于未选中和选中状态时的宽度
        val unselectedWidths = navItems.mapIndexed { index, item ->
            subcompose("measure_unselected_$index") {
                TabContent(item = item, isSelected = false, textAlpha = 0f)
            }.first().measure(constraints).width
        }

        val selectedWidths = navItems.mapIndexed { index, item ->
            subcompose("measure_selected_$index") {
                TabContent(item = item, isSelected = true, textAlpha = 1f)
            }.first().measure(constraints).width
        }

        // 计算容器最大所需宽度
        var maxCapsuleWidth = 0
        navItems.indices.forEach { selectedIndex ->
            val comboWidth = navItems.indices.sumOf { i ->
                if (i == selectedIndex) selectedWidths[i] else unselectedWidths[i]
            } + (navItems.size - 1) * minSpacingPx

            if (comboWidth > maxCapsuleWidth) {
                maxCapsuleWidth = comboWidth
            }
        }

        // 测量实际渲染的 Tab 节点
        val placeables = navItems.mapIndexed { index, item ->
            val isSelected = currentDestination::class == item.destination::class
            subcompose("real_$index") {
                key(item.destination) {
                    AnimatedTabItem(
                        item = item,
                        isSelected = isSelected,
                        collapsedWidth = with(this@SubcomposeLayout) { unselectedWidths[index].toDp() },
                        expandedWidth = with(this@SubcomposeLayout) { selectedWidths[index].toDp() },
                        onSelect = { onTabSelected(item.destination) }
                    )
                }
            }.first().measure(constraints)
        }

        val maxHeight = placeables.maxOfOrNull { it.height } ?: 0

        // 均匀分配合适的间距，保证两端对齐
        val sumTabWidths = placeables.sumOf { it.width }
        val remainingSpace = maxCapsuleWidth - sumTabWidths
        val gapCount = (placeables.size - 1).coerceAtLeast(1)
        val dynamicGap = remainingSpace.toFloat() / gapCount

        layout(maxCapsuleWidth, maxHeight) {
            var xCursor = 0f
            placeables.forEachIndexed { _, placeable ->
                placeable.placeRelative(xCursor.toInt(), 0)
                xCursor += placeable.width + dynamicGap
            }
        }
    }
}

/**
 * 动画 Tab 项容器
 */
@Composable
private fun AnimatedTabItem(
    item: NavItemData,
    isSelected: Boolean,
    collapsedWidth: Dp,
    expandedWidth: Dp,
    onSelect: () -> Unit
) {
    val animatedWidth by animateDpAsState(
        targetValue = if (isSelected) expandedWidth else collapsedWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "TabWidthAnimation"
    )

    // 根据当前的宽度计算动画展开进度 (0f ~ 1f)
    val totalDelta = (expandedWidth - collapsedWidth).value
    val currentDelta = (animatedWidth - collapsedWidth).value
    val progress = if (totalDelta > 0f) {
        (currentDelta / totalDelta).coerceIn(0f, 1f)
    } else {
        if (isSelected) 1f else 0f
    }

    val pillBgColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent

    Box(
        modifier = Modifier
            .width(animatedWidth)
            .height(40.dp)
            .clip(CircleShape)
            .background(pillBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (!isSelected) onSelect() },
        contentAlignment = Alignment.CenterStart
    ) {
        TabContent(
            item = item,
            isSelected = isSelected,
            textAlpha = progress
        )
    }
}

/**
 * Tab 内部图标和文本组件
 */
@Composable
private fun TabContent(
    item: NavItemData,
    isSelected: Boolean,
    textAlpha: Float
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    colorFilter = ColorFilter.tint(contentColor)
                }
        )

        if (textAlpha > 0f) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item.label,
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha
                }
            )
        }
    }
}