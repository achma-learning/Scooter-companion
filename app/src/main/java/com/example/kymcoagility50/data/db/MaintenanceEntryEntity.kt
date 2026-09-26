package com.example.kymcoagility50.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One maintenance-log entry, mirroring an "Intervention" slot in the KYMCO passport
 * (date + km + notes). This is the ONLY data that Sync Mode uploads — bookmarks, recent
 * pages, and settings always stay local regardless of mode.
 *
 * `dirty` marks a row that has local changes not yet pushed. `deleted` is a tombstone so a
 * deletion can be propagated to the server instead of just disappearing locally. `updatedAt`
 * is used for last-write-wins conflict resolution when merging with the server copy.
 */
@Entity(tableName = "maintenance_entries")
data class MaintenanceEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val km: Int,
    val date: Long,
    val note: String,
    val updatedAt: Long,
    val dirty: Boolean = true,
    val deleted: Boolean = false
)
