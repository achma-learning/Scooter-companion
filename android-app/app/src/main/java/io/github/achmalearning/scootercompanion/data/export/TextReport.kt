package io.github.achmalearning.scootercompanion.data.export

import io.github.achmalearning.scootercompanion.data.content.Content
import io.github.achmalearning.scootercompanion.data.db.taskIds
import io.github.achmalearning.scootercompanion.data.repository.GarageSnapshot
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.ExpenseLedger
import io.github.achmalearning.scootercompanion.domain.InsurancePolicy
import io.github.achmalearning.scootercompanion.domain.SpendCategory
import java.util.Locale

/**
 * Everything the owner logged, as one plain-text report to paste into an AI assistant (or
 * read). English and fixed-format on purpose: section headers, ISO dates and plain numbers
 * are what a model parses best, whatever the app language. With [hideIds], the plate, VIN,
 * engine number and policy numbers are left out.
 */
object TextReport {

    fun build(s: GarageSnapshot, content: Content, hideIds: Boolean, exportedOn: String = Dates.toIso(Dates.today())): String = buildString {
        val p = s.profile
        val cur = s.region.currency.ifBlank { s.region.currencySymbol }
        fun money(v: Double?) = v?.let { "%.2f".format(Locale.US, it).removeSuffix(".00") + " $cur".trimEnd() } ?: "-"
        fun hidden(v: String) = when { v.isBlank() -> "-"; hideIds -> "[hidden]"; else -> v }
        fun line(vararg parts: Any?) = appendLine(parts.joinToString(" | ") { it?.toString() ?: "-" })
        val l = s.ledger

        appendLine("SCOOTER COMPANION - EXPENSE REPORT")
        appendLine("Exported: $exportedOn · Currency: $cur · Dates: YYYY-MM-DD")
        appendLine()
        appendLine("Suggested prompt: Here is the complete cost history of my scooter. Analyse where my money goes,")
        appendLine("how my monthly spending evolves, whether my maintenance and fuel costs are normal for this scooter,")
        appendLine("and give me 3 concrete ways to spend less without riding less safely.")
        appendLine()

        appendLine("== SCOOTER ==")
        val engine = if (s.spec.isElectric) "electric" else
            "${p.engineCc} cc, ${p.fuelSystem.lowercase().replace('_', ' ')}, ${p.cooling.lowercase()}-cooled, petrol"
        appendLine("Model: ${listOf(p.brand, p.model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "-" }} ($engine)" +
            (p.modelYear?.let { ", model year $it" } ?: ""))
        if (p.nickname.isNotBlank()) appendLine("Nickname: ${p.nickname}")
        appendLine(
            "Bought: ${if (p.boughtUsed) "used" else "new"}" +
                (p.purchaseDate?.let { " on ${Dates.toIso(it)}" } ?: "") +
                " at ${p.purchaseKm} km for ${money(p.purchasePrice)}" +
                (if (p.boughtUsed) p.newPrice?.let { " (same model new: ${money(it)})" } ?: "" else "") +
                (if (p.boughtUsed) p.previousOwners?.let { ", $it previous owner(s)" } ?: "" else "")
        )
        appendLine("Plate: ${hidden(p.plate)} · VIN: ${hidden(p.vin)} · Engine no.: ${hidden(p.engineNo)}")
        appendLine("Odometer now: ${s.currentKm} km (${s.currentKm - p.purchaseKm} km since purchase)")
        appendLine("Monthly spending target: ${money(p.monthlyBudget)}")
        appendLine()

        appendLine("== TOTALS ==")
        appendLine("Total spent including purchase: ${money(l.total)}")
        appendLine("Running costs (everything except the purchase): ${money(l.running)}")
        appendLine("Months owned: ${l.monthsOwned} · Average per month: ${money(l.avgPerMonth)} (with purchase: ${money(l.avgPerMonthWithPurchase)})")
        appendLine("This month: ${money(l.thisMonth)} · Last month: ${money(l.lastMonth)} · This year: ${money(l.thisYear)}")
        appendLine("Cost per km: running ${l.costPerKm?.let { "%.2f".format(Locale.US, it) } ?: "-"} · with purchase ${l.totalCostPerKm?.let { "%.2f".format(Locale.US, it) } ?: "-"} $cur/km")
        appendLine("By category:")
        l.byCategory.forEach { (c, v) -> appendLine("  ${c.name.lowercase()}: ${money(v)} (${(v / l.running * 100).toInt()}%)") }
        appendLine()

        appendLine("== MONTH BY MONTH (running costs) ==")
        l.months.forEach { m ->
            val parts = m.byCategory.entries.sortedByDescending { it.value }.joinToString(", ") { "${it.key.name.lowercase()} ${money(it.value)}" }
            appendLine("${Dates.monthIso(m.key)}: ${money(m.total)}" + if (parts.isNotBlank()) " ($parts)" else "")
        }
        appendLine()

        if (s.fuel.isNotEmpty()) {
            val f = s.fuelSummary
            appendLine("== FUEL (${s.fuel.size} fill-ups) ==")
            appendLine(
                "Average consumption: ${f.litersPer100?.let { "%.2f L/100 km".format(Locale.US, it) } ?: "not measured yet"} · " +
                    "average price: ${f.avgPricePerLiter?.let { "%.2f $cur/L".format(Locale.US, it) } ?: "-"} · total ${money(f.totalCost)}"
            )
            line("date", "km", "litres", "paid", "price/L", "full tank")
            s.fuel.sortedBy { it.date }.forEach { e ->
                line(Dates.toIso(e.date), e.km, "%.2f".format(Locale.US, e.liters), money(e.cost),
                    e.cost?.takeIf { e.liters > 0 }?.let { "%.2f".format(Locale.US, it / e.liters) }, if (e.full) "yes" else "no")
            }
            appendLine()
        }

        if (s.entries.isNotEmpty()) {
            appendLine("== MAINTENANCE (${s.entries.size} services) ==")
            line("date", "km", "work", "product", "parts", "labour", "total", "workshop", "notes")
            s.entries.sortedBy { it.date }.forEach { e ->
                val work = (e.taskIds.map { content.task(it)?.name ?: it } + listOfNotNull(e.title.takeIf { it.isNotBlank() })).joinToString(", ")
                line(Dates.toIso(Dates.startOfDay(e.date)), e.km, work.ifBlank { "-" }, e.product.ifBlank { "-" }, money(e.partsCost), money(e.laborCost),
                    money(e.cost), e.shop.ifBlank { "-" }, e.note.replace('\n', ' ').ifBlank { "-" })
            }
            appendLine()
        }

        if (s.insurance.isNotEmpty()) {
            appendLine("== INSURANCE ==")
            line("start", "end", "months", "paid", "per month", "insurer", "policy")
            s.insurance.sortedBy { it.startDate }.forEach { i ->
                val pol = InsurancePolicy(i.startDate, i.months, i.totalPaid)
                line(Dates.toIso(i.startDate), Dates.toIso(pol.endDate), i.months, money(i.totalPaid), money(pol.perMonth), i.insurer.ifBlank { "-" }, hidden(i.policyNumber))
            }
            appendLine("Cover today: ${s.coverage.state.name.lowercase()}" + (s.coverage.daysLeft?.let { ", $it days left" } ?: ""))
            appendLine()
        }

        if (s.expenses.isNotEmpty()) {
            appendLine("== OTHER EXPENSES ==")
            line("date", "category", "what", "amount", "notes")
            s.expenses.sortedBy { it.date }.forEach { x ->
                line(Dates.toIso(x.date), ExpenseLedger.categoryOf(x.category).name.lowercase(), x.title.ifBlank { "-" }, money(x.amount), x.notes.replace('\n', ' ').ifBlank { "-" })
            }
            appendLine()
        }

        val due = s.statuses.filter { it.kmLeft != null || it.daysLeft != null }.sortedBy { it.kmLeft ?: Int.MAX_VALUE }.take(8)
        if (due.isNotEmpty()) {
            appendLine("== MAINTENANCE COMING UP ==")
            due.forEach { st ->
                val parts = listOfNotNull(
                    st.kmLeft?.let { if (it <= 0) "${-it} km overdue" else "in $it km" },
                    st.daysLeft?.let { if (it < 0) "${-it} days overdue" else "in $it days" }
                )
                appendLine("${st.task.name}: ${parts.joinToString(", ")}")
            }
        }
        if (s.spends.none { it.category != SpendCategory.PURCHASE }) appendLine("(No running costs logged yet.)")
    }
}
