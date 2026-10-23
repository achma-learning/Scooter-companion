package io.github.achmalearning.scootercompanion.data

import io.github.achmalearning.scootercompanion.data.fuel.FuelPriceClient
import org.junit.Assert.assertEquals
import org.junit.Test

class FuelPriceClientTest {
    private val published = """
        {"format": "agility-fuel-price", "version": 1, "country": "MA", "fuel": "gasoline-95", "currency": "MAD",
         "pricePerLiter": 15.18, "asOf": "2026-10-05", "source": "GlobalPetrolPrices.com", "license": "CC BY-NC-ND 3.0"}
    """.trimIndent()

    @Test fun readsThePublishedFile() {
        val p = FuelPriceClient.parse(published)
        assertEquals(15.18, p.pricePerLiter, 1e-9)
        assertEquals("2026-10-05", p.asOf)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnImplausiblePrice() {
        FuelPriceClient.parse(published.replace("15.18", "1518"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSomeOtherFile() {
        FuelPriceClient.parse(published.replace("agility-fuel-price", "something-else"))
    }
}
