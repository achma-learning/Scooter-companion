package io.github.achmalearning.scootercompanion.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.data.repository.GarageSnapshot
import io.github.achmalearning.scootercompanion.domain.ChartGroup
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.FuelFill
import io.github.achmalearning.scootercompanion.domain.Insight
import io.github.achmalearning.scootercompanion.domain.Insights
import io.github.achmalearning.scootercompanion.domain.Stats
import io.github.achmalearning.scootercompanion.domain.StatsData
import io.github.achmalearning.scootercompanion.ui.charts.CalendarHeatmap
import io.github.achmalearning.scootercompanion.ui.charts.LineChart
import io.github.achmalearning.scootercompanion.ui.charts.StackedBarChart
import io.github.achmalearning.scootercompanion.ui.charts.chartColors
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import io.github.achmalearning.scootercompanion.ui.components.EmptyState
import io.github.achmalearning.scootercompanion.ui.components.Money
import io.github.achmalearning.scootercompanion.ui.components.Panel
import io.github.achmalearning.scootercompanion.ui.components.ScreenPadding
import io.github.achmalearning.scootercompanion.ui.components.SectionTitle
import io.github.achmalearning.scootercompanion.ui.components.StatTile
import io.github.achmalearning.scootercompanion.ui.components.VSpace
import io.github.achmalearning.scootercompanion.ui.components.formatKm
import io.github.achmalearning.scootercompanion.ui.components.formatMoney
import io.github.achmalearning.scootercompanion.ui.screens.garage.GarageViewModel
import io.github.achmalearning.scootercompanion.ui.screens.money.spendCategoryLabel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val RANGES = listOf(3, 6, 12, null)

@Composable
fun groupLabel(g: ChartGroup): String = stringResource(
    when (g) {
        ChartGroup.ENERGY -> R.string.group_ENERGY
        ChartGroup.MAINTENANCE -> R.string.group_MAINTENANCE
        ChartGroup.INSURANCE -> R.string.group_INSURANCE
        ChartGroup.EQUIPMENT -> R.string.group_EQUIPMENT
        ChartGroup.FINES -> R.string.group_FINES
        ChartGroup.OTHER -> R.string.group_OTHER
    }
)

private fun utcFormat(pattern: String) = SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }

/**
 * The "Stats" tab: where the money goes, month by month, with the target, a running total,
 * the category split, fuel price and consumption, a spending calendar, and plain-language
 * insights. Every chart has its numbers as text too (tooltips, labels and tables).
 */
