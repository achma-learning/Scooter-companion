package io.github.achmalearning.scootercompanion

import android.content.Context
import io.github.achmalearning.scootercompanion.data.content.ContentRepository
import io.github.achmalearning.scootercompanion.data.db.ManualDatabase
import io.github.achmalearning.scootercompanion.data.region.RegionRepository
import io.github.achmalearning.scootercompanion.data.repository.GarageRepository
import io.github.achmalearning.scootercompanion.data.repository.LibraryRepository
import io.github.achmalearning.scootercompanion.data.repository.MaintenanceRepository
import io.github.achmalearning.scootercompanion.data.repository.OcrRepository
import io.github.achmalearning.scootercompanion.data.repository.SettingsRepository
import io.github.achmalearning.scootercompanion.data.repository.TocRepository

class AppContainer(context: Context) {
    private val db = ManualDatabase.getInstance(context)

    val ocrRepository = OcrRepository(context, db.ocrPageDao())
    val tocRepository = TocRepository(context)
    val libraryRepository = LibraryRepository(db.bookmarkDao(), db.recentPageDao())
    val maintenanceRepository = MaintenanceRepository(db.maintenanceEntryDao())
    val settingsRepository = SettingsRepository(context)
    val contentRepository = ContentRepository(context)
    val regionRepository = RegionRepository(context)
    val garageRepository = GarageRepository(context, db) { contentRepository.content }

    companion object {
        @Volatile private var INSTANCE: AppContainer? = null
        fun getInstance(context: Context): AppContainer =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context.applicationContext).also { INSTANCE = it }
            }
    }
}
