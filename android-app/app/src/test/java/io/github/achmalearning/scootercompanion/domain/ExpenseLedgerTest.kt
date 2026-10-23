package io.github.achmalearning.scootercompanion.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseLedgerTest {
    private fun day(iso: String) = Dates.fromIso(iso)!!
    private fun spend(iso: String, c: SpendCategory, amount: Double) = Spend(day(iso), c, amount)

    private val today = day("2026-10-10")
    private val spends = listOf(
        spend("2026-08-01", SpendCategory.PURCHASE, 15000.0),
        spend("2026-08-01", SpendCategory.INSURANCE, 1500.0),
        spend("2026-08-05", SpendCategory.GEAR, 400.0),
        spend("2026-09-03", SpendCategory.FUEL, 30.0),
        spend("2026-09-20", SpendCategory.MAINTENANCE, 100.0),
        spend("2026-10-02", SpendCategory.FUEL, 30.0),
        spend("2026-10-06", SpendCategory.FUEL, 20.0),
        spend("2026-10-08", SpendCategory.FINE, 150.0),
        // Logged ahead of time: not spent yet, so not counted.
        spend("2026-11-01", SpendCategory.INSURANCE, 2000.0)
    )

    @Test fun totalsSeparateThePurchaseFromRunningCosts() {
        val s = ExpenseLedger.stats(spends, today, ownedSince = day("2026-08-01"), kmDriven = 1000)
        assertEquals(2230.0, s.running, 1e-9)
        assertEquals(15000.0, s.purchase, 1e-9)
        assertEquals(17230.0, s.total, 1e-9)
        assertEquals(3, s.monthsOwned)
        assertEquals(2230.0 / 3, s.avgPerMonth, 1e-9)
        assertEquals(17230.0 / 3, s.avgPerMonthWithPurchase, 1e-9)
        assertEquals(2.23, s.costPerKm!!, 1e-9)
        assertEquals(17.23, s.totalCostPerKm!!, 1e-9)
    }

    @Test fun monthsCoverEveryMonthFromTheFirstSpend() {
        val s = ExpenseLedger.stats(spends, today, ownedSince = day("2026-08-01"))
        assertEquals(listOf("2026-08", "2026-09", "2026-10"), s.months.map { Dates.monthIso(it.key) })
        assertEquals(1900.0, s.months[0].total, 1e-9)
        assertEquals(200.0, s.thisMonth, 1e-9)
        assertEquals(130.0, s.lastMonth, 1e-9)
        assertEquals(2230.0, s.thisYear, 1e-9)
        assertEquals(SpendCategory.INSURANCE, s.byCategory.first().first)
    }

    @Test fun projectionOnlyExtrapolatesRecurringCosts() {
        // 10 Oct: 50 of fuel in 10 days → 5/day for the 21 days left; the 150 fine stays once.
        val s = ExpenseLedger.stats(spends, today, ownedSince = day("2026-08-01"), monthlyTarget = 250.0)
        assertEquals(200.0 + 5.0 * 21, s.projectedThisMonth, 1e-9)
        val b = s.budget!!
        assertEquals(50.0, b.remaining, 1e-9)
        assertFalse(b.over)
        assertTrue(b.projectedOver)
    }

    @Test fun noTargetMeansNoBudget() {
        assertNull(ExpenseLedger.stats(spends, today, null).budget)
        assertNull(ExpenseLedger.stats(spends, today, null, monthlyTarget = 0.0).budget)
    }

    @Test fun anEmptyLedgerIsOneQuietMonth() {
        val s = ExpenseLedger.stats(emptyList(), today, ownedSince = null)
        assertEquals(1, s.monthsOwned)
        assertEquals(0.0, s.avgPerMonth, 1e-9)
        assertNull(s.costPerKm)
    }

    @Test fun lastMonthReportComparesWithTheMonthBefore() {
        val r = ExpenseLedger.previousMonthReport(spends, today, monthlyTarget = 300.0)
        assertEquals("2026-09", Dates.monthIso(r.key))
        assertEquals(130.0, r.total, 1e-9)
        assertEquals(1900.0, r.previousTotal, 1e-9)
        assertEquals(SpendCategory.MAINTENANCE, r.top)
        assertEquals(-1770.0, r.change, 1e-9)
    }

    @Test fun januaryReportLooksAtDecember() {
        val r = ExpenseLedger.previousMonthReport(listOf(spend("2026-12-24", SpendCategory.FUEL, 50.0)), day("2027-01-01"), null)
        assertEquals("2026-12", Dates.monthIso(r.key))
        assertEquals(50.0, r.total, 1e-9)
    }

    @Test fun unknownCategoryNamesBecomeOther() {
        assertEquals(SpendCategory.FINE, ExpenseLedger.categoryOf("FINE"))
        assertEquals(SpendCategory.OTHER, ExpenseLedger.categoryOf("ROCKET"))
        // Derived categories can't be typed into the expenses table.
        assertEquals(SpendCategory.OTHER, ExpenseLedger.categoryOf("FUEL"))
    }

    @Test fun usedSavingAgainstNew() {
        val (saved, fraction) = ExpenseLedger.usedSaving(10500.0, 17000.0)!!
        assertEquals(6500.0, saved, 1e-9)
        assertEquals(0.38235, fraction, 1e-4)
        assertNull(ExpenseLedger.usedSaving(10500.0, null))
    }

    @Test fun engineClassFromCc() {
        assertEquals(EngineClass.CC50, EngineClass.of(Powertrain.PETROL, 50))
        assertEquals(EngineClass.CC125, EngineClass.of(Powertrain.PETROL, 125))
        assertEquals(EngineClass.MID, EngineClass.of(Powertrain.PETROL, 150))
        assertEquals(EngineClass.MAXI, EngineClass.of(Powertrain.PETROL, 300))
        assertEquals(EngineClass.ELECTRIC, EngineClass.of(Powertrain.ELECTRIC, 0))
        assertEquals(Cooling.LIQUID, VehicleSpec.parse("PETROL", 300, "INJECTION", "LIQUID").cooling)
        assertEquals(FuelSystem.CARBURETOR, VehicleSpec.parse("PETROL", 50, "???", "AIR").fuelSystem)
    }
}
