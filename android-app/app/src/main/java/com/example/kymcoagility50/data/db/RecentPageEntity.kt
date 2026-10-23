package com.example.kymcoagility50.data.db

import androidx.room.Entity
import com.example.kymcoagility50.data.model.PdfSource

@Entity(tableName = "recent_pages", primaryKeys = ["pdf", "page"])
data class RecentPageEntity(
    val pdf: PdfSource,
    val page: Int,
    val lastViewedAt: Long
)
