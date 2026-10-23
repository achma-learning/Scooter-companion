package io.github.achmalearning.scootercompanion.data.repository

import io.github.achmalearning.scootercompanion.data.db.MaintenanceEntryDao
import io.github.achmalearning.scootercompanion.data.db.MaintenanceEntryEntity
import kotlinx.coroutines.flow.Flow

/** The maintenance logbook: every service the owner logged, stored only in Room. */
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

    /** Backup import / wipe: the imported entries replace the whole logbook. */
    suspend fun replaceAll(entries: List<MaintenanceEntryEntity>) {
        dao.clearAll()
        entries.forEach { dao.upsert(it.copy(id = 0, remoteId = null, dirty = false, deleted = false)) }
    }

    suspend fun updateEntry(entry: MaintenanceEntryEntity, km: Int, date: Long, note: String) {
        dao.update(
            entry.copy(km = km, date = date, note = note, updatedAt = System.currentTimeMillis(), dirty = true)
        )
    }

    suspend fun deleteEntry(entry: MaintenanceEntryEntity) = dao.hardDelete(entry.id)
}
