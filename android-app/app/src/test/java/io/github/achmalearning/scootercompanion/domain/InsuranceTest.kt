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
}
