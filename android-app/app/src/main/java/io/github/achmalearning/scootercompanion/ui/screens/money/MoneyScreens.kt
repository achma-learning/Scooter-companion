package io.github.achmalearning.scootercompanion.ui.screens.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.data.db.ExpenseEntity
import io.github.achmalearning.scootercompanion.data.db.InsurancePolicyEntity
import io.github.achmalearning.scootercompanion.data.region.RegionPack
import io.github.achmalearning.scootercompanion.data.region.pick
import io.github.achmalearning.scootercompanion.domain.CoverageState
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.EngineClass
import io.github.achmalearning.scootercompanion.domain.ExpenseLedger
import io.github.achmalearning.scootercompanion.domain.InsurancePolicy
import io.github.achmalearning.scootercompanion.domain.SpendCategory
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import io.github.achmalearning.scootercompanion.ui.components.BackTopBar
import io.github.achmalearning.scootercompanion.ui.components.Callout
import io.github.achmalearning.scootercompanion.ui.components.DateField
import io.github.achmalearning.scootercompanion.ui.components.EmptyState
import io.github.achmalearning.scootercompanion.ui.components.IconBadge
import io.github.achmalearning.scootercompanion.ui.components.Money
import io.github.achmalearning.scootercompanion.ui.components.NumberField
import io.github.achmalearning.scootercompanion.ui.components.Panel
import io.github.achmalearning.scootercompanion.ui.components.ScreenPadding
import io.github.achmalearning.scootercompanion.ui.components.SectionTitle
import io.github.achmalearning.scootercompanion.ui.components.StatusPill
import io.github.achmalearning.scootercompanion.ui.components.VSpace
import io.github.achmalearning.scootercompanion.ui.components.formatMoney
import io.github.achmalearning.scootercompanion.ui.screens.garage.GarageViewModel
import io.github.achmalearning.scootercompanion.ui.screens.garage.decimalText
import io.github.achmalearning.scootercompanion.ui.screens.profile.engineClassLabel
import io.github.achmalearning.scootercompanion.ui.theme.LocalStatusColors
import kotlin.math.abs
import kotlin.math.roundToInt

// ------------------------------------------------------------------------------ shared

@Composable
fun spendCategoryLabel(c: SpendCategory): String = stringResource(spendCategoryRes(c))

fun spendCategoryRes(c: SpendCategory): Int = (
    when (c) {
        SpendCategory.PURCHASE -> R.string.cat_PURCHASE
        SpendCategory.FUEL -> R.string.cat_FUEL
        SpendCategory.MAINTENANCE -> R.string.cat_MAINTENANCE
        SpendCategory.INSURANCE -> R.string.cat_INSURANCE
        SpendCategory.ACCESSORY -> R.string.cat_ACCESSORY
        SpendCategory.GEAR -> R.string.cat_GEAR
        SpendCategory.FINE -> R.string.cat_FINE
        SpendCategory.PARKING -> R.string.cat_PARKING
        SpendCategory.WASH -> R.string.cat_WASH
        SpendCategory.PAPERS -> R.string.cat_PAPERS
        SpendCategory.CHARGING -> R.string.cat_CHARGING
        SpendCategory.OTHER -> R.string.cat_OTHER
    }
)

fun spendCategoryIcon(c: SpendCategory): ImageVector = when (c) {
    SpendCategory.ACCESSORY -> Icons.Filled.ShoppingBag
    SpendCategory.GEAR -> Icons.Filled.SportsMotorsports
    SpendCategory.FINE -> Icons.Filled.Gavel
    SpendCategory.PARKING -> Icons.Filled.LocalParking
    SpendCategory.WASH -> Icons.Filled.LocalCarWash
    SpendCategory.PAPERS -> Icons.Filled.Description
    SpendCategory.CHARGING -> Icons.Filled.ElectricBolt
    SpendCategory.INSURANCE -> Icons.Filled.Shield
    else -> Icons.Filled.Receipt
}

