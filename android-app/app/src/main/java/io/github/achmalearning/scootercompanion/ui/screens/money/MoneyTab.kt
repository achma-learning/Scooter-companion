package io.github.achmalearning.scootercompanion.ui.screens.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.data.db.ExpenseEntity
import io.github.achmalearning.scootercompanion.data.db.FuelEntity
import io.github.achmalearning.scootercompanion.domain.ChartGroup
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.Spend
import io.github.achmalearning.scootercompanion.domain.SpendCategory
import io.github.achmalearning.scootercompanion.domain.SpendSource
import io.github.achmalearning.scootercompanion.domain.Stats
import io.github.achmalearning.scootercompanion.ui.charts.MonthBarChart
import io.github.achmalearning.scootercompanion.ui.charts.StackedBarChart
import io.github.achmalearning.scootercompanion.ui.charts.chartColors
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import io.github.achmalearning.scootercompanion.ui.components.EmptyState
import io.github.achmalearning.scootercompanion.ui.components.IconBadge
import io.github.achmalearning.scootercompanion.ui.components.Panel
import io.github.achmalearning.scootercompanion.ui.components.ScreenPadding
import io.github.achmalearning.scootercompanion.ui.components.StatTile
import io.github.achmalearning.scootercompanion.ui.components.StatusBar
import io.github.achmalearning.scootercompanion.ui.components.VSpace
import io.github.achmalearning.scootercompanion.ui.components.formatMoney
import io.github.achmalearning.scootercompanion.ui.screens.garage.FuelDialog
import io.github.achmalearning.scootercompanion.ui.screens.garage.GarageViewModel
import io.github.achmalearning.scootercompanion.ui.screens.stats.groupLabel
import io.github.achmalearning.scootercompanion.ui.theme.LocalStatusColors
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class MoneyNav(
    /** Open the logbook entry (id) or a new service (null). */
    val service: (Long?) -> Unit,
    val insurance: () -> Unit,
    val profile: () -> Unit,
    val fuel: () -> Unit
)

/**
 * The "Expenses" tab: one tracker for every payment (purchase, fuel, services, insurance,
 * accessories, gear, fines, parking…). A monthly chart on top (tap a month to list only its
 * payments), filters by kind, the list grouped by month with subtotals, and one Add button
 * for every kind of payment. Tapping a payment opens it where it was logged.
 */
