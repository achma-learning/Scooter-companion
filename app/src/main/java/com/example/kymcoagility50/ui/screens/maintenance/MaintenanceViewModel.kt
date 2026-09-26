package com.example.kymcoagility50.ui.screens.maintenance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kymcoagility50.AppContainer
import com.example.kymcoagility50.data.db.MaintenanceEntryEntity
import com.example.kymcoagility50.privacy.AppMode
import com.example.kymcoagility50.privacy.PrivacyManager
import com.example.kymcoagility50.work.SyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MaintenanceUiState(
    val entries: List<MaintenanceEntryEntity> = emptyList(),
    val mode: AppMode = AppMode.PRIVATE
)

class MaintenanceViewModel(
    private val context: Context,
    private val container: AppContainer
) : ViewModel() {

    val uiState: StateFlow<MaintenanceUiState> = combine(
        container.maintenanceRepository.observeAll(),
        PrivacyManager.mode
    ) { entries, mode -> MaintenanceUiState(entries, mode) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MaintenanceUiState())

    fun addEntry(km: Int, date: Long, note: String) {
        viewModelScope.launch {
            container.maintenanceRepository.addEntry(km, date, note)
            SyncScheduler.scheduleSyncNow(context)
        }
    }

    fun updateEntry(entry: MaintenanceEntryEntity, km: Int, date: Long, note: String) {
        viewModelScope.launch {
            container.maintenanceRepository.updateEntry(entry, km, date, note)
            SyncScheduler.scheduleSyncNow(context)
        }
    }

    fun deleteEntry(entry: MaintenanceEntryEntity) {
        viewModelScope.launch {
            container.maintenanceRepository.deleteEntry(entry)
            SyncScheduler.scheduleSyncNow(context)
        }
    }

    fun syncNow() = SyncScheduler.scheduleSyncNow(context)
}
