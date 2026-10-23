package com.example.kymcoagility50.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.kymcoagility50.data.model.PdfSource

// Bookmarks are always local-only (per scope: only the maintenance logbook syncs).
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pdf: PdfSource,
    val page: Int,
    val title: String,
    val note: String?,
    val createdAt: Long
)
