package com.xingheyuzhuan.shiguangschedule.tool

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import okio.use
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import shiguangschedule.shared.generated.resources.Res

/**
 * 资源与缓存初始化管理器。
 *
 * 负责应用启动时的静态离线资源解压校验，以及过期的分享与下载临时缓存清理。
 */
@Suppress("unused")
@Single(createdAtStart = true)
class ResourceInitializerManager(
    private val fileSystem: FileSystem,
    @Named("FilesDir") private val filesDir: Path,
    @Named("CacheDir") private val cacheDir: Path
) {
    private val targetRepoDir: Path = filesDir / "repo"
    private val shareTempDir: Path = cacheDir / "share_temp"

    /**
     * App 内置适配资源：Compose 资源路径 → 相对 `<filesDir>/repo` 的落盘路径。
     *
     * 内置适配脚本不走 offline_schools.zip：zip 只在首次启动解压一次，App 升级后新增的
     * 内置脚本无法补进已有安装。放在这里每次启动按内容补齐，升级即可生效。
     */
    private val builtinResourceMap: Map<String, String> = mapOf(
        "files/builtin_NWAFU_wdkbby.js" to "schools/resources/NWAFU/wdkbby.js"
    )

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initializeOfflineRepo()
            ensureBuiltinAdapters()
            clearTempCaches()
        }
    }

    /**
     * 校验并解压内置离线适配仓库。
     *
     * @param forceOverwrite 是否强制清除并重新解压覆盖现有本地仓库
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun initializeOfflineRepo(forceOverwrite: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!forceOverwrite && fileSystem.exists(targetRepoDir / "index")) {
                return@runCatching
            }

            val zipBytes = Res.readBytes("files/offline_schools.zip")
            val tempZipFile = filesDir / "temp_offline_schools.zip"

            fileSystem.write(tempZipFile) {
                write(zipBytes)
            }

            try {
                val zipFileSystem = fileSystem.openZip(tempZipFile)

                if (fileSystem.exists(targetRepoDir)) {
                    fileSystem.deleteRecursively(targetRepoDir)
                }
                fileSystem.createDirectories(targetRepoDir)

                unzipDirectory(zipFileSystem, "/".toPath(), targetRepoDir)
            } finally {
                fileSystem.delete(tempZipFile)
            }
        }
    }

    /**
     * 校验并补齐 App 内置适配脚本等资源（幂等：内容一致时不写盘）。
     *
     * 对应学校索引由 [SchoolRepository] 在读取索引时叠加内置片段提供，这里只负责把脚本文件
     * 放到 WebView 导入时约定的位置 `<filesDir>/repo/schools/resources/<学校>/<脚本>`。
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun ensureBuiltinAdapters(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            builtinResourceMap.forEach { (resourcePath, relativeTargetPath) ->
                val targetPath = targetRepoDir / relativeTargetPath
                val bytes = Res.readBytes(resourcePath)

                val isUpToDate = fileSystem.exists(targetPath) &&
                        fileSystem.read(targetPath) { readByteArray() }.contentEquals(bytes)

                if (!isUpToDate) {
                    targetPath.parent?.let { fileSystem.createDirectories(it) }
                    fileSystem.write(targetPath) { write(bytes) }
                }
            }
        }
    }

    /**
     * 清理应用生成的临时文件与过期缓存目录。
     */
    suspend fun clearTempCaches(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (fileSystem.exists(shareTempDir)) {
                fileSystem.deleteRecursively(shareTempDir)
            }

            val tempSchoolsRepo = cacheDir / "temp_schools_repo"
            val tempIndexRepo = cacheDir / "temp_index_repo"
            if (fileSystem.exists(tempSchoolsRepo)) fileSystem.deleteRecursively(tempSchoolsRepo)
            if (fileSystem.exists(tempIndexRepo)) fileSystem.deleteRecursively(tempIndexRepo)
        }
    }

    /**
     * 递归解压 Zip 虚拟文件系统中的目录与文件。
     */
    private fun unzipDirectory(
        zipFileSystem: FileSystem,
        currentZipPath: Path,
        targetDir: Path
    ) {
        val entries = zipFileSystem.list(currentZipPath)
        for (entry in entries) {
            val destinationPath = targetDir / entry.name

            if (!destinationPath.toString().startsWith(targetDir.toString())) {
                throw IllegalArgumentException("Illegal zip path: ${entry.name}")
            }

            val metadata = zipFileSystem.metadata(entry)

            if (metadata.isDirectory) {
                fileSystem.createDirectories(destinationPath)
                unzipDirectory(zipFileSystem, entry, destinationPath)
            } else {
                destinationPath.parent?.let { fileSystem.createDirectories(it) }

                zipFileSystem.source(entry).use { source ->
                    fileSystem.sink(destinationPath).buffer().use { sink ->
                        sink.writeAll(source)
                    }
                }
            }
        }
    }
}