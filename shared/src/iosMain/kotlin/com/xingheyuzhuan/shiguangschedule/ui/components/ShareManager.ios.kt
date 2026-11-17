package com.xingheyuzhuan.shiguangschedule.ui.components

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindowScene

actual fun platformShareFile(filePath: String, mimeType: String) {
    val fileUrl = NSURL.fileURLWithPath(filePath)
    val activityItems = listOf(fileUrl)

    val activityViewController = UIActivityViewController(
        activityItems = activityItems,
        applicationActivities = null
    )

    // 获取当前处于激活状态的 UIViewController 来弹出分享面板
    val windowScene = UIApplication.sharedApplication.connectedScenes
        .firstOrNull { it is UIWindowScene } as? UIWindowScene
    val rootViewController = windowScene?.windows
        .orEmpty()
        .map { it as? platform.UIKit.UIWindow }
        .firstOrNull { it?.isKeyWindow() == true }
        ?.rootViewController

    rootViewController?.presentViewController(
        activityViewController,
        animated = true,
        completion = null
    )
}