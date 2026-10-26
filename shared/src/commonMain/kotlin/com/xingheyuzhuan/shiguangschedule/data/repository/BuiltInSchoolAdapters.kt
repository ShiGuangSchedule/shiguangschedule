package com.xingheyuzhuan.shiguangschedule.data.repository

import org.jetbrains.compose.resources.ExperimentalResourceApi
import school_index.Adapter
import school_index.AdapterCategory
import school_index.School
import shiguangschedule.shared.generated.resources.Res

/** Adapters shipped with this fork, independent of the downloaded school repository. */
internal object BuiltInSchoolAdapters {
    const val NCEPU_SCHOOL_ID = "NCEPU_BAODING"
    const val NCEPU_SCRIPT_PATH = "builtin/ncepu_baoding.js"

    private val retiredAdapterIds = setOf("${NCEPU_SCHOOL_ID}_WEBVPN", "${NCEPU_SCHOOL_ID}_CAMPUS")

    private val ncepuSchool =
        School(
            id = NCEPU_SCHOOL_ID,
            name = "华北电力大学（保定）",
            initial = "HBDLDXBD",
            resource_folder = "builtin",
            adapters =
                listOf(
                    Adapter(
                        adapter_id = "${NCEPU_SCHOOL_ID}_PORTAL",
                        adapter_name = "华北电力大学（保定）教务系统",
                        category = AdapterCategory.BACHELOR_AND_ASSOCIATE,
                        asset_js_path = "ncepu_baoding.js",
                        import_url =
                            "https://ids.ncepu.edu.cn/?timestamp=1793009307516&service=https%3A%2F%2Fjwxt.ncepu.edu.cn%2FLogon.do%3Fmethod%3DlogonByHbdldx",
                        description = "适配华北电力大学（保定）教务系统，需校园网或WebVPN访问。登陆后切换到课表页面，点击导入即可。",
                        maintainer = "Wisabel2025",
                    )
                ),
        )

    fun mergeSchools(downloaded: List<School>): List<School> {
        val existing = downloaded.find { it.id == NCEPU_SCHOOL_ID }
        // Preserve any additional remote adapters under the same school ID.
        // Downloaded indexes must not restore the retired WebVPN and campus choices.
        val replacedIds = ncepuSchool.adapters.map { it.adapter_id }.toSet() + retiredAdapterIds
        val merged =
            ncepuSchool.copy(
                resource_folder = existing?.resource_folder ?: ncepuSchool.resource_folder,
                adapters =
                    ncepuSchool.adapters +
                        existing?.adapters.orEmpty().filterNot { it.adapter_id in replacedIds },
            )
        return downloaded.filterNot { it.id == NCEPU_SCHOOL_ID } + merged
    }

    fun scriptPath(adapter: Adapter, resourceFolder: String): String {
        if (ncepuSchool.adapters.any { it.adapter_id == adapter.adapter_id }) {
            return NCEPU_SCRIPT_PATH
        }
        val fileName = adapter.asset_js_path.ifBlank { "${adapter.adapter_id}.js" }
        return "$resourceFolder/$fileName"
    }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun readScript(assetJsPath: String): String? =
        when (assetJsPath) {
            NCEPU_SCRIPT_PATH -> Res.readBytes("files/adapters/ncepu_baoding.js").decodeToString()
            else -> null
        }
}
