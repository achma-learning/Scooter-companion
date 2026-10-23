package io.github.achmalearning.scootercompanion.domain

/**
 * Where money went. PURCHASE comes from the profile, FUEL from the fuel log, MAINTENANCE from
 * the logbook, INSURANCE from the contracts; every other category is an entry in the expenses
 * table (stored by name, see [ExpenseLedger.categoryOf]).
 */
enum class SpendCategory(
    /** One-off costs are not extrapolated when projecting the month (see [ExpenseLedger.stats]). */
    val recurring: Boolean = false
) {
    PURCHASE,
    FUEL(recurring = true),
    MAINTENANCE,
    INSURANCE,
    ACCESSORY,
    GEAR,
    FINE,
    PARKING(recurring = true),
    WASH(recurring = true),
    PAPERS,
    CHARGING(recurring = true),
    OTHER;

    companion object {
        /** Categories the owner can pick for an entry in the expenses table. */
        val manual = listOf(ACCESSORY, GEAR, FINE, PARKING, WASH, PAPERS, CHARGING, OTHER)
    }
}

/** Which screen a payment was logged in, so the expense tracker can open it for editing. */
enum class SpendSource { PURCHASE, FUEL, SERVICE, INSURANCE, EXPENSE }

/** One payment, whatever module it was logged in. [refId] is its row id in that module. */
data class Spend(
    val date: Long,
    val category: SpendCategory,
    val amount: Double,
    val label: String = "",
    val source: SpendSource = SpendSource.EXPENSE,
    val refId: Long = 0
)

data class MonthTotal(val key: Int, val total: Double, val byCategory: Map<SpendCategory, Double>)

/** The monthly target: how much is spent so far, and where the month is heading. */
data class BudgetStatus(
    val target: Double,
    val spent: Double,
    val projected: Double
) {
    val remaining: Double get() = target - spent
    val fraction: Double get() = if (target > 0) spent / target else 0.0
    val over: Boolean get() = spent > target
    val projectedOver: Boolean get() = projected > target
}

data class LedgerStats(
    /** Everything, purchase price included. */
    val total: Double,
    /** Running costs: everything except the purchase price. */
    val running: Double,
    val purchase: Double,
    val thisMonth: Double,
    val lastMonth: Double,
    val thisYear: Double,
    /** Running costs per month owned. */
    val avgPerMonth: Double,
    /** Purchase price included, spread over the months owned. */
    val avgPerMonthWithPurchase: Double,
    val monthsOwned: Int,
    /** Running costs this month extrapolated to its last day (recurring categories only). */
    val projectedThisMonth: Double,
    val budget: BudgetStatus?,
    /** Running cost per km driven since purchase, null before any distance. */
    val costPerKm: Double?,
    /** Total cost of ownership per km, purchase included. */
    val totalCostPerKm: Double?,
    /** Oldest month first, every month from the first spend to today, empty months included. */
    val months: List<MonthTotal>,
    val byYear: Map<Int, Double>,
    /** Running costs by category, biggest first. */
    val byCategory: List<Pair<SpendCategory, Double>>
)

/** Last month in one paragraph: the 1st-of-the-month notification. */
data class MonthReport(
    val key: Int,
    val total: Double,
    val previousTotal: Double,
    val target: Double?,
    val top: SpendCategory?,
    val topAmount: Double
) {
    val change: Double get() = total - previousTotal
}

object ExpenseLedger {

    fun categoryOf(name: String): SpendCategory =
        SpendCategory.manual.firstOrNull { it.name == name } ?: SpendCategory.OTHER

    /**
     * Running costs only, so a purchase never makes a month look like a disaster. Months are
     * calendar months in UTC (all dates are UTC-midnight days).
     */
    fun stats(
        spends: List<Spend>,
        today: Long,
        ownedSince: Long?,
        monthlyTarget: Double? = null,
        kmDriven: Int = 0
    ): LedgerStats {
        val running = spends.filter { it.category != SpendCategory.PURCHASE && it.date <= today }
        val purchase = spends.filter { it.category == SpendCategory.PURCHASE }.sumOf { it.amount }
        val runningTotal = running.sumOf { it.amount }
        val nowKey = Dates.monthKey(today)

        val firstDay = listOfNotNull(ownedSince, running.minOfOrNull { it.date }).minOrNull() ?: today
        val firstKey = minOf(Dates.monthKey(firstDay), nowKey)
        val monthsOwned = nowKey - firstKey + 1

        val byMonth = running.groupBy { Dates.monthKey(it.date) }
        val months = (firstKey..nowKey).map { key ->
            val items = byMonth[key].orEmpty()
            MonthTotal(key, items.sumOf { it.amount }, items.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } })
        }
        val thisMonthItems = byMonth[nowKey].orEmpty()
        val thisMonth = thisMonthItems.sumOf { it.amount }

        // Fuel, parking, washing, charging keep coming at the same pace; an insurance payment
        // or a new helmet doesn't, so only the recurring part is extrapolated.
        val elapsed = Dates.dayOfMonth(today)
        val left = Dates.daysInMonth(nowKey) - elapsed
        val recurringSoFar = thisMonthItems.filter { it.category.recurring }.sumOf { it.amount }
        val projected = thisMonth + recurringSoFar / elapsed * left

        return LedgerStats(
            total = runningTotal + purchase,
            running = runningTotal,
            purchase = purchase,
            thisMonth = thisMonth,
            lastMonth = byMonth[nowKey - 1].orEmpty().sumOf { it.amount },
            thisYear = running.filter { Dates.year(it.date) == Dates.year(today) }.sumOf { it.amount },
            avgPerMonth = runningTotal / monthsOwned,
            avgPerMonthWithPurchase = (runningTotal + purchase) / monthsOwned,
            monthsOwned = monthsOwned,
            projectedThisMonth = projected,
            budget = monthlyTarget?.takeIf { it > 0 }?.let { BudgetStatus(it, thisMonth, projected) },
            costPerKm = if (kmDriven > 0) runningTotal / kmDriven else null,
            totalCostPerKm = if (kmDriven > 0) (runningTotal + purchase) / kmDriven else null,
            months = months,
            byYear = running.groupBy { Dates.year(it.date) }.mapValues { (_, v) -> v.sumOf { it.amount } }.toSortedMap(),
            byCategory = running.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
                .toList().sortedByDescending { it.second }
        )
    }

    /** Report for the month before the one containing [today]. */
    fun previousMonthReport(spends: List<Spend>, today: Long, monthlyTarget: Double?): MonthReport {
        val key = Dates.monthKey(today) - 1
        val running = spends.filter { it.category != SpendCategory.PURCHASE }
        val inMonth = running.filter { Dates.monthKey(it.date) == key }
        val top = inMonth.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }.maxByOrNull { it.value }
        return MonthReport(
            key = key,
            total = inMonth.sumOf { it.amount },
            previousTotal = running.filter { Dates.monthKey(it.date) == key - 1 }.sumOf { it.amount },
            target = monthlyTarget?.takeIf { it > 0 },
            top = top?.key,
            topAmount = top?.value ?: 0.0
        )
    }

    /** What a used scooter saved against the same model new; null unless both prices exist. */
    fun usedSaving(pricePaid: Double?, newPrice: Double?): Pair<Double, Double>? {
        if (pricePaid == null || newPrice == null || newPrice <= 0) return null
        val saved = newPrice - pricePaid
        return saved to saved / newPrice
    }
}
