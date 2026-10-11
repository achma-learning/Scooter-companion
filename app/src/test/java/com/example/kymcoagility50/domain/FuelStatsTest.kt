package com.example.kymcoagility50.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuelStatsTest {
    private val d = Dates.utcMidnight(2026, 0, 1)

    @Test fun fullToFullConsumption() {
        val s = FuelStats.summarize(
            listOf(
                FuelFill(d, 1000, 4.0, 52.0, true),
                FuelFill(d, 1100, 1.0, 13.0, false),
                FuelFill(d, 1200, 3.0, 39.0, true)
            )
        )
        // 4 L used over 200 km
        assertEquals(2.0, s.litersPer100!!, 1e-9)
        assertEquals(2.0, s.lastLitersPer100!!, 1e-9)
        assertEquals(52.0 / 200, s.costPerKm!!, 1e-9)
        assertEquals(200, s.distance)
    }

    @Test fun needsTwoFullFills() {
        val s = FuelStats.summarize(listOf(FuelFill(d, 1000, 4.0, null, true), FuelFill(d, 1100, 2.0, null, false)))
        assertNull(s.litersPer100)
        assertNull(s.costPerKm)
    }

    @Test fun topUpsBetweenFullTanksAndPricePerLiter() {
        val s = FuelStats.summarize(
            listOf(
                FuelFill(d, 1000, 3.0, 45.0, true),
                FuelFill(d, 1050, 2.0, 30.0, false),
                FuelFill(d, 1300, 2.0, 30.36, true)
            )
        )
        assertEquals(4.0 / 3, s.litersPer100!!, 1e-9) // 4 L over 300 km
        assertEquals(105.36 / 7, s.avgPricePerLiter!!, 1e-9)
        assertEquals(15.18, s.lastPricePerLiter!!, 1e-9)
    }

    @Test fun partialFillsOnlyStillGivePriceStats() {
        val s = FuelStats.summarize(listOf(FuelFill(d, 1000, 2.0, 30.0, false), FuelFill(d, 1100, 2.0, null, false)))
        assertNull(s.litersPer100)
        assertEquals(15.0, s.avgPricePerLiter!!, 1e-9)
        assertEquals(15.0, s.lastPricePerLiter!!, 1e-9)
    }
}
