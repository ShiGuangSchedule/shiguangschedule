package com.xingheyuzhuan.shiguangschedule.di.platform

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberShareFileLauncher
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

/** iOS 支持的能力在本包注册；不支持的能力无需声明。 */
@Module
@ComponentScan
actual class PlatformBridgeModule

/** iOS 文件分享实现。 */
@Singleton
class IosShareFileSupport : ShareFileSupport {
    @Composable
    override fun rememberShareAction(): (filePath: String) -> Unit {
        val launcher = rememberShareFileLauncher(
            onError = { failure -> failure.printStackTrace() }
        )
        return { filePath -> launcher.launch(PlatformFile(filePath)) }
    }
}
