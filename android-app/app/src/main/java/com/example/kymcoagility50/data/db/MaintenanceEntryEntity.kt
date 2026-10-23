package com.example.kymcoagility50.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One maintenance-log entry, mirroring an "Intervention" slot in the KYMCO passport
 * (date + km + notes), plus which planner tasks it covered, the passport milestone it
 * answers, where it was done, its cost, and whether a dealer stamped the paper passport.
 * This is the ONLY data that Sync Mode uploads — bookmarks, recent pages, settings and the
 * rest of the garage (profile, fuel, documents) always stay local regardless of mode.
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
    /** Comma-separated task ids from the knowledge base, e.g. "engine-oil,gear-oil". */
    @ColumnInfo(defaultValue = "") val tasks: String = "",
    val milestoneKm: Int? = null,
    @ColumnInfo(defaultValue = "") val shop: String = "",
    val cost: Double? = null,
    @ColumnInfo(defaultValue = "0") val stamped: Boolean = false,
    val updatedAt: Long,
    val dirty: Boolean = true,
    val deleted: Boolean = false
)

val MaintenanceEntryEntity.taskIds: Set<String>
    get() = tasks.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
