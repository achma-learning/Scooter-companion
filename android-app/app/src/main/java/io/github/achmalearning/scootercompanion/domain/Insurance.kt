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

/** Insurance cost spread over the months a contract covers, and the totals per year. */
data class InsuranceMonths(
    /** Month key (see [Dates.monthKey]) → cost per month of the contracts covering it. */
    val perMonth: List<Pair<Int, Double>>,
    /** Calendar year → amount actually paid that year (contracts dated by their start). */
    val paidPerYear: Map<Int, Double>,
    /** Average cost per covered month, over every contract. */
    val averagePerMonth: Double?
)

object Insurance {
    /** Below this many days left, the app reminds the owner every day until they renew. */
    const val REMIND_DAYS = 20

    /**
     * Coverage on [today]. Contracts that follow on without a gap extend it, so a renewal
     * entered before the current one ends silences the reminder straight away.
     */
    /**
     * The monthly view: a 12-month contract of 2,000 starting in January counts 166.67 in
     * each month from January to December. Months with no contract count 0 (uninsured).
     */
    fun months(policies: List<InsurancePolicy>, fromKey: Int, toKey: Int): InsuranceMonths {
        val perMonth = (fromKey..toKey).map { key ->
            key to policies.filter { p ->
                val first = Dates.monthKey(p.startDate)
                key >= first && key < first + p.months
            }.sumOf { it.perMonth }
        }
        val covered = policies.sumOf { it.months }
        return InsuranceMonths(
            perMonth = perMonth,
            paidPerYear = policies.groupBy { Dates.year(it.startDate) }.mapValues { (_, v) -> v.sumOf { it.totalPaid } }.toSortedMap(),
            averagePerMonth = if (covered > 0) policies.sumOf { it.totalPaid } / covered else null
        )
    }

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
