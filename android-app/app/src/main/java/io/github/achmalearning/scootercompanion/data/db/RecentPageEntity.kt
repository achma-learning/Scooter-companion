package io.github.achmalearning.scootercompanion.data.db

import androidx.room.Entity
import io.github.achmalearning.scootercompanion.data.model.PdfSource

@Entity(tableName = "recent_pages", primaryKeys = ["pdf", "page"])
data class RecentPageEntity(
    val pdf: PdfSource,
    val page: Int,
    val lastViewedAt: Long
)
