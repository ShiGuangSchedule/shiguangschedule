package com.xingheyuzhuan.shiguangschedule

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.xingheyuzhuan.shiguangschedule.data.model.StartScreen
import com.xingheyuzhuan.shiguangschedule.tool.ExternalFileManager
import com.xingheyuzhuan.shiguangschedule.ui.components.AdaptiveNavigationScaffold
import com.xingheyuzhuan.shiguangschedule.ui.schedule.WeeklyScheduleScreen
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list.AdapterSelectionScreen
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list.SchoolSelectionListScreen
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.WebViewScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsViewModel
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.LanguageSettingScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.MoreOptionsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.OpenSourceLicensesScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.backup.BackupScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.contribution.ContributionScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.conversion.CourseTableConversionScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.course.AddEditCourseScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement.CourseInstanceListScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement.CourseNameListScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursetables.ManageCourseTablesScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.holiday.HolidayManagementScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.QuickActionsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.delete.QuickDeleteScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.tweaks.TweakScheduleScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.style.StyleSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.style.widget.WidgetStyleSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.themesettings.ThemeSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.ComboScheduleEditScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.SingleScheduleEditScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.TimeScheduleManagementScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.update.UpdateRepoScreen
import com.xingheyuzhuan.shiguangschedule.ui.theme.ShiguangScheduleTheme
import com.xingheyuzhuan.shiguangschedule.ui.today.TodayScheduleScreen
import kotlinx.serialization.serializer
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()

    if (state.isReady) {
        ShiguangScheduleTheme(settings = state.appSettings) {
            val startDest = remember(state.appSettings.startScreen) {
                when (state.appSettings.startScreen) {
                    StartScreen.COURSE_SCHEDULE -> Destination.CourseSchedule
                    StartScreen.TODAY_SCHEDULE -> Destination.TodaySchedule
                }
            }
            AppNavigation(startDestination = startDest)
        }
    } else {
        Surface(modifier = Modifier.fillMaxSize()) {}
    }
}

@Composable
fun AppNavigation(startDestination: Destination) {
    val startTopLevel = startDestination as? Destination.MainDestination
        ?: Destination.CourseSchedule

    val topLevelRoutes = remember {
        listOf<Destination.MainDestination>(
            Destination.CourseSchedule,
            Destination.TodaySchedule,
            Destination.Settings
        )
    }

    // 每个一级页面一个独立且可持久化的 typed 栈
    val backStacks = topLevelRoutes.associateWith { start ->
        rememberSerializable(serializer = serializer()) {
            NavBackStack<Destination>(start)
        }
    }

    val currentTopLevelState = remember {
        mutableStateOf<Destination.MainDestination>(startTopLevel)
    }
    // 跨重组保持稳定，使导航回调引用稳定
    val navigationState = remember(startTopLevel, topLevelRoutes) {
        NavigationState(startTopLevel, currentTopLevelState, backStacks)
    }
    val navigator = remember(navigationState) { Navigator(navigationState) }

    val currentDestination = navigator.currentRoute()

    val pendingFile by ExternalFileManager.pendingFile.collectAsState()

    val onNavigate: (Destination) -> Unit = remember(navigator) {
        { dest -> navigator.navigate(dest) }
    }

    LaunchedEffect(pendingFile) {
        val target = pendingFile?.targetDestination
        if (target != null) {
            // 外部文件统一压入 Settings 栈
            navigator.navigateExternal(Destination.Settings, target)
        }
    }

    val onBack: () -> Unit = remember(navigator) {
        { navigator.goBack() }
    }

    // 判断当前是否为移动端窄屏（NavigationBar 模式）
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
    val isCompact = layoutType == NavigationSuiteType.NavigationBar

    // 动画规格定义
    val slideAnimSpec = tween<IntOffset>(300)

    val transitionSpec = remember(isCompact) {
        if (isCompact) {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = slideAnimSpec) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = slideAnimSpec) + fadeOut()
        } else {
            fadeIn(animationSpec = tween(150)) togetherWith ExitTransition.None
        }
    }

    val popTransitionSpec = remember(isCompact) {
        if (isCompact) {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = slideAnimSpec) + fadeIn() togetherWith
                    slideOutHorizontally(targetOffsetX = { it }, animationSpec = slideAnimSpec)
        } else {
            fadeIn(animationSpec = tween(150)) togetherWith ExitTransition.None
        }
    }

    val predictivePopTransitionSpec = remember(isCompact) {
        if (isCompact) {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = slideAnimSpec) + fadeIn() togetherWith
                    slideOutHorizontally(targetOffsetX = { it }, animationSpec = slideAnimSpec)
        } else {
            fadeIn(animationSpec = tween(150)) togetherWith ExitTransition.None
        }
    }

    AdaptiveNavigationScaffold(
        currentDestination = currentDestination,
        currentTopLevel = navigationState.currentTopLevel,
        onTabSelected = onNavigate,
    ) { _ ->
        val entryProvider = entryProvider<Destination>(
            fallback = { key ->
                NavEntry(key) {
                    if (key is Destination.AndroidDestination ||
                        key is Destination.IosDestination ||
                        key is Destination.DesktopDestination
                    ) {
                        // 平台专属页面但当前平台未实现 → 自动弹回
                        LaunchedEffect(key) { onBack() }
                    } else {
                        // 通用页面漏注册 → 暴露问题
                        Text("未知页面: $key")
                    }
                }
            }
        ) {
            commonEntryBuilder(
                onNavigate = onNavigate,
                onBack = onBack,
            )
            platformEntryBuilder(
                onNavigate = onNavigate,
                onBack = onBack
            )
        }

        NavDisplay(
            entries = navigationState.toDecoratedEntries(entryProvider),
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { transitionSpec },
            popTransitionSpec = { popTransitionSpec },
            predictivePopTransitionSpec = { predictivePopTransitionSpec }
        )
    }
}

