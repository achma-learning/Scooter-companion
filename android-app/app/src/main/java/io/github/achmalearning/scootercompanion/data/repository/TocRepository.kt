package io.github.achmalearning.scootercompanion.data.repository

import android.content.Context
import io.github.achmalearning.scootercompanion.data.model.AGILITY_DOCS
import io.github.achmalearning.scootercompanion.data.model.TocDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class TocRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cached: TocDocument? = null

    suspend fun load(): TocDocument = withContext(Dispatchers.IO) {
        cached ?: run {
            val text = context.assets.open("$AGILITY_DOCS/toc.json").bufferedReader().use { it.readText() }
            json.decodeFromString(TocDocument.serializer(), text).also { cached = it }
        }
    }
}
