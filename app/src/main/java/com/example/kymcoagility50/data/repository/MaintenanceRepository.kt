package com.example.kymcoagility50.data.repository

import com.example.kymcoagility50.data.db.MaintenanceEntryDao
import com.example.kymcoagility50.data.db.MaintenanceEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * The maintenance logbook (date/km/notes) is the ONLY data this app ever syncs. Reads and
 * writes always go to Room first — the sync worker (see MaintenanceSyncWorker) is the only
 * thing that talks to the network, and only when Sync Mode is on.
 */
class MaintenanceRepository(private val dao: MaintenanceEntryDao) {

    fun observeAll(): Flow<List<MaintenanceEntryEntity>> = dao.observeAll()

    suspend fun addEntry(km: Int, date: Long, note: String) {
        dao.upsert(
            MaintenanceEntryEntity(
                km = km,
                date = date,
                note = note,
                updatedAt = System.currentTimeMillis(),
                dirty = true
            )
        )
    }

    suspend fun updateEntry(entry: MaintenanceEntryEntity, km: Int, date: Long, note: String) {
        dao.update(
            entry.copy(km = km, date = date, note = note, updatedAt = System.currentTimeMillis(), dirty = true)
        )
    }

    suspend fun deleteEntry(entry: MaintenanceEntryEntity) {
        if (entry.remoteId == null) {
            // Never synced — safe to remove outright, nothing to reconcile on the server.
            dao.hardDeleteIfNeverSynced(entry.id)
        } else {
            dao.markDeleted(entry.id, System.currentTimeMillis())
        }
    }

    suspend fun clearAllLocal() = dao.clearAll()
}
