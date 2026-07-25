package com.parrotworks.redreamer.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Dream::class, Tag::class, DreamTagCrossRef::class, DreamFts::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dreamDao(): DreamDao
    abstract fun tagDao(): TagDao
    abstract fun dreamFtsDao(): DreamFtsDao

    companion object {
        const val DATABASE_NAME = "redreamer.db"
    }
}
