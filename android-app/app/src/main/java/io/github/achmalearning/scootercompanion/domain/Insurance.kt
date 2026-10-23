package io.github.achmalearning.scootercompanion.domain

/** One contract as the owner entered it: start day, length, and the amount actually paid. */
data class InsurancePolicy(val startDate: Long, val months: Int, val totalPaid: Double) {
    /** Last day covered: a 12-month contract from 15 Jan 2026 runs to 14 Jan 2027. */
    val endDate: Long get() = Dates.addDays(Dates.addMonths(startDate, months), -1)
    val perMonth: Double get() = if (months > 0) totalPaid / months else totalPaid
    val perYear: Double get() = perMonth * 12
}

enum class CoverageState {
    /** No contract entered yet. */
    NONE,
    /** Not covered today. Riding uninsured is illegal (RC is mandatory in Morocco). */
    EXPIRED,
    /** Covered, but fewer than [Insurance.REMIND_DAYS] days left: remind every day. */
    EXPIRING,
    OK
}

data class Coverage(
    val state: CoverageState,
    /** Last covered day, following renewals entered ahead of time; null if not covered. */
    val coveredUntil: Long?,
    /** Days from today to [coveredUntil] (0 on the last day); null if not covered. */
    val daysLeft: Int?
)

object Insurance {
    /** Below this many days left, the app reminds the owner every day until they renew. */
    const val REMIND_DAYS = 20

    /**
     * Coverage on [today]. Contracts that follow on without a gap extend it, so a renewal
     * entered before the current one ends silences the reminder straight away.
     */
    fun coverage(policies: List<InsurancePolicy>, today: Long): Coverage {
        if (policies.isEmpty()) return Coverage(CoverageState.NONE, null, null)
        val active = policies.filter { it.startDate <= today && it.endDate >= today }
        if (active.isEmpty()) return Coverage(CoverageState.EXPIRED, null, null)
        var until = active.maxOf { it.endDate }
        // Walk forward through contracts that start by the day after the current end.
        val later = policies.filter { it.endDate > until }.sortedBy { it.startDate }
        for (p in later) if (p.startDate <= Dates.addDays(until, 1)) until = maxOf(until, p.endDate)
        val daysLeft = Dates.daysBetween(today, until)
        val state = if (daysLeft < REMIND_DAYS) CoverageState.EXPIRING else CoverageState.OK
        return Coverage(state, until, daysLeft)
    }
}
