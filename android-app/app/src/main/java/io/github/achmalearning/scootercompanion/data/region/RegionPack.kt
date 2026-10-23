package io.github.achmalearning.scootercompanion.data.region

import android.content.Context
import io.github.achmalearning.scootercompanion.domain.EngineClass
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/** Text in several languages, keyed by language code; English is the fallback. */
typealias Localized = Map<String, String>

fun Localized.pick(locale: Locale = Locale.getDefault()): String =
    this[locale.language] ?: this["en"] ?: values.firstOrNull().orEmpty()

@Serializable
data class FineReason(val id: String, val en: String, val fr: String = en, val ar: String = en) {
    fun label(locale: Locale = Locale.getDefault()) = when (locale.language) { "fr" -> fr; "ar" -> ar; else -> en }
}

@Serializable
data class RegionInsurance(
    val mandatory: String = "",
    val durations: List<Int> = listOf(1, 3, 6, 12),
    /** Approximate yearly price by EngineClass name. Only a hint: the owner enters what they paid. */
    val yearlyReference: Map<String, Double> = emptyMap(),
    /** Roughly what a 12-month contract saves over shorter ones for the same year. */
    val yearlySaving: Double? = null,
    val note: Localized = emptyMap()
)

/** A website for buying or selling used scooters, opened in the browser. */
@Serializable
data class UsedMarket(val name: String, val url: String, val country: String = "")

@Serializable
data class RegionFines(val amounts: List<Double> = emptyList(), val reasons: List<FineReason> = emptyList())

/**
 * Everything country-specific, from shared/regions/<id>.json: currency, common brands,
 * fuel amount chips, insurance hints, fine presets. Add a country by adding a file.
 */
@Serializable
data class RegionPack(
    val id: String,
    val name: Localized = emptyMap(),
    val currency: String = "",
    val currencySymbol: String = "",
    val brands: List<String> = emptyList(),
    /** Where to look for a used scooter. */
    val usedMarkets: List<UsedMarket> = emptyList(),
    /** Fuel amount chips by EngineClass name. */
    val fuelChips: Map<String, List<Double>> = emptyMap(),
    val insurance: RegionInsurance = RegionInsurance(),
    val fines: RegionFines = RegionFines()
) {
    fun fuelChipsFor(engineClass: EngineClass): List<Double> =
        fuelChips[engineClass.name] ?: fuelChips.values.firstOrNull() ?: listOf(5.0, 10.0, 20.0, 50.0)

    fun insuranceReference(engineClass: EngineClass): Double? = insurance.yearlyReference[engineClass.name]

    companion object {
        /** Countries without a pack yet: no currency symbol, no presets. */
        val GENERIC = RegionPack(id = "XX", name = mapOf("en" to "Other country", "fr" to "Autre pays", "ar" to "بلد آخر"))
    }
}

/** Reads the bundled region packs (assets/regions/). */
class RegionRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, RegionPack>()

    val available: List<String> by lazy {
        context.assets.list("regions").orEmpty().filter { it.endsWith(".json") }.map { it.removeSuffix(".json") }.sorted()
    }

    fun pack(id: String): RegionPack = cache.getOrPut(id) {
        runCatching {
            json.decodeFromString(RegionPack.serializer(), context.assets.open("regions/$id.json").bufferedReader().use { it.readText() })
        }.getOrDefault(RegionPack.GENERIC)
    }
}
