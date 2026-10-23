package com.example.kymcoagility50.ui.screens.garage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kymcoagility50.AppContainer
import com.example.kymcoagility50.data.content.Content
import com.example.kymcoagility50.data.db.BikeProfileEntity
import com.example.kymcoagility50.data.db.DocumentEntity
import com.example.kymcoagility50.data.db.FuelEntity
import com.example.kymcoagility50.data.db.MaintenanceEntryEntity
import com.example.kymcoagility50.data.repository.GarageSnapshot
import com.example.kymcoagility50.domain.Dates
import com.example.kymcoagility50.work.SyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One ViewModel for every "garage" screen: dashboard, service, passport, fuel, wallet, profile. */
class GarageViewModel(private val context: Context, private val container: AppContainer) : ViewModel() {

    val content: Content = container.contentRepository.content

    val state: StateFlow<GarageSnapshot?> = container.garageRepository.snapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val garage get() = container.garageRepository
    private val log get() = container.maintenanceRepository

    fun updateKm(km: Int) = viewModelScope.launch { garage.addOdometer(km) }

    fun saveProfile(profile: BikeProfileEntity) = viewModelScope.launch { garage.saveProfile(profile) }

    fun saveFuel(entry: FuelEntity) = viewModelScope.launch { garage.saveFuel(entry) }
    fun deleteFuel(entry: FuelEntity) = viewModelScope.launch { garage.deleteFuel(entry) }

    fun saveDocument(doc: DocumentEntity) = viewModelScope.launch { garage.saveDocument(doc) }
    fun deleteDocument(doc: DocumentEntity) = viewModelScope.launch { garage.deleteDocument(doc) }

    fun markPreRideDone() = viewModelScope.launch { garage.markPreRideDone() }

    fun saveService(entry: MaintenanceEntryEntity) = viewModelScope.launch {
        log.save(entry)
        SyncScheduler.scheduleSyncNow(context)
    }

    fun deleteService(entry: MaintenanceEntryEntity) = viewModelScope.launch {
        log.deleteEntry(entry)
        SyncScheduler.scheduleSyncNow(context)
    }

    /** Quick "done today at the current km" for a single task (e.g. tyre pressure). */
    fun markTaskDone(taskId: String) = viewModelScope.launch {
        val km = state.value?.currentKm ?: 0
        log.save(
            MaintenanceEntryEntity(km = km, date = Dates.today(), note = "", tasks = taskId, updatedAt = System.currentTimeMillis())
        )
        SyncScheduler.scheduleSyncNow(context)
    }
}
