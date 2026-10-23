package com.example.kymcoagility50.data.fuel

import com.example.kymcoagility50.privacy.NetworkGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/** fuel-price.json as written by scripts/fuel/fetch_price.py. */
@Serializable
data class FuelPrice(
    val format: String = "",
    val pricePerLiter: Double,
    val currency: String = "MAD",
    val asOf: String,
    val source: String = ""
)

/**
 * "Today's price": Morocco's national average gasoline price, published weekly on the repo's
 * `data` branch by .github/workflows/fuel-price.yml. Same file and checks as web/js/fuelprice.js.
 */
object FuelPriceClient {
    const val PRICE_URL = "https://raw.githubusercontent.com/achma-learning/kymco-agility-moto/data/fuel-price.json"
    private val json = Json { ignoreUnknownKeys = true }

    /** Only from a tap on the button — see [NetworkGuard.userRequested]. */
    suspend fun fetch(): Result<FuelPrice> = NetworkGuard.userRequested {
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(PRICE_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    useCaches = false
                }
                try {
                    when (val code = conn.responseCode) {
                        200 -> parse(conn.inputStream.bufferedReader().use { it.readText() })
                        404 -> error("No price published yet")
                        else -> error("Price lookup failed ($code)")
                    }
                } finally {
                    conn.disconnect()
                }
            }
        }
    }

    /** Same plausibility band as the scraper: anything else means a broken file, not the market. */
    fun parse(text: String): FuelPrice {
        val p = json.decodeFromString(FuelPrice.serializer(), text)
        require(p.format == "agility-fuel-price" && p.pricePerLiter in 5.0..40.0) { "Unexpected price file" }
        return p
    }
}
