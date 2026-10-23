package io.github.achmalearning.scootercompanion.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InsuranceTest {
    private fun day(iso: String) = Dates.fromIso(iso)!!

    @Test fun aYearContractEndsTheDayBeforeTheAnniversary() {
        val p = InsurancePolicy(day("2026-01-15"), 12, 2000.0)
        assertEquals("2027-01-14", Dates.toIso(p.endDate))
        assertEquals(2000.0 / 12, p.perMonth, 1e-9)
        assertEquals(2000.0, p.perYear, 1e-9)
    }

    @Test fun noContractMeansNone() {
        assertEquals(CoverageState.NONE, Insurance.coverage(emptyList(), day("2026-10-23")).state)
    }

    @Test fun moreThanTwentyDaysLeftIsOk() {
        val c = Insurance.coverage(listOf(InsurancePolicy(day("2026-10-01"), 3, 600.0)), day("2026-10-23"))
        assertEquals(CoverageState.OK, c.state)
        assertEquals("2026-12-31", Dates.toIso(c.coveredUntil!!))
        assertEquals(69, c.daysLeft)
    }

    @Test fun underTwentyDaysLeftStartsTheDailyReminder() {
        // A 1-month contract from 1 Oct ends 31 Oct: 8 days left on 23 Oct.
        val c = Insurance.coverage(listOf(InsurancePolicy(day("2026-10-01"), 1, 200.0)), day("2026-10-23"))
        assertEquals(CoverageState.EXPIRING, c.state)
        assertEquals(8, c.daysLeft)
        // 19 days left is still a reminder, 20 is not.
        assertEquals(CoverageState.EXPIRING, Insurance.coverage(listOf(InsurancePolicy(day("2026-10-01"), 1, 200.0)), day("2026-10-12")).state)
        assertEquals(CoverageState.OK, Insurance.coverage(listOf(InsurancePolicy(day("2026-10-01"), 1, 200.0)), day("2026-10-11")).state)
    }

    @Test fun aRenewalEnteredAheadStopsTheReminder() {
        val current = InsurancePolicy(day("2026-10-01"), 1, 200.0)
        val renewal = InsurancePolicy(day("2026-11-01"), 12, 2000.0)
        val c = Insurance.coverage(listOf(current, renewal), day("2026-10-23"))
        assertEquals(CoverageState.OK, c.state)
        assertEquals("2027-10-31", Dates.toIso(c.coveredUntil!!))
    }

    @Test fun aRenewalAfterAGapDoesNotHideTheGap() {
        val current = InsurancePolicy(day("2026-10-01"), 1, 200.0)
        val late = InsurancePolicy(day("2026-11-10"), 12, 2000.0)
        val c = Insurance.coverage(listOf(current, late), day("2026-10-23"))
        assertEquals(CoverageState.EXPIRING, c.state)
        assertEquals("2026-10-31", Dates.toIso(c.coveredUntil!!))
    }

    @Test fun anEndedContractIsExpired() {
        val c = Insurance.coverage(listOf(InsurancePolicy(day("2025-01-01"), 12, 2000.0)), day("2026-10-23"))
        assertEquals(CoverageState.EXPIRED, c.state)
        assertNull(c.daysLeft)
    }

    @Test fun monthlyCostSpreadsEachContractOverItsMonths() {
        val q4 = InsurancePolicy(day("2025-10-01"), 3, 600.0)        // 200 / month, Oct-Dec 2025
        val year = InsurancePolicy(day("2026-01-15"), 12, 2000.0)    // 166.67 / month, Jan-Dec 2026
        val m = Insurance.months(listOf(q4, year), Dates.monthKey(day("2025-09-01")), Dates.monthKey(day("2026-02-01")))
        assertEquals(listOf("2025-09", "2025-10", "2025-11", "2025-12", "2026-01", "2026-02"), m.perMonth.map { Dates.monthIso(it.first) })
        assertEquals(0.0, m.perMonth[0].second, 1e-9)           // not insured yet
        assertEquals(200.0, m.perMonth[1].second, 1e-9)
        assertEquals(2000.0 / 12, m.perMonth[4].second, 1e-9)
        assertEquals(mapOf(2025 to 600.0, 2026 to 2000.0), m.paidPerYear)
        assertEquals(2600.0 / 15, m.averagePerMonth!!, 1e-9)
    }

    @Test fun overlappingRenewalsAddUp() {
        val a = InsurancePolicy(day("2026-01-01"), 2, 400.0)
        val b = InsurancePolicy(day("2026-02-01"), 1, 300.0)
        val m = Insurance.months(listOf(a, b), Dates.monthKey(day("2026-02-01")), Dates.monthKey(day("2026-02-01")))
        assertEquals(500.0, m.perMonth.single().second, 1e-9)
        assertEquals(null, Insurance.months(emptyList(), 0, 0).averagePerMonth)
    }
}
