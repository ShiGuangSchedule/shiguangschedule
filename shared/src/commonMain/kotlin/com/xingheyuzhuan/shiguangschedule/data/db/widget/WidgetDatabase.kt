package com.xingheyuzhuan.shiguangschedule.data.db.widget

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import okio.Path

@Database(
    entities = [WidgetCourse::class, WidgetAppSettings::class],
    version = 3,
    exportSchema = false
)
@ConstructedBy(WidgetDatabaseConstructor::class)
abstract class WidgetDatabase : RoomDatabase() {

    abstract fun widgetCourseDao(): WidgetCourseDao
    abstract fun widgetAppSettingsDao(): WidgetAppSettingsDao

    companion object {
        fun getDatabase(databasesDir: Path): WidgetDatabase {
            return createWidgetDatabase(databasesDir)
        }
    }
}

expect fun createWidgetDatabase(databasesDir: Path): WidgetDatabase

expect object WidgetDatabaseConstructor : RoomDatabaseConstructor<WidgetDatabase> {
    override fun initialize(): WidgetDatabase
}
