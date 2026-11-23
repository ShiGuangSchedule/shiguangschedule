package com.xingheyuzhuan.shiguangschedule

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 应用所有目的地（页面）的定义
 */
@Serializable
sealed interface Destination : NavKey {

    /** 标记一级主界面的子接口 */
    sealed interface MainDestination : Destination

    // --- 平台专属页面标记接口 ---
    sealed interface AndroidDestination : Destination
    sealed interface IosDestination : Destination
    sealed interface DesktopDestination : Destination

    // --- 一级导航页面（底栏对应页面） ---
    @Serializable data object CourseSchedule : MainDestination
    @Serializable data object Settings : MainDestination
    @Serializable data object TodaySchedule : MainDestination

    // --- 二级功能页面 ---
    @Serializable data object TimeScheduleManagement : Destination
    @Serializable data object ManageCourseTables : Destination
    @Serializable data object SchoolSelectionListScreen : Destination
    @Serializable data object CourseTableConversion : Destination
    @Serializable data object MoreOptions : Destination
    @Serializable data object OpenSourceLicenses : Destination
    @Serializable data object UpdateRepo : Destination
    @Serializable data object QuickActions : Destination
    @Serializable data object TweakSchedule : Destination
    @Serializable data object QuickDelete : Destination
    @Serializable data object ContributionList : Destination
    @Serializable data object CourseManagementList : Destination
    @Serializable data object StyleSettings : Destination
    @Serializable data object ThemeSettings : Destination
    @Serializable data object BackupAndRestore : Destination
    @Serializable data object LanguageSettings : Destination
    @Serializable data object WidgetStyleSettings : Destination
    @Serializable data object HolidayManagement : Destination

    // --- 动态传参页面 ---
    @Serializable
    data class AdapterSelection(
        val schoolId: String,
        val schoolName: String,
        val categoryNumber: Int,
        val resourceFolder: String
    ) : Destination

    @Serializable
    data class WebView(
        val initialUrl: String? = "about:blank",
        val assetJsPath: String? = null
    ) : Destination

    @Serializable
    data class AddEditCourse(
        val courseId: String? = null
    ) : Destination

    @Serializable
    data class CourseManagementDetail(
        val courseName: String
    ) : Destination

    @Serializable
    data class SingleScheduleEdit(
        val tableId: String? = null,
        val isPublic: Boolean = false,
        val copyFromId: String? = null
    ) : Destination

    @Serializable
    data class ComboScheduleEdit(
        val comboId: String? = null,
        val copyFromId: String? = null
    ) : Destination

    // Android 平台专属页面
    @Serializable data object AndroidNotificationSettings : AndroidDestination

    // Ios 平台专属页面
    @Serializable data object IosNotificationSettings : IosDestination

    // Desktop 平台专属页面
    @Serializable data object DesktopNotificationSettings : DesktopDestination

    @Serializable
    data class DesktopExtensionBridgeSettings(
        val schoolId: String? = null,
        val adapterId: String? = null
    ) : DesktopDestination
}

val Destination.isMainScreen: Boolean
    get() = this is Destination.MainDestination
