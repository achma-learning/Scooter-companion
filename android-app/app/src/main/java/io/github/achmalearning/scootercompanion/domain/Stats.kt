package io.github.achmalearning.scootercompanion.domain

import kotlin.math.roundToInt

/**
 * The six series the charts draw. Twelve spending categories are too many colours to tell
 * apart, so they fold into groups; the category table keeps the detail.
 */
enum class ChartGroup {
    ENERGY, MAINTENANCE, INSURANCE, EQUIPMENT, FINES, OTHER;

    companion object {
        fun of(c: SpendCategory): ChartGroup = when (c) {
            SpendCategory.FUEL, SpendCategory.CHARGING -> ENERGY
            SpendCategory.MAINTENANCE -> MAINTENANCE
            SpendCategory.INSURANCE -> INSURANCE
            SpendCategory.ACCESSORY, SpendCategory.GEAR -> EQUIPMENT
            SpendCategory.FINE -> FINES
            else -> OTHER
        }
    }
}

data class MonthBar(val key: Int, val byGroup: Map<ChartGroup, Double>) {
    val total: Double get() = byGroup.values.sum()
}

data class Point(val date: Long, val value: Double)

data class DayTotal(val date: Long, val total: Double)

/** What the statistics screen draws, for one time range. */
data class StatsData(
    val months: List<MonthBar>,
    /** Running costs added up day by day over the range (purchase excluded). */
    val cumulative: List<Point>,
    /** Price per litre of each priced fill-up. */
    val pricePerLiter: List<Point>,
    /** L/100 km of each full-to-full segment, dated at the closing fill-up. */
    val consumption: List<Point>,
    /** Spending per day for the heatmap, last [HEATMAP_WEEKS] weeks. */
    val days: List<DayTotal>,
    val rangeTotal: Double,
    val byCategory: List<Pair<SpendCategory, Double>>
) {
    companion object {
        const val HEATMAP_WEEKS = 26
    }
}

object Stats {

    /** [months] = how many calendar months back from [today]'s month, or null for everything. */
    fun build(spends: List<Spend>, fills: List<FuelFill>, today: Long, months: Int?): StatsData {
        val running = spends.filter { it.category != SpendCategory.PURCHASE && it.date <= today }
        val nowKey = Dates.monthKey(today)
        val firstKey = when {
            months != null -> nowKey - months + 1
            else -> running.minOfOrNull { Dates.monthKey(it.date) } ?: nowKey
        }
        val inRange = running.filter { Dates.monthKey(it.date) >= firstKey }

        val byMonth = inRange.groupBy { Dates.monthKey(it.date) }
        val bars = (firstKey..nowKey).map { key ->
            MonthBar(key, byMonth[key].orEmpty().groupBy { ChartGroup.of(it.category) }.mapValues { (_, v) -> v.sumOf { it.amount } })
        }

        var acc = 0.0
        val cumulative = inRange.groupBy { it.date }.toSortedMap().map { (day, items) ->
            acc += items.sumOf { it.amount }
            Point(day, acc)
        }

        val rangeStart = Dates.monthStart(firstKey)
        val priced = fills.filter { it.cost != null && it.liters > 0 && it.date >= rangeStart }.sortedBy { it.date }
        val heatStart = Dates.addDays(today, -7 * StatsData.HEATMAP_WEEKS + 1)

        return StatsData(
            months = bars,
            cumulative = cumulative,
            pricePerLiter = priced.map { Point(it.date, it.cost!! / it.liters) },
            consumption = FuelStats.segments(fills).filter { it.date >= rangeStart },
            days = running.filter { it.date >= heatStart }.groupBy { it.date }.map { (d, v) -> DayTotal(d, v.sumOf { it.amount }) },
            rangeTotal = inRange.sumOf { it.amount },
            byCategory = inRange.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amount } }
                .toList().sortedByDescending { it.second }
        )
    }
}

