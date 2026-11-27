package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Room
import androidx.sqlite.driver.NativeSQLiteDriver
import okio.Path

actual fun createMainDatabase(databasesDir: Path): MainAppDatabase {
    val dbPath = (databasesDir / "main_app_database").toString()
    return Room.databaseBuilder<MainAppDatabase>(
        name = dbPath,
        factory = { MainAppDatabaseConstructor.initialize() }
    )
        .addMigrations(*ALL_MIGRATIONS)
        .setDriver(NativeSQLiteDriver())
        .build()
}
