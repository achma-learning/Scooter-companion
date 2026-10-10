package com.example.kymcoagility50.data.repository

import com.example.kymcoagility50.data.db.MaintenanceEntryDao
import com.example.kymcoagility50.data.db.MaintenanceEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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

    /** Insert (id = 0) or update a full service record — tasks, milestone, shop, cost, stamp. */
    suspend fun save(entry: MaintenanceEntryEntity) {
        dao.upsert(entry.copy(updatedAt = System.currentTimeMillis(), dirty = true))
    }

    /**
     * Backup import: every current entry goes through [deleteEntry] (so synced ones leave a
     * tombstone the next sync propagates), then the imported entries are added as new.
     */
    suspend fun replaceAll(entries: List<MaintenanceEntryEntity>) {
        dao.observeAll().first().forEach { deleteEntry(it) }
        entries.forEach { dao.upsert(it.copy(id = 0, remoteId = null, dirty = true, deleted = false)) }
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
