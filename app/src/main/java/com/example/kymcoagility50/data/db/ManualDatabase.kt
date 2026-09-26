package com.example.kymcoagility50.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        OcrPageEntity::class,
        BookmarkEntity::class,
        RecentPageEntity::class,
        MaintenanceEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ManualDatabase : RoomDatabase() {

    abstract fun ocrPageDao(): OcrPageDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun recentPageDao(): RecentPageDao
    abstract fun maintenanceEntryDao(): MaintenanceEntryDao

    companion object {
        @Volatile private var INSTANCE: ManualDatabase? = null

        fun getInstance(context: Context): ManualDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ManualDatabase::class.java,
                    "kymco_agility50.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
