package com.xingheyuzhuan.shiguangschedule.ui.components

import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.getString
import org.koin.core.context.GlobalContext
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_share

actual fun platformShareFile(filePath: String, mimeType: String) {
    val context = GlobalContext.get().get<android.content.Context>()

    val file = filePath.toPath().toFile()
    if (!file.exists()) return

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = mimeType
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val chooserTitle = runBlocking { getString(Res.string.action_share) }

    val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}