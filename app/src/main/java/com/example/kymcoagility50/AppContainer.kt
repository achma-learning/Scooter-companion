package com.example.kymcoagility50

import android.content.Context
import com.example.kymcoagility50.data.db.ManualDatabase
import com.example.kymcoagility50.data.repository.LibraryRepository
import com.example.kymcoagility50.data.repository.MaintenanceRepository
import com.example.kymcoagility50.data.repository.OcrRepository
import com.example.kymcoagility50.data.repository.SettingsRepository
import com.example.kymcoagility50.data.repository.TocRepository

class AppContainer(context: Context) {
    private val db = ManualDatabase.getInstance(context)

    val ocrRepository = OcrRepository(context, db.ocrPageDao())
    val tocRepository = TocRepository(context)
    val libraryRepository = LibraryRepository(db.bookmarkDao(), db.recentPageDao())
    val maintenanceRepository = MaintenanceRepository(db.maintenanceEntryDao())
    val settingsRepository = SettingsRepository(context)

    companion object {
        @Volatile private var INSTANCE: AppContainer? = null
        fun getInstance(context: Context): AppContainer =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context.applicationContext).also { INSTANCE = it }
            }
    }
}
