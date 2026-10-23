package io.github.achmalearning.scootercompanion.data.repository

import android.content.Context
import io.github.achmalearning.scootercompanion.data.db.OcrPageDao
import io.github.achmalearning.scootercompanion.data.db.OcrPageEntity
import io.github.achmalearning.scootercompanion.data.model.ManualIndex
import io.github.achmalearning.scootercompanion.data.model.OcrStatus
import io.github.achmalearning.scootercompanion.data.model.PdfSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * On first run for each PDF, seeds Room from the pre-generated assets/<pdf>_index.json (real OCR
 * output produced when this project was built — see README). Search runs on that text only;
 * nothing here touches the network.
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
