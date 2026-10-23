package io.github.achmalearning.scootercompanion.data.db

import androidx.room.Entity
import io.github.achmalearning.scootercompanion.data.model.OcrStatus
import io.github.achmalearning.scootercompanion.data.model.PdfSource

/**
 * One row per (pdf, page). `text` holds either the pre-generated OCR text shipped in
 * assets/manual_index.json / assets/passport_index.json, or text produced on-device by
 * ML Kit as a fallback, cached permanently so OCR never re-runs for the same page.
 */
@Entity(tableName = "ocr_pages", primaryKeys = ["pdf", "page"])
data class OcrPageEntity(
    val pdf: PdfSource,
    val page: Int,
    val text: String,
    val status: OcrStatus,
    val updatedAt: Long
)
