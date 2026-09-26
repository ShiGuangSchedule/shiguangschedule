package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作组合作息及其调度规则。
 */
@Dao
interface TimeTableComboDao {

    // --- 全量备份与恢复方法 ---

    /**
     * 获取所有组合作息元数据（用于全量备份）
     */
    @Query("SELECT * FROM time_table_combos")
    suspend fun getAll(): List<TimeTableCombo>

    /**
     * 清空所有组合作息元数据（用于全量恢复）
     */
    @Query("DELETE FROM time_table_combos")
    suspend fun clearAll()

    /**
     * 获取所有组合作息调度规则（用于全量备份）
     */
    @Query("SELECT * FROM time_table_combo_rules")
    suspend fun getAllRules(): List<TimeTableComboRule>

    /**
     * 清空所有组合作息调度规则（用于全量恢复）
     */
    @Query("DELETE FROM time_table_combo_rules")
    suspend fun clearAllRules()

    // --- TimeTableCombo 基础操作 ---

    @Query("SELECT * FROM time_table_combos WHERE id = :id LIMIT 1")
    suspend fun getComboById(id: String): TimeTableCombo?

    /**
     * 响应式监听单个组合作息元数据（当基准作息 baseTimeTableId 变更时能实时推送）
     */
    @Query("SELECT * FROM time_table_combos WHERE id = :id LIMIT 1")
    fun getComboByIdFlow(id: String): Flow<TimeTableCombo?>

    @Query("SELECT * FROM time_table_combos ORDER BY createdAt DESC")
    fun getAllCombos(): Flow<List<TimeTableCombo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCombo(combo: TimeTableCombo)

    @Update
    suspend fun updateCombo(combo: TimeTableCombo)

    @Query("DELETE FROM time_table_combos WHERE id = :id")
    suspend fun deleteComboById(id: String)

    // --- TimeTableComboRule 规则操作 ---

    @Query("SELECT * FROM time_table_combo_rules WHERE comboId = :comboId")
    fun getRulesByComboId(comboId: String): Flow<List<TimeTableComboRule>>

    @Query("SELECT * FROM time_table_combo_rules WHERE comboId = :comboId")
    suspend fun getRuleListByComboId(comboId: String): List<TimeTableComboRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<TimeTableComboRule>)

    @Query("DELETE FROM time_table_combo_rules WHERE comboId = :comboId")
    suspend fun deleteRulesByComboId(comboId: String)

    /**
     * 事务操作：全量更新某个组合方案的内部规则（先清空再重新插入）
     */
    @Transaction
    suspend fun replaceComboRules(comboId: String, rules: List<TimeTableComboRule>) {
        deleteRulesByComboId(comboId)
        if (rules.isNotEmpty()) {
            insertRules(rules)
        }
    }

    // --- 清理/重置无效引用 ---

    /**
     * 根据基准作息 ID 查询所有关联的组合作息
     */
    @Query("SELECT * FROM time_table_combos WHERE baseTimeTableId = :baseTimeTableId")
    suspend fun getCombosByBaseTimeTableId(baseTimeTableId: String): List<TimeTableCombo>

    /**
     * 将引用了指定 baseTimeTableId 的组合作息全部将 baseTimeTableId 置为 NULL
     */
    @Query("UPDATE time_table_combos SET baseTimeTableId = NULL WHERE baseTimeTableId = :baseTimeTableId")
    suspend fun clearBaseTimeTableId(baseTimeTableId: String)

    /**
     * 清除规则表中引用了已被删除的作息方案（targetTimeTableId）的规则
     */
    @Query("DELETE FROM time_table_combo_rules WHERE targetTimeTableId = :targetTimeTableId")
    suspend fun deleteRulesByTargetTimeTableId(targetTimeTableId: String)
}