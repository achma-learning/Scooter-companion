package io.github.achmalearning.scootercompanion.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsTest {
    private fun day(iso: String) = Dates.fromIso(iso)!!
    private fun spend(iso: String, c: SpendCategory, amount: Double) = Spend(day(iso), c, amount)
    private val today = day("2026-10-20")

    private val spends = listOf(
        spend("2026-01-10", SpendCategory.PURCHASE, 15000.0),
        spend("2026-01-10", SpendCategory.INSURANCE, 1500.0),
        spend("2026-08-03", SpendCategory.FUEL, 30.0),
        spend("2026-09-05", SpendCategory.GEAR, 400.0),
        spend("2026-09-06", SpendCategory.ACCESSORY, 300.0),
        spend("2026-10-02", SpendCategory.FUEL, 30.0),
        spend("2026-10-02", SpendCategory.PARKING, 5.0),
        spend("2026-10-12", SpendCategory.FINE, 150.0)
    )

    @Test fun groupsFoldTwelveCategoriesIntoSix() {
        assertEquals(ChartGroup.ENERGY, ChartGroup.of(SpendCategory.CHARGING))
        assertEquals(ChartGroup.EQUIPMENT, ChartGroup.of(SpendCategory.GEAR))
        assertEquals(ChartGroup.OTHER, ChartGroup.of(SpendCategory.WASH))
    }

    @Test fun threeMonthRangeHasEveryMonthAndNoPurchase() {
        val d = Stats.build(spends, emptyList(), today, months = 3)
        assertEquals(listOf("2026-08", "2026-09", "2026-10"), d.months.map { Dates.monthIso(it.key) })
        assertEquals(700.0, d.months[1].byGroup[ChartGroup.EQUIPMENT]!!, 1e-9)
        assertEquals(185.0, d.months[2].total, 1e-9)
        assertEquals(915.0, d.rangeTotal, 1e-9)
        // Same-day spends make one cumulative point.
        assertEquals(listOf(30.0, 430.0, 730.0, 765.0, 915.0), d.cumulative.map { it.value })
    }

    @Test fun allTimeStartsAtTheFirstRunningCost() {
        val d = Stats.build(spends, emptyList(), today, months = null)
        assertEquals("2026-01", Dates.monthIso(d.months.first().key))
        assertEquals(10, d.months.size)
        assertEquals(SpendCategory.INSURANCE, d.byCategory.first().first)
    }

    @Test fun consumptionSegmentsBetweenFullTanks() {
        val fills = listOf(
            FuelFill(day("2026-10-01"), 1000, 4.0, 60.0, true),
            FuelFill(day("2026-10-05"), 1100, 1.0, 15.0, false),
            FuelFill(day("2026-10-10"), 1200, 3.0, 45.0, true)
        )
        val seg = FuelStats.segments(fills)
        assertEquals(1, seg.size)
        assertEquals(2.0, seg[0].value, 1e-9) // (1 + 3) L over 200 km
        assertEquals(day("2026-10-10"), seg[0].date)
    }

    @Test fun insightsPickTheStoryInTheNumbers() {
        val ledger = ExpenseLedger.stats(spends, today, ownedSince = day("2026-01-10"), monthlyTarget = 150.0, kmDriven = 2000)
        val list = Insights.build(ledger, spends, emptyList(), emptyList(), kmPerDay = null, today = today)
        val top = list.filterIsInstance<Insight.TopShare>().single()
        assertEquals(ChartGroup.INSURANCE, top.group)
        assertTrue(list.any { it is Insight.Fines && it.total == 150.0 })
        assertTrue(list.any { it is Insight.CostPerKm })
        // 185 this month vs 241.5 average: -23 %
        assertEquals(-23, list.filterIsInstance<Insight.VsAverage>().single().percent)
    }

    @Test fun fuelPriceTrendNeedsEnoughFills() {
        fun fill(iso: String, price: Double) = FuelFill(day(iso), 0, 2.0, 2.0 * price, false)
        val up = listOf(fill("2026-08-01", 13.0), fill("2026-08-15", 13.0), fill("2026-09-01", 13.0), fill("2026-09-15", 14.0), fill("2026-10-01", 14.0), fill("2026-10-15", 14.0))
        assertEquals(8, Insights.fuelTrend(up, today)!!.percent)
        assertNull(Insights.fuelTrend(up.take(3), today))
    }

    @Test fun kmPerDayFromRecentReadings() {
        val readings = listOf(day("2026-09-01") to 1000, day("2026-10-01") to 1600)
        assertEquals(20.0, Insights.kmPerDay(readings, today)!!, 1e-9)
        assertNull(Insights.kmPerDay(listOf(day("2026-10-15") to 1000, day("2026-10-18") to 1100), today))
    }
}
