package com.xingheyuzhuan.shiguangschedule.data.repository

import com.xingheyuzhuan.shiguangschedule.data.model.ContributionList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import shiguangschedule.shared.generated.resources.Res


/**
 * 贡献者数据仓库
 */
@Single
class ContributionRepository {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * 读取贡献者 JSON 数据并反序列化。
     *
     * 注意：`files/contributors_data/contributors.json` 由 CI 生成
     * （`.github/scripts/build_contributors.sh`，由 ci-build.yml 的 prepare-resources 任务调用），
     * 本地直接构建（含 Android Studio）默认不含该资源。
     * 因此资源缺失时返回空列表，让页面显示「暂无贡献者」而不是报错；
     * 只有文件存在却解析失败时才抛错，便于发现数据格式问题。
     * 需要在本地构建里看到真实贡献者数据时，运行 `python tools/gen_contributors.py`。
     */
    suspend fun getContributions(): ContributionList = withContext(Dispatchers.IO) {
        val bytes = try {
            // 直接跨平台读取打包进去的静态资源文件
            Res.readBytes("files/contributors_data/contributors.json")
        } catch (e: Exception) {
            println("贡献者数据未打包（本地构建属正常情况，可用 tools/gen_contributors.py 生成）: ${e.message}")
            return@withContext ContributionList(
                appDev = emptyList(),
                jiaowuadapter = emptyList()
            )
        }

        try {
            json.decodeFromString<ContributionList>(bytes.decodeToString())
        } catch (e: Exception) {
            throw Exception("无法解析贡献者数据文件", e)
        }
    }
}
