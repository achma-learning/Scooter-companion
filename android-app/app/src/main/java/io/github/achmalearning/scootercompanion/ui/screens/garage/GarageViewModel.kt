package io.github.achmalearning.scootercompanion.ui.screens.garage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.data.content.Content
import io.github.achmalearning.scootercompanion.data.db.BikeProfileEntity
import io.github.achmalearning.scootercompanion.data.db.DocumentEntity
import io.github.achmalearning.scootercompanion.data.db.ExpenseEntity
import io.github.achmalearning.scootercompanion.data.db.InsurancePolicyEntity
import io.github.achmalearning.scootercompanion.data.db.FuelEntity
import io.github.achmalearning.scootercompanion.data.db.MaintenanceEntryEntity
import io.github.achmalearning.scootercompanion.data.repository.GarageSnapshot
import io.github.achmalearning.scootercompanion.domain.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One ViewModel for every "garage" screen: dashboard, service, passport, fuel, wallet, profile. */
class GarageViewModel(private val context: Context, private val container: AppContainer) : ViewModel() {

    val content: Content = container.contentRepository.content

    fun region(id: String) = container.regionRepository.pack(id)

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

    fun saveInsurance(policy: InsurancePolicyEntity) = viewModelScope.launch { garage.saveInsurance(policy) }
    fun deleteInsurance(policy: InsurancePolicyEntity) = viewModelScope.launch { garage.deleteInsurance(policy) }

    fun saveExpense(expense: ExpenseEntity) = viewModelScope.launch { garage.saveExpense(expense) }
    fun deleteExpense(expense: ExpenseEntity) = viewModelScope.launch { garage.deleteExpense(expense) }

    fun saveService(entry: MaintenanceEntryEntity) = viewModelScope.launch {
        log.save(entry)
    }

    fun deleteService(entry: MaintenanceEntryEntity) = viewModelScope.launch {
        log.deleteEntry(entry)
    }

    /** Quick "done today at the current km" for a single task (e.g. tyre pressure). */
    fun markTaskDone(taskId: String) = viewModelScope.launch {
        val km = state.value?.currentKm ?: 0
        log.save(
            MaintenanceEntryEntity(km = km, date = Dates.today(), note = "", tasks = taskId, updatedAt = System.currentTimeMillis())
        )
    }
}
