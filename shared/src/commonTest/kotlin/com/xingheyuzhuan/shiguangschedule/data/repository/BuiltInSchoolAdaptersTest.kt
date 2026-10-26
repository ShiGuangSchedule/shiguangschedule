package com.xingheyuzhuan.shiguangschedule.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import school_index.Adapter
import school_index.AdapterCategory
import school_index.School

class BuiltInSchoolAdaptersTest {
    @Test
    fun schoolIsAvailableWithoutDownloadedIndex() {
        val school = BuiltInSchoolAdapters.mergeSchools(emptyList()).single()

        assertEquals("华北电力大学（保定）", school.name)
        val adapter = school.adapters.single()
        assertEquals("华北电力大学（保定）教务系统", adapter.adapter_name)
        assertEquals(
            "https://ids.ncepu.edu.cn/?timestamp=1793009307516&service=https%3A%2F%2Fjwxt.ncepu.edu.cn%2FLogon.do%3Fmethod%3DlogonByHbdldx",
            adapter.import_url,
        )
        assertTrue(school.adapters.all { it.category == AdapterCategory.BACHELOR_AND_ASSOCIATE })
        assertTrue(
            school.adapters.all {
                "${school.resource_folder}/${it.asset_js_path}" ==
                    BuiltInSchoolAdapters.NCEPU_SCRIPT_PATH
            }
        )
    }

    @Test
    fun repositoryUpdatesPreserveOtherSchoolsAndExtraAdapters() {
        val remoteAdapter = Adapter(adapter_id = "REMOTE", adapter_name = "远程适配")
        val downloaded =
            listOf(
                School(id = "OTHER", name = "其他学校"),
                School(id = BuiltInSchoolAdapters.NCEPU_SCHOOL_ID, adapters = listOf(remoteAdapter)),
            )

        val merged = BuiltInSchoolAdapters.mergeSchools(downloaded)
        assertEquals(downloaded.first(), merged.first())
        assertEquals(2, merged.last().adapters.size)
        assertTrue(remoteAdapter in merged.last().adapters)
        assertEquals(merged, BuiltInSchoolAdapters.mergeSchools(merged))
    }

    @Test
    fun builtInScriptTakesPrecedenceOverAnOutdatedRemoteEntry() {
        val outdated =
            Adapter(
                adapter_id = "${BuiltInSchoolAdapters.NCEPU_SCHOOL_ID}_PORTAL",
                adapter_name = "旧版",
                asset_js_path = "outdated.js",
            )
        val merged =
            BuiltInSchoolAdapters.mergeSchools(
                    listOf(
                        School(
                            id = BuiltInSchoolAdapters.NCEPU_SCHOOL_ID,
                            adapters = listOf(outdated),
                        )
                    )
                )
                .single()

        assertEquals(1, merged.adapters.size)
        assertEquals("ncepu_baoding.js", merged.adapters.first().asset_js_path)
    }

    @Test
    fun retiredEntriesDoNotReappearFromDownloadedIndexes() {
        val oldAdapters =
            listOf("PORTAL", "WEBVPN", "CAMPUS").map { suffix ->
                Adapter(
                    adapter_id = "${BuiltInSchoolAdapters.NCEPU_SCHOOL_ID}_$suffix",
                    adapter_name = "旧入口 $suffix",
                    asset_js_path = "old.js",
                )
            }
        val school =
            BuiltInSchoolAdapters.mergeSchools(
                    listOf(
                        School(id = BuiltInSchoolAdapters.NCEPU_SCHOOL_ID, adapters = oldAdapters)
                    )
                )
                .single()

        assertEquals(
            listOf("${BuiltInSchoolAdapters.NCEPU_SCHOOL_ID}_PORTAL"),
            school.adapters.map { it.adapter_id },
        )
        assertEquals("华北电力大学（保定）教务系统", school.adapters.single().adapter_name)
        assertEquals(
            BuiltInSchoolAdapters.NCEPU_SCRIPT_PATH,
            BuiltInSchoolAdapters.scriptPath(school.adapters.single(), school.resource_folder),
        )
    }
}
