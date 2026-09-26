package com.xingheyuzhuan.shiguangschedule.ui.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object AndroidToastHelper : KoinComponent {
    val context: Context by inject()
    val mainHandler = Handler(Looper.getMainLooper())
}

actual fun showPlatformToast(message: String) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Toast.makeText(AndroidToastHelper.context, message, Toast.LENGTH_SHORT).show()
    } else {
        AndroidToastHelper.mainHandler.post {
            Toast.makeText(AndroidToastHelper.context, message, Toast.LENGTH_SHORT).show()
        }
    }
}