package com.xingheyuzhuan.shiguangschedule.di.platform

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberShareFileLauncher
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

/** Android 支持的能力在本包注册；不支持的能力无需声明。 */
@Module
@ComponentScan(
    "com.xingheyuzhuan.shiguangschedule.di.platform",
    "com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web"
)
actual class PlatformBridgeModule

/** Android 12 (API 31) 起支持系统动态取色。 */
@Singleton
class AndroidDynamicColorSupport : DynamicColorSupport {
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    override val supported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

/** Android 文件分享实现。 */
@Singleton
class AndroidShareFileSupport : ShareFileSupport {
    @Composable
    override fun rememberShareAction(): (filePath: String) -> Unit {
        val launcher = rememberShareFileLauncher(
            onError = { failure -> failure.printStackTrace() }
        )
        return { filePath -> launcher.launch(PlatformFile(filePath)) }
    }
}