/**
 * 平台页面注册入口：各平台 source set 实现，只注册本平台专属页面。
 */
expect fun EntryProviderScope<Destination>.platformEntryBuilder(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit
)

/**
 * 通用页面注册入口：所有平台共享的页面在此统一声明。
 * 新增通用页面只需在下方加一行 entry<T> { ... }。
 */
fun EntryProviderScope<Destination>.commonEntryBuilder(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
) {
    entry<Destination.CourseSchedule> {
        WeeklyScheduleScreen(
            onNavigate = onNavigate,
            onBack = onBack,
        )
    }
    entry<Destination.Settings> { SettingsScreen(onNavigate, onBack) }
    entry<Destination.TodaySchedule> { TodayScheduleScreen(onNavigate, onBack) }
    entry<Destination.ManageCourseTables> { ManageCourseTablesScreen(onBack) }
    entry<Destination.SchoolSelectionListScreen> { SchoolSelectionListScreen(onNavigate, onBack) }
    entry<Destination.CourseTableConversion> { CourseTableConversionScreen(onNavigate, onBack) }
    entry<Destination.MoreOptions> { MoreOptionsScreen(onNavigate, onBack) }
    entry<Destination.OpenSourceLicenses> { OpenSourceLicensesScreen(onBack) }
    entry<Destination.UpdateRepo> { UpdateRepoScreen(onBack) }
    entry<Destination.QuickActions> { QuickActionsScreen(onNavigate, onBack) }
    entry<Destination.TweakSchedule> { TweakScheduleScreen(onBack) }
    entry<Destination.ContributionList> { ContributionScreen(onBack) }
    entry<Destination.CourseManagementList> { CourseNameListScreen(onNavigate, onBack) }
    entry<Destination.StyleSettings> { StyleSettingsScreen(onNavigate, onBack) }
    entry<Destination.QuickDelete> { QuickDeleteScreen(onBack) }
    entry<Destination.ThemeSettings> { ThemeSettingsScreen(onBack) }
    entry<Destination.BackupAndRestore> { BackupScreen(onBack) }
    entry<Destination.LanguageSettings> { LanguageSettingScreen(onBack) }
    entry<Destination.WidgetStyleSettings> { WidgetStyleSettingsScreen(onBack) }
    entry<Destination.HolidayManagement> { HolidayManagementScreen(onBack = onBack) }

    entry<Destination.TimeScheduleManagement> {
        TimeScheduleManagementScreen(
            onBack = onBack,
            onEditSingleSchedule = { tableId, isPublic, copyFromId ->
                onNavigate(Destination.SingleScheduleEdit(tableId, isPublic, copyFromId))
            },
            onEditComboSchedule = { comboId, copyFromId ->
                onNavigate(Destination.ComboScheduleEdit(comboId, copyFromId))
            }
        )
    }

    // 单一/公共作息编辑页面路由（key 已自动为具体类型）
    entry<Destination.SingleScheduleEdit> { key ->
        SingleScheduleEditScreen(
            tableId = key.tableId,
            isPublic = key.isPublic,
            copyFromId = key.copyFromId,
            onBack = onBack
        )
    }

    // 组合作息编辑页面路由
    entry<Destination.ComboScheduleEdit> { key ->
        ComboScheduleEditScreen(
            comboId = key.comboId,
            copyFromId = key.copyFromId,
            onBack = onBack
        )
    }

    entry<Destination.AdapterSelection> { key ->
        AdapterSelectionScreen(
            onNavigate, onBack, key.schoolId, key.schoolName, key.categoryNumber, key.resourceFolder
        )
    }
    entry<Destination.WebView> { key ->
        WebViewScreen(onNavigate, onBack, key.initialUrl, key.assetJsPath)
    }
    entry<Destination.AddEditCourse> { key ->
        AddEditCourseScreen(onBack, key.courseId)
    }
    entry<Destination.CourseManagementDetail> { key ->
        CourseInstanceListScreen(key.courseName, onBack, onNavigate)
    }
}