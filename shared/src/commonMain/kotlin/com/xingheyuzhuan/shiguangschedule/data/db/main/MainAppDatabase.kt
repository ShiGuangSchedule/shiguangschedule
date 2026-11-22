package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.DeleteTable
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.AutoMigrationSpec
import com.xingheyuzhuan.shiguangschedule.data.di.AppStorage

@Database(
    entities = [
        CourseTable::class,
        Course::class,
        CourseWeek::class,
        TimeSlot::class,
        CourseTableConfig::class,
        TimeTable::class,
        CourseTimeBinding::class,
        TimeTableCombo::class,
        TimeTableComboRule::class
    ],
    version = 8,
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5, spec = MainAppDatabase.RemoveAppSettingsSpec::class),
        AutoMigration(from = 6, to = 7),
        // 7 → 8：courses 表新增 isExam 列（考试日程标记），带默认值，自动迁移即可
        AutoMigration(from = 7, to = 8)
    ],
    exportSchema = true
)
@ConstructedBy(MainAppDatabaseConstructor::class)
abstract class MainAppDatabase : RoomDatabase() {

    @DeleteTable(tableName = "app_settings")
    class RemoveAppSettingsSpec : AutoMigrationSpec

    abstract fun courseTableDao(): CourseTableDao
    abstract fun courseDao(): CourseDao
    abstract fun courseWeekDao(): CourseWeekDao
    abstract fun timeSlotDao(): TimeSlotDao
    abstract fun courseTableConfigDao(): CourseTableConfigDao
    abstract fun timeTableDao(): TimeTableDao
    abstract fun courseTimeBindingDao(): CourseTimeBindingDao
    abstract fun timeTableComboDao(): TimeTableComboDao

    companion object {
        fun getDatabase(appStorage: AppStorage): MainAppDatabase {
            return createMainDatabase(appStorage)
        }
    }
}

expect fun createMainDatabase(appStorage: AppStorage): MainAppDatabase

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MainAppDatabaseConstructor : RoomDatabaseConstructor<MainAppDatabase>