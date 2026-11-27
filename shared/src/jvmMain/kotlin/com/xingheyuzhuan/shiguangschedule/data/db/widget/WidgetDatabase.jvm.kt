package com.xingheyuzhuan.shiguangschedule.data.db.widget

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import okio.Path

actual fun createWidgetDatabase(databasesDir: Path): WidgetDatabase {
    val dbPath = (databasesDir / "widget_database").toString()
    return Room.databaseBuilder<WidgetDatabase>(
        name = dbPath,
        factory = { WidgetDatabaseConstructor.initialize() }
    )
        .fallbackToDestructiveMigration(true)
        .setDriver(BundledSQLiteDriver())
        .build()
}