@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    var range by rememberSaveable { mutableStateOf(1) }
    var selectedBar by remember(range) { mutableStateOf<Int?>(null) }
    val hidden = remember { mutableStateListOf<ChartGroup>() }
    var pickedDay by remember { mutableStateOf<Long?>(null) }

    val fills = remember(s.fuel) { s.fuel.map { FuelFill(it.date, it.km, it.liters, it.cost, it.full) } }
    val data = remember(s.spends, fills, s.today, range) { Stats.build(s.spends, fills, s.today, RANGES[range]) }
    val insights = insightsFor(s, fills)
    val colors = chartColors()
    val monthShort = remember { utcFormat("MMM") }
    val monthLong = remember { utcFormat("MMMM yyyy") }
    val dayShort = remember { utcFormat("d MMM") }

    LazyColumn(contentPadding = ScreenPadding) {
        item {
            Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                items(RANGES.indices.toList()) { i ->
                    FilterChip(
                        selected = range == i, onClick = { range = i },
                        label = {
                            Text(
                                stringResource(
                                    when (RANGES[i]) { 3 -> R.string.stats_range_3; 6 -> R.string.stats_range_6; 12 -> R.string.stats_range_12; else -> R.string.stats_range_all }
                                )
                            )
                        }
                    )
                }
            }
        }
        if (s.spends.none { it.category != io.github.achmalearning.scootercompanion.domain.SpendCategory.PURCHASE }) {
            item { EmptyState(Icons.Filled.BarChart, stringResource(R.string.stats_empty_title), stringResource(R.string.stats_empty_text)) }
            return@LazyColumn
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(formatMoney(data.rangeTotal), stringResource(R.string.stats_in_range), Modifier.weight(1f))
                StatTile(formatMoney(s.ledger.avgPerMonth), stringResource(R.string.home_avg_month), Modifier.weight(1f))
                StatTile(formatMoney(s.ledger.thisMonth), stringResource(R.string.home_this_month), Modifier.weight(1f))
            }
        }

        if (insights.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.stats_insights)) }
            items(insights) { text ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Lightbulb, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        // ---- spending per month
        item {
            SectionTitle(stringResource(R.string.stats_monthly))
            Panel {
                Column(Modifier.padding(12.dp)) {
                    val stacks = data.months.map { m -> ChartGroup.entries.filter { it !in hidden }.map { it to (m.byGroup[it] ?: 0.0) } }
                    StackedBarChart(
                        labels = data.months.map { monthShort.format(Dates.monthStart(it.key)) },
                        stacks = stacks,
                        target = s.profile.monthlyBudget,
                        targetLabel = stringResource(R.string.stats_target),
                        selected = selectedBar,
                        onSelect = { selectedBar = it }
                    )
                    Text(stringResource(R.string.stats_monthly_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    VSpace(6.dp)
                    Legend(data, hidden = hidden.toSet(), onToggle = { g -> if (g in hidden) hidden.remove(g) else hidden.add(g) })
                    selectedBar?.let { i -> data.months.getOrNull(i) }?.let { m ->
                        VSpace(8.dp)
                        Text(
                            stringResource(R.string.stats_month_detail, monthLong.format(Dates.monthStart(m.key)).replaceFirstChar { it.uppercase() }, formatMoney(m.total)),
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
                        )
                        ChartGroup.entries.mapNotNull { g -> m.byGroup[g]?.takeIf { it > 0 }?.let { g to it } }.forEach { (g, v) ->
                            TableRow(colors.of(g), groupLabel(g), formatMoney(v))
                        }
                    }
                }
            }
        }

        // ---- cumulative
        if (data.cumulative.size >= 2) item {
            SectionTitle(stringResource(R.string.stats_cumulative))
            Panel {
                Column(Modifier.padding(12.dp)) {
                    LineChart(
                        xs = data.cumulative.map { it.date }, ys = data.cumulative.map { it.value }, color = colors.series[0],
                        format = { formatMoney(it) }, dateFormat = { dayShort.format(it) }, step = true
                    )
                    Text(stringResource(R.string.stats_cumulative_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ---- categories (the table view of the period)
        item {
            SectionTitle(stringResource(R.string.stats_categories))
            Panel {
                Column(Modifier.padding(12.dp)) {
                    val max = data.byCategory.maxOfOrNull { it.second } ?: 1.0
                    data.byCategory.forEach { (cat, v) ->
                        val g = ChartGroup.of(cat)
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(spendCategoryLabel(cat), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(110.dp), maxLines = 1)
                            Box(Modifier.weight(1f).height(14.dp)) {
                                Box(Modifier.fillMaxWidth((v / max).toFloat().coerceIn(0.02f, 1f)).height(14.dp).clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)).background(colors.of(g)))
                            }
                            Text(
                                "${formatMoney(v)} · ${(v / data.rangeTotal * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp).width(104.dp), maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ---- fuel price and consumption (one measure per chart: never two y-axes)
        if (data.pricePerLiter.size >= 2) item {
            SectionTitle(stringResource(R.string.stats_price_l))
            Panel {
                Box(Modifier.padding(12.dp)) {
                    LineChart(
                        xs = data.pricePerLiter.map { it.date }, ys = data.pricePerLiter.map { it.value }, color = colors.series[1],
                        format = { "%.2f %s/L".format(Locale.US, it, Money.symbol) }, dateFormat = { dayShort.format(it) },
                        showMean = stringResource(R.string.stats_average), zeroBased = false
                    )
                }
            }
        }
        if (data.consumption.size >= 2) item {
            SectionTitle(stringResource(R.string.stats_consumption))
            Panel {
                Box(Modifier.padding(12.dp)) {
                    LineChart(
                        xs = data.consumption.map { it.date }, ys = data.consumption.map { it.value }, color = colors.series[2],
                        format = { "%.2f L".format(Locale.US, it) }, dateFormat = { dayShort.format(it) },
                        showMean = stringResource(R.string.stats_average), zeroBased = false
                    )
                }
            }
        }

        // ---- calendar
        item {
            SectionTitle(stringResource(R.string.stats_calendar))
            Panel {
                Column(Modifier.padding(12.dp)) {
                    val weeks = StatsData.HEATMAP_WEEKS
                    // Columns are weeks ending today; rows start on the weekday 26 weeks back.
                    val start = Dates.addDays(s.today, -7 * weeks + 1)
                    val values = remember(data.days) { data.days.associate { it.date to it.total } }
                    CalendarHeatmap(start, weeks, values, onPick = { pickedDay = it }, picked = pickedDay)
                    Text(
                        pickedDay?.let { stringResource(R.string.stats_day, Dates.display(it), formatMoney(values[it] ?: 0.0)) }
                            ?: stringResource(R.string.stats_calendar_hint),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

/** Legend chips: colour, name and the period's total; tap to hide or show the series. */
@Composable
private fun Legend(data: StatsData, hidden: Set<ChartGroup>, onToggle: (ChartGroup) -> Unit) {
    val colors = chartColors()
    val totals = ChartGroup.entries.associateWith { g -> data.months.sumOf { it.byGroup[g] ?: 0.0 } }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ChartGroup.entries.filter { (totals[it] ?: 0.0) > 0 }.forEach { g ->
            FilterChip(
                selected = g !in hidden,
                onClick = { onToggle(g) },
                leadingIcon = { Box(Modifier.size(10.dp).clip(CircleShape).background(if (g in hidden) colors.grid else colors.of(g))) },
                label = { Text("${groupLabel(g)} · ${formatMoney(totals[g] ?: 0.0)}", style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

@Composable
private fun TableRow(color: androidx.compose.ui.graphics.Color, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp).weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Turns the domain's findings into sentences. */
@Composable
private fun insightsFor(s: GarageSnapshot, fills: List<FuelFill>): List<String> {
    val readings = s.odometer.map { it.date to it.km } + s.fuel.map { it.date to it.km } + s.entries.map { Dates.startOfDay(it.date) to it.km }
    val list = Insights.build(s.ledger, s.spends, fills, s.statuses, Insights.kmPerDay(readings, s.today), s.today)
    return list.map { i ->
        when (i) {
            is Insight.TopShare -> stringResource(R.string.ins_top_share, groupLabel(i.group), i.percent)
            is Insight.VsAverage -> if (i.percent > 0) stringResource(R.string.ins_vs_avg_up, i.percent) else stringResource(R.string.ins_vs_avg_down, -i.percent)
            is Insight.HeadingOver -> stringResource(R.string.ins_heading_over, formatMoney(i.projected), formatMoney(i.target))
            is Insight.FuelPriceTrend -> if (i.percent > 0) stringResource(R.string.ins_fuel_up, i.percent) else stringResource(R.string.ins_fuel_down, -i.percent)
            is Insight.NextService -> {
                val name = s.tasks.firstOrNull { it.id == i.taskId }?.localName() ?: i.taskId
                if (i.days != null) stringResource(R.string.ins_next_service_days, name, formatKm(i.kmLeft), i.days)
                else stringResource(R.string.ins_next_service, name, formatKm(i.kmLeft))
            }
            is Insight.Fines -> stringResource(R.string.ins_fines, formatMoney(i.total))
            is Insight.CostPerKm -> stringResource(R.string.ins_cost_km, "%.2f %s".format(Locale.US, i.perKm, Money.symbol).trim())
        }
    }
}
