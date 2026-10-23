package io.github.achmalearning.scootercompanion.data.db

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

    @Query("DELETE FROM maintenance_entries WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("DELETE FROM maintenance_entries")
    suspend fun clearAll()
}