/** One-tap names for a new expense, per category. */
@Composable
private fun suggestions(c: SpendCategory, region: RegionPack): List<String> = when (c) {
    SpendCategory.GEAR -> listOf(R.string.sugg_helmet, R.string.sugg_gloves, R.string.sugg_vest, R.string.sugg_jacket, R.string.sugg_rain).map { stringResource(it) }
    SpendCategory.ACCESSORY -> listOf(R.string.sugg_topcase, R.string.sugg_lock, R.string.sugg_phone, R.string.sugg_cover, R.string.sugg_mirror).map { stringResource(it) }
    SpendCategory.FINE -> region.fines.reasons.map { it.label() }
    SpendCategory.PAPERS -> listOf(stringResource(R.string.sugg_registration), stringResource(R.string.sugg_tax))
    SpendCategory.WASH -> listOf(stringResource(R.string.sugg_wash))
    SpendCategory.PARKING -> listOf(stringResource(R.string.sugg_parking))
    else -> emptyList()
}

// ------------------------------------------------------------------------------ Insurance

@Composable
fun InsuranceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    var editing by remember { mutableStateOf<InsurancePolicyEntity?>(null) }
    val colors = LocalStatusColors.current
    val engineClass = s.spec.engineClass
    val reference = s.region.insuranceReference(engineClass)

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.ins_title), onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // A renewal starts the day after the current cover ends.
                    val start = s.coverage.coveredUntil?.let { Dates.addDays(it, 1) } ?: Dates.today()
                    editing = InsurancePolicyEntity(startDate = start, months = 12, totalPaid = 0.0)
                },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.ins_add)) }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                val c = s.coverage
                val (color, title, text) = when (c.state) {
                    CoverageState.NONE -> Triple(colors.soon, stringResource(R.string.ins_state_none), stringResource(R.string.ins_state_none_text))
                    CoverageState.EXPIRED -> Triple(colors.due, stringResource(R.string.ins_state_expired), stringResource(R.string.ins_state_expired_text))
                    CoverageState.EXPIRING -> Triple(
                        colors.soon, stringResource(R.string.ins_state_expiring, c.daysLeft ?: 0),
                        stringResource(R.string.ins_until, Dates.display(c.coveredUntil!!)) + ". " + stringResource(R.string.ins_renew_hint)
                    )
                    CoverageState.OK -> Triple(
                        colors.ok, stringResource(R.string.ins_state_ok, c.daysLeft ?: 0),
                        stringResource(R.string.ins_until, Dates.display(c.coveredUntil!!)) + ". " + stringResource(R.string.ins_renew_hint)
                    )
                }
                Callout(title, text, color)
            }
            if (reference != null) item {
                Panel(modifier = Modifier.padding(vertical = 6.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(stringResource(R.string.ins_reference), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.ins_reference_value, formatMoney(reference), engineClassLabel(engineClass)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        s.region.insurance.note.pick().takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                        s.region.insurance.yearlySaving?.let {
                            Text(stringResource(R.string.ins_tip_12, formatMoney(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
            if (s.insurance.isNotEmpty()) item { SectionTitle(stringResource(R.string.ins_history)) }
            items(s.insurance, key = { it.id }) { p ->
                val policy = InsurancePolicy(p.startDate, p.months, p.totalPaid)
                val active = p.startDate <= s.today && policy.endDate >= s.today
                Panel(onClick = { editing = p }, modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Shield, if (active) colors.ok else MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                listOf(formatMoney(p.totalPaid), monthsLabel(p.months)).joinToString(" · "),
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${Dates.display(p.startDate)} → ${Dates.display(policy.endDate)}" + if (p.insurer.isNotBlank()) " · ${p.insurer}" else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { p ->
        InsuranceDialog(p, reference, onSave = { vm.saveInsurance(it); editing = null }, onDelete = { vm.deleteInsurance(p); editing = null }, onDismiss = { editing = null })
    }
}

@Composable
private fun monthsLabel(months: Int): String =
    if (months == 1) stringResource(R.string.ins_month_1) else stringResource(R.string.ins_months_n, months)

@Composable
private fun InsuranceDialog(
    p: InsurancePolicyEntity,
    yearlyReference: Double?,
    onSave: (InsurancePolicyEntity) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf(1, 3, 6, 12)
    var start by remember(p) { mutableStateOf<Long?>(p.startDate) }
    var months by remember(p) { mutableStateOf(p.months.toString()) }
    var custom by remember(p) { mutableStateOf(p.months !in presets) }
    var paid by remember(p) { mutableStateOf(if (p.totalPaid > 0) decimalText(p.totalPaid) else "") }
    var insurer by remember(p) { mutableStateOf(p.insurer) }
    var number by remember(p) { mutableStateOf(p.policyNumber) }
    val monthsValue = months.toIntOrNull()?.takeIf { it in 1..60 }
    val paidValue = paid.toDoubleOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (p.id == 0L) R.string.ins_new else R.string.ins_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(start, { start = it }, stringResource(R.string.ins_start))
                Text(stringResource(R.string.ins_duration), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { m ->
                        FilterChip(selected = !custom && monthsValue == m, onClick = { custom = false; months = m.toString() }, label = { Text(monthsLabel(m)) })
                    }
                    FilterChip(selected = custom, onClick = { custom = true }, label = { Text(stringResource(R.string.ins_custom)) })
                }
                if (custom) NumberField(months, { months = it.take(2) }, stringResource(R.string.ins_months_field))
                NumberField(paid, { paid = it }, stringResource(R.string.ins_total_paid), decimal = true, suffix = Money.symbol.ifBlank { null })
                Text(stringResource(R.string.ins_total_paid_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (start != null && monthsValue != null && paidValue != null) {
                    val policy = InsurancePolicy(start!!, monthsValue, paidValue)
                    Text(
                        stringResource(R.string.ins_summary, Dates.display(policy.endDate), formatMoney(policy.perMonth), formatMoney(policy.perYear)),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary
                    )
                    yearlyReference?.let { ref ->
                        val diff = ((policy.perYear - ref) / ref * 100).roundToInt()
                        if (abs(diff) >= 5) Text(
                            stringResource(if (diff < 0) R.string.ins_vs_reference else R.string.ins_vs_reference_above, abs(diff)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedTextField(insurer, { insurer = it }, label = { Text(stringResource(R.string.ins_insurer)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(number, { number = it }, label = { Text(stringResource(R.string.ins_policy)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                enabled = start != null && monthsValue != null && paidValue != null,
                onClick = { onSave(p.copy(startDate = start!!, months = monthsValue!!, totalPaid = paidValue!!, insurer = insurer.trim(), policyNumber = number.trim())) }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (p.id != 0L) TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete), color = LocalStatusColors.current.due) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )
}

// ------------------------------------------------------------------------------ Expenses

/** Accessories, gear, fines, parking, washing, papers, charging, anything else. */
@Composable
fun ExpensesScreen(onBack: () -> Unit, initialCategory: SpendCategory? = null) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<ExpenseEntity?>(null) }
    val shown = s.expenses.filter { filter == null || ExpenseLedger.categoryOf(it.category).name == filter }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.exp_title), onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = newExpense(filter?.let { ExpenseLedger.categoryOf(it) } ?: initialCategory ?: SpendCategory.ACCESSORY, s.currentKm) },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.exp_add)) }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    item { FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text(stringResource(R.string.exp_all)) }) }
                    items(SpendCategory.manual) { c ->
                        FilterChip(selected = filter == c.name, onClick = { filter = c.name }, label = { Text(spendCategoryLabel(c)) })
                    }
                }
                if (filter != null && shown.isNotEmpty()) Text(
                    stringResource(R.string.exp_total_category, formatMoney(shown.sumOf { it.amount })),
                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (shown.isEmpty()) item { EmptyState(Icons.Filled.Receipt, stringResource(R.string.exp_empty_title), stringResource(R.string.exp_empty_text)) }
            items(shown, key = { it.id }) { x ->
                val c = ExpenseLedger.categoryOf(x.category)
                Panel(onClick = { editing = x }, modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(spendCategoryIcon(c), if (c == SpendCategory.FINE) LocalStatusColors.current.due else MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(x.title.ifBlank { spendCategoryLabel(c) }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${spendCategoryLabel(c)} · ${Dates.display(x.date)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(formatMoney(x.amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    editing?.let { x ->
        ExpenseDialog(x, s.region, onSave = { vm.saveExpense(it); editing = null }, onDelete = { vm.deleteExpense(x); editing = null }, onDismiss = { editing = null })
    }
}

fun newExpense(category: SpendCategory, km: Int?, title: String = "") =
    ExpenseEntity(date = Dates.today(), category = category.name, title = title, amount = 0.0, km = km)

@Composable
fun ExpenseDialog(
    x: ExpenseEntity,
    region: RegionPack,
    onSave: (ExpenseEntity) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember(x) { mutableStateOf(ExpenseLedger.categoryOf(x.category)) }
    var title by remember(x) { mutableStateOf(x.title) }
    var amount by remember(x) { mutableStateOf(if (x.amount > 0) decimalText(x.amount) else "") }
    var date by remember(x) { mutableStateOf<Long?>(x.date) }
    var quantity by remember(x) { mutableStateOf(x.quantity?.let { decimalText(it) } ?: "") }
    var notes by remember(x) { mutableStateOf(x.notes) }
    val amountValue = amount.toDoubleOrNull()?.takeIf { it > 0 }
    val fine = category == SpendCategory.FINE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (x.id == 0L) R.string.exp_new else R.string.exp_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.exp_category), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpendCategory.manual.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(spendCategoryLabel(c)) })
                    }
                }
                OutlinedTextField(
                    title, { title = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(if (fine) R.string.exp_reason else R.string.exp_name)) }
                )
                val names = suggestions(category, region)
                if (names.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    names.forEach { n -> SuggestionChip(onClick = { title = n }, label = { Text(n) }) }
                }
                if (fine && region.fines.amounts.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    region.fines.amounts.forEach { a ->
                        FilterChip(selected = amountValue == a, onClick = { amount = decimalText(a) }, label = { Text(formatMoney(a)) })
                    }
                }
                NumberField(amount, { amount = it }, stringResource(R.string.exp_amount), decimal = true, suffix = Money.symbol.ifBlank { null })
                DateField(date, { date = it }, stringResource(R.string.date))
                if (category == SpendCategory.CHARGING) NumberField(quantity, { quantity = it }, stringResource(R.string.exp_kwh), decimal = true, suffix = "kWh")
                OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.exp_notes)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                enabled = amountValue != null && date != null,
                onClick = {
                    onSave(
                        x.copy(
                            category = category.name, title = title.trim(), amount = amountValue!!, date = date!!,
                            quantity = if (category == SpendCategory.CHARGING) quantity.toDoubleOrNull() else null, notes = notes.trim()
                        )
                    )
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (x.id != 0L) TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete), color = LocalStatusColors.current.due) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )
}

// ------------------------------------------------------------------------------ Recommendations

@Composable
fun RecommendationsScreen(onBack: () -> Unit, logTyres: (String) -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    var buying by remember { mutableStateOf<ExpenseEntity?>(null) }
    val engineClass: EngineClass = s.spec.engineClass

    Scaffold(topBar = { BackTopBar(stringResource(R.string.rec_title), onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                Text(stringResource(R.string.rec_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            }
            items(s.region.recommendations, key = { it.id }) { r ->
                val size = r.sizeByClass[engineClass.name]
                val name = r.name.pick() + (size?.let { " $it" } ?: "")
                Panel(modifier = Modifier.padding(vertical = 6.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.ThumbUp, MaterialTheme.colorScheme.primary)
                            Text(r.name.pick(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp))
                        }
                        VSpace(6.dp)
                        Text(r.why.pick(), style = MaterialTheme.typography.bodyMedium)
                        size?.let { Text(stringResource(R.string.rec_size, it), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp)) }
                        r.priceHint.pick().takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                        FilledTonalButton(
                            onClick = {
                                if (r.category == "TYRES") logTyres(r.name.pick())
                                else buying = newExpense(ExpenseLedger.categoryOf(r.category), s.currentKm, name)
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null)
                            Text(" " + stringResource(R.string.rec_bought))
                        }
                    }
                }
            }
        }
    }

    buying?.let { x -> ExpenseDialog(x, s.region, onSave = { vm.saveExpense(it); buying = null }, onDelete = { buying = null }, onDismiss = { buying = null }) }
}

/** Shared by the dashboard: a coloured line about insurance cover. */
@Composable
fun coverageLine(state: CoverageState, daysLeft: Int?): Pair<String, Color> {
    val colors = LocalStatusColors.current
    return when (state) {
        CoverageState.NONE -> stringResource(R.string.ins_state_none) to colors.soon
        CoverageState.EXPIRED -> stringResource(R.string.ins_state_expired) to colors.due
        CoverageState.EXPIRING -> stringResource(R.string.ins_state_expiring, daysLeft ?: 0) to colors.soon
        CoverageState.OK -> stringResource(R.string.ins_state_ok, daysLeft ?: 0) to colors.ok
    }
}
