package io.github.achmalearning.scootercompanion.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.achmalearning.scootercompanion.data.model.PdfSource
import kotlinx.coroutines.flow.Flow

@Dao
interface OcrPageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(page: OcrPageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(pages: List<OcrPageEntity>)

    @Query("SELECT * FROM ocr_pages WHERE pdf = :pdf AND page = :page")
    suspend fun get(pdf: PdfSource, page: Int): OcrPageEntity?

    @Query("SELECT * FROM ocr_pages WHERE pdf = :pdf ORDER BY page ASC")
    fun observeAll(pdf: PdfSource): Flow<List<OcrPageEntity>>

    @Query("SELECT COUNT(*) FROM ocr_pages WHERE pdf = :pdf AND status = 'DONE'")
    fun observeDoneCount(pdf: PdfSource): Flow<Int>

    @Query("SELECT COUNT(*) FROM ocr_pages WHERE pdf = :pdf")
    suspend fun count(pdf: PdfSource): Int

    // Offline full-text search (LIKE-based; dataset is small — 64 pages total across both PDFs).
    // Works for French/English text as-is; Arabic text is matched by substring too since it's
    // stored as normal Unicode text in SQLite.
    @Query("SELECT * FROM ocr_pages WHERE status = 'DONE' AND text LIKE '%' || :query || '%' AND (:pdf IS NULL OR pdf = :pdf) ORDER BY pdf ASC, page ASC")
    suspend fun search(query: String, pdf: PdfSource?): List<OcrPageEntity>

    @Query("DELETE FROM ocr_pages WHERE pdf = :pdf")
    suspend fun clear(pdf: PdfSource)

    @Query("DELETE FROM ocr_pages")
    suspend fun clearAll()
}
