package io.github.achmalearning.scootercompanion.data.content

import android.content.Context
import kotlinx.serialization.json.Json

/** Loads the bundled knowledge base once; it is static app data, never written. */
class ContentRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    val content: Content by lazy {
        val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        json.decodeFromString(Content.serializer(), text)
    }

    companion object {
        const val ASSET = "agility50.json"
    }
}
