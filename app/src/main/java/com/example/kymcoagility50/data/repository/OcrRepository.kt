package com.example.kymcoagility50.data.repository

import android.content.Context
import com.example.kymcoagility50.data.db.OcrPageDao
import com.example.kymcoagility50.data.db.OcrPageEntity
import com.example.kymcoagility50.data.model.ManualIndex
import com.example.kymcoagility50.data.model.OcrStatus
import com.example.kymcoagility50.data.model.PdfSource
import com.example.kymcoagility50.pdf.PdfPageRenderer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * On first run for each PDF, seeds Room from the pre-generated assets/*_index.json (real OCR
 * output produced when this project was built — see README). If a page is ever missing from
 * that seed (e.g. a future replacement PDF with different page count), [ocrMissingPages] runs
 * ML Kit Text Recognition entirely on-device to fill the gap; nothing here ever touches the
 * network in either mode, so it runs identically in Private Mode and Sync Mode.
 */
class OcrRepository(
    private val context: Context,
    private val dao: OcrPageDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun ensureSeeded(source: PdfSource) = withContext(Dispatchers.IO) {
        if (dao.count(source) > 0) return@withContext
        val text = context.assets.open(source.indexAssetName).bufferedReader().use { it.readText() }
        val index = json.decodeFromString(ManualIndex.serializer(), text)
        val now = System.currentTimeMillis()
        dao.upsertAll(
            index.pages.map {
                OcrPageEntity(pdf = source, page = it.page, text = it.text, status = OcrStatus.DONE, updatedAt = now)
            }
        )
    }

    fun observeDoneCount(source: PdfSource): Flow<Int> = dao.observeDoneCount(source)

    suspend fun pageCountKnown(source: PdfSource): Int = dao.count(source)

    /** Runs ML Kit on-device OCR for any page in [1..totalPages] not already cached. */
    suspend fun ocrMissingPages(source: PdfSource, totalPages: Int, renderer: PdfPageRenderer) =
        withContext(Dispatchers.Default) {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            for (page in 1..totalPages) {
                if (dao.get(source, page) != null) continue
                val bitmap = renderer.renderPage(page, targetWidthPx = 2000)
                val text = runCatching {
                    recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
                }.getOrDefault("")
                dao.upsert(
                    OcrPageEntity(
                        pdf = source,
                        page = page,
                        text = text,
                        status = if (text.isNotBlank()) OcrStatus.DONE else OcrStatus.FAILED,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }

    data class SearchHit(val pdf: PdfSource, val page: Int, val snippet: String)

    suspend fun search(query: String, filter: PdfSource?): List<SearchHit> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        dao.search(query.trim(), filter).map { entity ->
            SearchHit(entity.pdf, entity.page, snippet(entity.text, query.trim()))
        }
    }

    private fun snippet(text: String, query: String): String {
        val idx = text.indexOf(query, ignoreCase = true)
        if (idx < 0) return text.take(120)
        val start = (idx - 40).coerceAtLeast(0)
        val end = (idx + query.length + 60).coerceAtMost(text.length)
        return (if (start > 0) "…" else "") + text.substring(start, end).replace('\n', ' ') + (if (end < text.length) "…" else "")
    }

    suspend fun clear(source: PdfSource) = dao.clear(source)
}
