package com.xingheyuzhuan.shiguangschedule.ui.components

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

object GlobalToastState {
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun emit(message: String) {
        _messages.trySend(message)
    }
}

actual fun showPlatformToast(message: String) {
    GlobalToastState.emit(message)
}