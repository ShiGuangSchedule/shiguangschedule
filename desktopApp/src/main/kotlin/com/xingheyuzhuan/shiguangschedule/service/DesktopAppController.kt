package com.xingheyuzhuan.shiguangschedule.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.koin.core.annotation.Single
import kotlin.system.exitProcess

@Single
class DesktopAppController {
    private val _showWindowEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showWindowEvent: SharedFlow<Unit> = _showWindowEvent

    fun requestShowWindow() {
        _showWindowEvent.tryEmit(Unit)
    }

    fun exitApp() {
        exitProcess(0)
    }
}