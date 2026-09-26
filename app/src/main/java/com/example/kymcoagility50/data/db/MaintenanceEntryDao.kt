package com.example.kymcoagility50.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MaintenanceEntryEntity): Long

    @Update
    suspend fun update(entry: MaintenanceEntryEntity)

    // Visible, non-deleted entries, newest km first.
    @Query("SELECT * FROM maintenance_entries WHERE deleted = 0 ORDER BY km DESC, date DESC")
    fun observeAll(): Flow<List<MaintenanceEntryEntity>>

    @Query("SELECT * FROM maintenance_entries WHERE id = :id")
    suspend fun get(id: Long): MaintenanceEntryEntity?

    @Query("SELECT * FROM maintenance_entries WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): MaintenanceEntryEntity?

    // Soft-delete: keep the row as a tombstone until it has been pushed (or immediately if
    // it was never synced), so a deletion made offline still propagates once sync resumes.
    @Query("UPDATE maintenance_entries SET deleted = 1, dirty = 1, updatedAt = :now WHERE id = :id")
    suspend fun markDeleted(id: Long, now: Long)

    @Query("DELETE FROM maintenance_entries WHERE id = :id AND remoteId IS NULL")
    suspend fun hardDeleteIfNeverSynced(id: Long)

    // --- Sync Mode only, called from the sync worker ---

    @Query("SELECT * FROM maintenance_entries WHERE dirty = 1")
    suspend fun getDirty(): List<MaintenanceEntryEntity>

    @Query("UPDATE maintenance_entries SET dirty = 0, remoteId = :remoteId WHERE id = :id")
    suspend fun markSynced(id: Long, remoteId: String)

    @Query("DELETE FROM maintenance_entries WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("DELETE FROM maintenance_entries")
    suspend fun clearAll()
}