@Composable
fun MoneyTab(nav: MoneyNav) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var month by rememberSaveable { mutableStateOf<Int?>(null) }
    var adding by remember { mutableStateOf(false) }
    var expense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var fuel by remember { mutableStateOf<FuelEntity?>(null) }
    val colors = chartColors()
    val group = filter?.let { name -> ChartGroup.entries.firstOrNull { it.name == name } }
    val monthShort = remember { utc("MMM") }
    val monthLong = remember { utc("MMMM yyyy") }

    val data = remember(s.spends, s.today) { Stats.build(s.spends, emptyList(), s.today, 12) }
    val shown = s.spends
        .filter { group == null || (it.category != SpendCategory.PURCHASE && ChartGroup.of(it.category) == group) }
        .filter { month == null || Dates.monthKey(it.date) == month }
        .sortedByDescending { it.date }
    val byMonth = shown.groupBy { Dates.monthKey(it.date) }.toSortedMap(compareByDescending { it })

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.money_add)) })
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = ScreenPadding) {
            item {
                Text(stringResource(R.string.money_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.money_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(formatMoney(s.ledger.thisMonth), stringResource(R.string.home_this_month), Modifier.weight(1f))
                    StatTile(formatMoney(s.ledger.avgPerMonth), stringResource(R.string.home_avg_month), Modifier.weight(1f))
                    StatTile(formatMoney(s.ledger.total), stringResource(R.string.home_total_with_purchase), Modifier.weight(1f))
                }
                s.ledger.budget?.let { b ->
                    val st = LocalStatusColors.current
                    val color = when { b.over -> st.due; b.projectedOver -> st.soon; else -> st.ok }
                    VSpace(8.dp)
                    StatusBar(b.fraction.toFloat(), color)
                    Text(
                        if (b.over) stringResource(R.string.home_over, formatMoney(-b.remaining)) else stringResource(R.string.home_left, formatMoney(b.remaining)),
                        style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold
                    )
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                    item { FilterChip(selected = group == null, onClick = { filter = null }, label = { Text(stringResource(R.string.exp_all)) }) }
                    items(ChartGroup.entries) { g ->
                        FilterChip(selected = group == g, onClick = { filter = if (group == g) null else g.name }, label = { Text(groupLabel(g)) })
                    }
                }
                Panel {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.money_last_12), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val labels = data.months.map { monthShort.format(Dates.monthStart(it.key)) }
                        val selected = month?.let { key -> data.months.indexOfFirst { it.key == key }.takeIf { it >= 0 } }
                        val pick: (Int?) -> Unit = { i -> month = i?.let { data.months[it].key } }
                        if (group == null) {
                            StackedBarChart(
                                labels = labels,
                                stacks = data.months.map { m -> ChartGroup.entries.map { it to (m.byGroup[it] ?: 0.0) } },
                                target = s.profile.monthlyBudget, targetLabel = stringResource(R.string.stats_target),
                                selected = selected, onSelect = pick, height = 190.dp
                            )
                        } else {
                            MonthBarChart(
                                labels = labels, values = data.months.map { it.byGroup[group] ?: 0.0 }, color = colors.of(group),
                                selected = selected, onSelect = pick
                            )
                        }
                        Text(
                            month?.let { stringResource(R.string.money_showing_month, monthLong.format(Dates.monthStart(it))) } ?: stringResource(R.string.money_chart_hint),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (shown.isEmpty()) item { EmptyState(Icons.Filled.Receipt, stringResource(R.string.money_title), stringResource(R.string.money_empty)) }
            byMonth.forEach { (key, items) ->
                item(key = "m$key") {
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp)) {
                        Text(monthLong.format(Dates.monthStart(key)).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(formatMoney(items.sumOf { it.amount }), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
                items(items, key = { "${it.source}-${it.refId}-${it.date}-${it.category}" }) { sp ->
                    SpendRow(sp) {
                        when (sp.source) {
                            SpendSource.PURCHASE -> nav.profile()
                            SpendSource.FUEL -> fuel = s.fuel.firstOrNull { it.id == sp.refId }
                            SpendSource.SERVICE -> nav.service(sp.refId)
                            SpendSource.INSURANCE -> nav.insurance()
                            SpendSource.EXPENSE -> expense = s.expenses.firstOrNull { it.id == sp.refId }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text(stringResource(R.string.money_add_title)) },
            text = {
                Column {
                    if (!s.spec.isElectric) AddRow(Icons.Filled.LocalGasStation, stringResource(R.string.money_add_fuel)) {
                        adding = false; fuel = FuelEntity(date = Dates.today(), km = s.currentKm, liters = 0.0, full = false)
                    }
                    AddRow(Icons.Filled.Build, stringResource(R.string.money_add_service)) { adding = false; nav.service(null) }
                    AddRow(Icons.Filled.Shield, stringResource(R.string.money_add_insurance)) { adding = false; nav.insurance() }
                    AddRow(Icons.Filled.Receipt, stringResource(R.string.money_add_expense)) {
                        adding = false; expense = newExpense(if (s.spec.isElectric) SpendCategory.CHARGING else SpendCategory.ACCESSORY, s.currentKm)
                    }
                    AddRow(Icons.Filled.Gavel, stringResource(R.string.money_add_fine)) { adding = false; expense = newExpense(SpendCategory.FINE, s.currentKm) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { adding = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
    expense?.let { x ->
        ExpenseDialog(x, s.region, onSave = { vm.saveExpense(it); expense = null }, onDelete = { vm.deleteExpense(x); expense = null }, onDismiss = { expense = null })
    }
    fuel?.let { f ->
        FuelDialog(
            f, s.fuelSummary.lastPricePerLiter, s.region.fuelChipsFor(s.spec.engineClass),
            onSave = { vm.saveFuel(it); fuel = null }, onDelete = { vm.deleteFuel(f); fuel = null }, onDismiss = { fuel = null }
        )
    }
}

@Composable
private fun AddRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, MaterialTheme.colorScheme.primary, 36.dp)
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun SpendRow(sp: Spend, onClick: () -> Unit) {
    val icon = when (sp.source) {
        SpendSource.PURCHASE -> Icons.Filled.TwoWheeler
        SpendSource.FUEL -> Icons.Filled.LocalGasStation
        SpendSource.SERVICE -> Icons.Filled.Build
        SpendSource.INSURANCE -> Icons.Filled.Shield
        SpendSource.EXPENSE -> spendCategoryIcon(sp.category)
    }
    val tint = if (sp.category == SpendCategory.PURCHASE) MaterialTheme.colorScheme.onSurfaceVariant
    else chartColors().of(ChartGroup.of(sp.category))
    val category = if (sp.category == SpendCategory.PURCHASE) stringResource(R.string.money_purchase_label) else spendCategoryLabel(sp.category)
    Panel(onClick = onClick, modifier = Modifier.padding(vertical = 3.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint, 36.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(sp.label.ifBlank { category }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("$category · ${Dates.display(sp.date)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatMoney(sp.amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

internal fun utc(pattern: String) = SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
