package io.github.achmalearning.scootercompanion.data.db

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
        MaintenanceEntryEntity::class,
        BikeProfileEntity::class,
        OdometerEntity::class,
        FuelEntity::class,
        DocumentEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ManualDatabase : RoomDatabase() {

    abstract fun ocrPageDao(): OcrPageDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun recentPageDao(): RecentPageDao
    abstract fun maintenanceEntryDao(): MaintenanceEntryDao
    abstract fun garageDao(): GarageDao

    companion object {
        @Volatile private var INSTANCE: ManualDatabase? = null

        fun getInstance(context: Context): ManualDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ManualDatabase::class.java,
                    "scooter_companion.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
        }
    }
}
