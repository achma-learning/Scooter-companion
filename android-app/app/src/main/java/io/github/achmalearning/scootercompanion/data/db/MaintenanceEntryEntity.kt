package io.github.achmalearning.scootercompanion.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One maintenance-log entry, mirroring an "Intervention" slot in the KYMCO passport
 * (date + km + notes), plus which planner tasks it covered, the passport milestone it
 * answers, where it was done, its cost, and whether a dealer stamped the paper passport.
 *
 * `remoteId`, `dirty` and `deleted` are left over from the removed cloud sync. They stay
 * in the schema so existing databases migrate cleanly; rows are now always deleted outright.
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
    /** Total paid. When parts or labour are filled in, it is their sum. */
    val cost: Double? = null,
    val partsCost: Double? = null,
    val laborCost: Double? = null,
    /** What was fitted or used, e.g. "Ipone R4000 10W-40" or "Michelin City Grip 2". */
    @ColumnInfo(defaultValue = "") val product: String = "",
    /** Name of a custom entry that isn't a planner task, e.g. "New mirror". */
    @ColumnInfo(defaultValue = "") val title: String = "",
    @ColumnInfo(defaultValue = "0") val stamped: Boolean = false,
    val updatedAt: Long,
    val dirty: Boolean = true,
    val deleted: Boolean = false
)

val MaintenanceEntryEntity.taskIds: Set<String>
    get() = tasks.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