/** A short, plain-language finding for the statistics screen. The UI turns it into a sentence. */
sealed interface Insight {
    /** One group takes most of the running costs. */
    data class TopShare(val group: ChartGroup, val percent: Int) : Insight
    /** This month compared with the average month. */
    data class VsAverage(val percent: Int) : Insight
    /** The month is heading over the target. */
    data class HeadingOver(val projected: Double, val target: Double) : Insight
    /** Price per litre moved over the last 90 days. */
    data class FuelPriceTrend(val percent: Int) : Insight
    /** The next maintenance task by km, and roughly when at the current pace. */
    data class NextService(val taskId: String, val kmLeft: Int, val days: Int?) : Insight
    /** Fines this year. */
    data class Fines(val total: Double) : Insight
    /** Purchase plus running costs, per km. */
    data class CostPerKm(val perKm: Double) : Insight
}

object Insights {

    fun build(
        ledger: LedgerStats,
        spends: List<Spend>,
        fills: List<FuelFill>,
        statuses: List<TaskStatus>,
        kmPerDay: Double?,
        today: Long
    ): List<Insight> = buildList {
        val running = ledger.byCategory.filter { it.first != SpendCategory.PURCHASE }
        val total = running.sumOf { it.second }
        if (total > 0) {
            val top = running.groupBy { ChartGroup.of(it.first) }.mapValues { (_, v) -> v.sumOf { it.second } }.maxByOrNull { it.value }
            if (top != null) {
                val share = (top.value / total * 100).roundToInt()
                if (share >= 40) add(Insight.TopShare(top.key, share))
            }
        }
        ledger.budget?.let { b -> if (!b.over && b.projectedOver) add(Insight.HeadingOver(b.projected, b.target)) }
        if (ledger.monthsOwned >= 2 && ledger.avgPerMonth > 0) {
            val pct = ((ledger.thisMonth - ledger.avgPerMonth) / ledger.avgPerMonth * 100).roundToInt()
            if (kotlin.math.abs(pct) >= 15) add(Insight.VsAverage(pct))
        }
        fuelTrend(fills, today)?.let { add(it) }
        statuses.filter { it.kmLeft != null && it.kmLeft > 0 }.minByOrNull { it.kmLeft!! }?.let { st ->
            val days = kmPerDay?.takeIf { it > 0 }?.let { (st.kmLeft!! / it).roundToInt() }
            add(Insight.NextService(st.task.id, st.kmLeft!!, days))
        }
        val fines = spends.filter { it.category == SpendCategory.FINE && Dates.year(it.date) == Dates.year(today) }.sumOf { it.amount }
        if (fines > 0) add(Insight.Fines(fines))
        ledger.totalCostPerKm?.let { add(Insight.CostPerKm(it)) }
    }

    /** Average price of the last 3 priced fills against the 3 before them, within 90 days. */
    fun fuelTrend(fills: List<FuelFill>, today: Long): Insight.FuelPriceTrend? {
        val recent = fills.filter { it.cost != null && it.liters > 0 && it.date >= Dates.addDays(today, -90) }.sortedBy { it.date }
        if (recent.size < 4) return null
        val prices = recent.map { it.cost!! / it.liters }
        val newer = prices.takeLast(3).average()
        val older = prices.dropLast(3).takeLast(3).average()
        val pct = ((newer - older) / older * 100).roundToInt()
        return if (kotlin.math.abs(pct) >= 2) Insight.FuelPriceTrend(pct) else null
    }

    /** Average km per day from odometer-carrying records over the last 60 days. */
    fun kmPerDay(readings: List<Pair<Long, Int>>, today: Long): Double? {
        val recent = readings.filter { it.first >= Dates.addDays(today, -60) }
        val first = recent.minByOrNull { it.first } ?: return null
        val last = recent.maxByOrNull { it.first } ?: return null
        val days = Dates.daysBetween(first.first, last.first)
        if (days < 7 || last.second <= first.second) return null
        return (last.second - first.second).toDouble() / days
    }
}
