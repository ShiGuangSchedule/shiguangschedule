package com.xingheyuzhuan.shiguangschedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/** 多栈导航状态容器：每个一级页面持有独立 back stack，切换 tab 不清空历史。 */
class NavigationState(
    val startRoute: Destination.MainDestination,
    currentTopLevel: MutableState<Destination.MainDestination>,
    val backStacks: Map<Destination.MainDestination, NavBackStack<Destination>>
) {
    /** 当前选中的一级页面（tab）。 */
    var currentTopLevel: Destination.MainDestination by currentTopLevel

    /** 当前一级页面对应的 back stack。 */
    val currentBackStack: NavBackStack<Destination>
        get() = backStacks.getValue(currentTopLevel)

    /** 将各栈转换为带独立 decorator 的 NavEntry 列表，各 tab 状态互不干扰。 */
    @Composable
    fun toDecoratedEntries(
        entryProvider: (Destination) -> NavEntry<Destination>
    ): List<NavEntry<Destination>> {
        val decoratedEntries = backStacks.mapValues { (_, stack) ->
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<Destination>(),
                rememberViewModelStoreNavEntryDecorator<Destination>()
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider
            )
        }
        // 仅渲染当前栈，一级页面无前置 entry，系统返回可自然退出。
        return decoratedEntries.getValue(currentTopLevel)
    }
}

/** 导航操作器：负责修改 [NavigationState]，自身不保存可变状态。 */
class Navigator(private val state: NavigationState) {
    /** 切 tab 时清理来源栈；二级页面压入当前栈。 */
    fun navigate(route: Destination) {
        if (route is Destination.MainDestination) {
            if (state.currentTopLevel != route) {
                clearCurrentStack()
            }
            state.currentTopLevel = route
        } else {
            val stack = state.currentBackStack
            if (stack.lastOrNull() != route) stack.add(route)
        }
    }

    /** 外部入口专用：切到指定 tab 并把目标压入该 tab 的栈。 */
    fun navigateExternal(targetTab: Destination.MainDestination, route: Destination) {
        state.currentTopLevel = targetTab
        val stack = state.backStacks[targetTab] ?: return
        if (stack.lastOrNull() != route) stack.add(route)
    }

    /** 只弹当前栈的二级页面；栈底交由系统返回退出。 */
    fun goBack() {
        val stack = state.currentBackStack
        val top = stack.lastOrNull() ?: return
        if (top != state.currentTopLevel) {
            stack.removeLastOrNull()
        }
    }

    /** 清空当前栈的二级页面，保留栈底一级页面。 */
    fun clearCurrentStack() {
        val stack = state.currentBackStack
        while (stack.size > 1) stack.removeLastOrNull()
    }

    /** 当前真正显示的页面（当前栈顶）。 */
    fun currentRoute(): Destination =
        state.currentBackStack.lastOrNull() ?: state.currentTopLevel
}
