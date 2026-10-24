package io.github.achmalearning.scootercompanion.data.models

import android.content.Context
import io.github.achmalearning.scootercompanion.data.region.Localized
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

/** scooter-data/<brand>/<model>/model.json; the format is described in scooter-data/README.md. */
@Serializable
data class ScooterModel(
    val schema: Int = 1,
    /** "<brand>/<model>", the folder path. */
    val id: String,
    val brand: String,
    val model: String,
    val years: String = "",
    val code: String = "",
    val aliases: List<String> = emptyList(),
    val engine: ModelEngine,
    /** False: figures from spec databases (the maker no longer lists the model). */
    val verified: Boolean = true,
    /** Numbers the comparison screen lines up. */
    val figures: ModelFigures = ModelFigures(),
    /** "agility50": the detailed Agility schedule, dealer passport and manual reader. */
    val plan: String = "",
    val specs: List<ModelSpec> = emptyList(),
    /** Manufacturer intervals by maintenance task id. */
    val schedule: Map<String, ModelInterval> = emptyMap(),
    val docs: List<ModelDoc> = emptyList(),
    val note: Localized = emptyMap(),
    val sources: List<ModelSource> = emptyList()
) {
    val name: String get() = "$brand $model"
    fun spec(key: String): String? = specs.firstOrNull { it.key == key }?.value
}

@Serializable
data class ModelEngine(
    val cc: Int,
    val powertrain: String = "PETROL",
    val fuelSystem: String = "CARBURETOR",
    val cooling: String = "AIR"
)

@Serializable
data class ModelFigures(
    val powerKw: Double? = null,
    val powerRpm: Int? = null,
    val torqueNm: Double? = null,
    val torqueRpm: Int? = null,
    val weightKg: Double? = null,
    /** "dry", "kerb" or "wet", as the maker states it: they don't compare like for like. */
    val weightKind: String = "",
    val seatMm: Int? = null,
    val tankL: Double? = null,
    val wheelbaseMm: Int? = null,
    val consumptionL100: Double? = null,
    val topSpeedKmh: Int? = null
) {
    /** Watts per kilogram: how briskly it pulls away. */
    val powerToWeight: Double? get() = if (powerKw != null && weightKg != null && weightKg > 0) powerKw * 1000 / weightKg else null

    /** Kilometres on a full tank at the stated consumption. */
    val rangeKm: Double? get() = if (tankL != null && consumptionL100 != null && consumptionL100 > 0) tankL / consumptionL100 * 100 else null
}

@Serializable data class ModelSpec(val key: String, val value: String)

@Serializable
data class ModelInterval(val km: Int? = null, val months: Int? = null, val breakInKm: List<Int>? = null)

/** A PDF downloaded on demand: the manufacturer's [url], or a [file] in the model's folder. */
@Serializable
data class ModelDoc(
    val id: String,
    val title: Localized,
    val url: String = "",
    val file: String = "",
    val bytes: Long = 0,
    val sha256: String = "",
    val lang: String = ""
)

@Serializable data class ModelSource(val title: String, val url: String = "")

/** Every model bundled from scooter-data (assets "scooter-data/<brand>/<model>/model.json"). */
class ModelCatalog(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    val models: List<ScooterModel> by lazy {
        modelFiles(ROOT, depth = 0).mapNotNull { path ->
            runCatching {
                context.assets.open(path).bufferedReader().use { json.decodeFromString(ScooterModel.serializer(), it.readText()) }
            }.getOrNull()
        }.sortedWith(compareBy({ it.brand.lowercase() }, { it.model.lowercase() }))
    }

    /**
     * Every <brand>/<model>/model.json under [path]. Android lists a folder's direct children;
     * Robolectric (the screen tests) lists whole relative paths: both are handled.
     */
    private fun modelFiles(path: String, depth: Int): List<String> = context.assets.list(path).orEmpty().flatMap { child ->
        when {
            child.endsWith("model.json") -> listOf("$path/$child")
            depth < 2 && '.' !in child -> modelFiles("$path/$child", depth + 1)
            else -> emptyList()
        }
    }

    fun byId(id: String): ScooterModel? = models.firstOrNull { it.id == id }

    fun forBrand(brand: String): List<ScooterModel> = models.filter { it.brand.equals(brand.trim(), ignoreCase = true) }

    /** The model the owner typed, if the catalogue knows it. */
    fun match(brand: String, model: String, cc: Int): ScooterModel? = match(models, brand, model, cc)

    companion object {
        const val ROOT = "scooter-data"
        const val AGILITY_50 = "kymco/agility-50-4t"

        /**
         * Same brand, and the typed model reads as the model's name or one of its aliases
         * ("sh 125i" matches "SH125i"). When several match, the longest name wins ("Symphony ST
         * 125 LC" over "Symphony ST"); an engine size that is clearly different rules a model out.
         */
        fun match(models: List<ScooterModel>, brand: String, model: String, cc: Int): ScooterModel? {
            val typed = normalize(model)
            if (typed.isEmpty()) return null
            return models.asSequence()
                .filter { it.brand.equals(brand.trim(), ignoreCase = true) }
                .filter { cc <= 0 || it.engine.cc <= 0 || abs(cc - it.engine.cc) <= it.engine.cc / 5 }
                .mapNotNull { m ->
                    (listOf(m.model) + m.aliases).map(::normalize)
                        .filter { name -> typed == name || typed.startsWith(name) }
                        .maxOfOrNull { it.length }
                        ?.let { m to it }
                }
                .maxByOrNull { it.second }?.first
        }

        private fun normalize(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
    }
}
