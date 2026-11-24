package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.Destination
import kotlinx.coroutines.launch
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
import kotlin.math.roundToInt

private data class NavItemData(
    val label: String,
    val destination: Destination,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/** 导航栏占用的底部高度，供页面计算滚动内容留白。 */
val LocalScaffoldPadding = compositionLocalOf { PaddingValues(0.dp) }

/** 页面可请求隐藏导航栏，用于浮层与导航栏冲突的场景。 */
object NavigationBarController {
    var isHiddenRequested by mutableStateOf(false)
        private set

    fun requestHidden(hidden: Boolean) {
        isHiddenRequested = hidden
    }
}

/** 自适应导航脚手架：窄屏使用悬浮胶囊栏，中/大屏使用 Material3 官方导航组件。 */
@Composable
fun AdaptiveNavigationScaffold(
    currentDestination: Destination,
    currentTopLevel: Destination.MainDestination,
    onTabSelected: (Destination) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
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
        settingsSelectedIcon, settingsUnselectedIcon,
    ) {
        listOf(
            NavItemData(
                label = todayLabel,
                destination = Destination.TodaySchedule,
                selectedIcon = todaySelectedIcon,
                unselectedIcon = todayUnselectedIcon,
            ),
            NavItemData(
                label = courseLabel,
                destination = Destination.CourseSchedule,
                selectedIcon = courseSelectedIcon,
                unselectedIcon = courseUnselectedIcon,
            ),
            NavItemData(
                label = settingsLabel,
                destination = Destination.Settings,
                selectedIcon = settingsSelectedIcon,
                unselectedIcon = settingsUnselectedIcon,
            ),
        )
    }

    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
        currentWindowAdaptiveInfoV2()
    )

    LaunchedEffect(currentDestination) {
        NavigationBarController.requestHidden(false)
    }

    if (layoutType == NavigationSuiteType.NavigationBar) {
        CompactNavigation(
            navItems = navItems,
            currentDestination = currentDestination,
            currentTopLevel = currentTopLevel,
            onTabSelected = onTabSelected,
            content = content,
        )
    } else {
        ExpandedNavigation(
            navItems = navItems,
            currentTopLevel = currentTopLevel,
            onTabSelected = onTabSelected,
            layoutType = layoutType,
            content = content,
        )
    }
}

/** 窄屏布局：内容铺满，导航栏悬浮于底部。 */
@Composable
private fun CompactNavigation(
    navItems: List<NavItemData>,
    currentDestination: Destination,
    currentTopLevel: Destination.MainDestination,
    onTabSelected: (Destination) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val density = LocalDensity.current
    var barHeightPx by remember { mutableIntStateOf(0) }
    val clearance = with(density) { barHeightPx.toDp() }

    val showNavigation = !NavigationBarController.isHiddenRequested &&
        currentDestination is Destination.MainDestination

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalScaffoldPadding provides
                if (showNavigation) PaddingValues(bottom = clearance) else PaddingValues(0.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content(PaddingValues(0.dp))
            }
        }

        AnimatedVisibility(
            visible = showNavigation,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { barHeightPx = it.height }
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                CapsuleNavigationBar(
                    items = navItems,
                    selectedIndex = navItems.indexOfFirst { it.destination == currentTopLevel },
                    onSelect = { item -> onTabSelected(item.destination) },
                )
            }
        }
    }
}

/** 中/大屏布局：使用官方 NavigationSuite 侧边导航。 */
@Composable
private fun ExpandedNavigation(
    navItems: List<NavItemData>,
    currentTopLevel: Destination.MainDestination,
    onTabSelected: (Destination) -> Unit,
    layoutType: NavigationSuiteType,
    content: @Composable (PaddingValues) -> Unit,
) {
    val navigationState = rememberNavigationSuiteScaffoldState()
    NavigationSuiteScaffoldLayout(
        navigationSuiteType = layoutType,
        state = navigationState,
        navigationSuite = {
            NavigationSuite(
                layoutType = layoutType,
                content = {
                    navItems.forEach { item ->
                        val selected = currentTopLevel == item.destination
                        item(
                            selected = selected,
                            onClick = { if (!selected) onTabSelected(item.destination) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                },
            )
        },
        content = {
            CompositionLocalProvider(LocalScaffoldPadding provides PaddingValues(0.dp)) {
                content(PaddingValues())
            }
        },
    )
}

/** 胶囊导航栏：滑块指示器，支持点击与拖动切换。 */
@Composable
private fun CapsuleNavigationBar(
    items: List<NavItemData>,
    selectedIndex: Int,
    onSelect: (NavItemData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var measuredWidthPx by remember { mutableIntStateOf(0) }
    val itemCount = items.size.coerceAtLeast(1)
    val slotWidthPx = measuredWidthPx.toFloat() / itemCount

    val safeSelected = selectedIndex.coerceAtLeast(0)
    val latestOnSelect = rememberUpdatedState(onSelect)

    var indicatorX by remember { mutableFloatStateOf(0f) }
    var initialized by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(slotWidthPx, safeSelected) {
        if (slotWidthPx <= 0f) return@LaunchedEffect
        val target = slotWidthPx * safeSelected
        if (!initialized) {
            indicatorX = target
            initialized = true
        } else if (!dragging) {
            Animatable(indicatorX).animateTo(target, animationSpec = spring()) {
                indicatorX = value
            }
        }
    }

    val currentOffset = indicatorX

    val indicatorColor = MaterialTheme.colorScheme.secondaryContainer
    val activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer
    val inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant

    val dragModifier = Modifier.pointerInput(itemCount, slotWidthPx) {
        val maxOffset = slotWidthPx * (itemCount - 1)
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            dragging = true
            val pointerId = down.id
            indicatorX = (down.position.x - slotWidthPx / 2f).coerceIn(0f, maxOffset)

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == pointerId }
                if (change == null || !change.pressed) break
                change.consume()
                indicatorX = (change.position.x - slotWidthPx / 2f).coerceIn(0f, maxOffset)
            }

            dragging = false
            val committed = (indicatorX / slotWidthPx.coerceAtLeast(1f))
                .roundToInt().coerceIn(0, items.lastIndex)
            val target = slotWidthPx * committed
            coroutineScope.launch {
                Animatable(indicatorX).animateTo(target, animationSpec = spring()) {
                    indicatorX = value
                }
            }
            items.getOrNull(committed)?.let { latestOnSelect.value(it) }
        }
    }

    Box(
        modifier = modifier
            .width((itemCount * 96).dp)
            .height(64.dp)
            .onSizeChanged { measuredWidthPx = it.width }
            .then(dragModifier)
            .shadow(6.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(NavigationBarDefaults.containerColor),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (slotWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(indicatorX.roundToInt(), 0) }
                    .width(with(density) { slotWidthPx.toDp() })
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(indicatorColor),
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                val start = slotWidthPx * index
                val overlap = (
                    minOf(start + slotWidthPx, currentOffset + slotWidthPx) -
                        maxOf(start, currentOffset)
                    ).coerceAtLeast(0f)
                val coverage = (overlap / slotWidthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                val active = coverage > 0.5f
                val tint = if (active) activeContentColor else inactiveContentColor
                val icon = if (active) item.selectedIcon else item.unselectedIcon

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.label,
                        tint = tint,
                    )
                    Text(
                        text = item.label,
                        color = tint,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}