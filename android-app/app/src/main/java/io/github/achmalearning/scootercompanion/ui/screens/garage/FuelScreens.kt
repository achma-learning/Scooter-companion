package io.github.achmalearning.scootercompanion.ui.screens.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.achmalearning.scootercompanion.domain.ChartGroup
import io.github.achmalearning.scootercompanion.ui.charts.MonthBarChart
import io.github.achmalearning.scootercompanion.ui.charts.chartColors
import io.github.achmalearning.scootercompanion.ui.screens.money.utc
import io.github.achmalearning.scootercompanion.R
import io.github.achmalearning.scootercompanion.data.db.FuelEntity
import io.github.achmalearning.scootercompanion.data.fuel.FuelPriceClient
import io.github.achmalearning.scootercompanion.domain.Dates
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
import io.github.achmalearning.scootercompanion.ui.components.StatTile
import io.github.achmalearning.scootercompanion.ui.components.VSpace
import io.github.achmalearning.scootercompanion.ui.components.formatKm
import io.github.achmalearning.scootercompanion.ui.components.formatMoney
import io.github.achmalearning.scootercompanion.ui.theme.LocalStatusColors
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@Composable
fun FuelScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    var editing by remember { mutableStateOf<FuelEntity?>(null) }
    val f = s.fuelSummary
    val fullTanks = s.fuel.count { it.full }
    val money = Money.symbol.ifBlank { "" }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.fuel_title), onBack) },
        floatingActionButton = {
            if (!s.spec.isElectric) ExtendedFloatingActionButton(
                onClick = { editing = FuelEntity(date = Dates.today(), km = s.currentKm, liters = 0.0, full = false) },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.fuel_add)) }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            if (s.spec.isElectric) {
                item { EmptyState(Icons.Filled.ElectricBolt, stringResource(R.string.type_electric), stringResource(R.string.fuel_electric)) }
                return@LazyColumn
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(f.litersPer100?.let { "%.2f".format(Locale.US, it) } ?: "—", stringResource(R.string.fuel_l100_avg), Modifier.weight(1f))
                    StatTile(f.litersPer100?.takeIf { it > 0 }?.let { "%.0f".format(Locale.US, 100 / it) } ?: "—", stringResource(R.string.fuel_km_per_l), Modifier.weight(1f))
                }
                VSpace(8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(formatMoney(f.totalCost), stringResource(R.string.fuel_total), Modifier.weight(1f))
                    StatTile(f.costPerKm?.let { "%.2f".format(Locale.US, it) } ?: "—", stringResource(R.string.fuel_per_km, money), Modifier.weight(1f))
                    StatTile(f.avgPricePerLiter?.let { "%.2f".format(Locale.US, it) } ?: "—", stringResource(R.string.fuel_per_liter_avg, money), Modifier.weight(1f))
                }
                if (s.fuel.isNotEmpty() && f.litersPer100 == null) {
                    VSpace(8.dp)
                    Callout(
                        stringResource(if (fullTanks > 0) R.string.fuel_one_more_full else R.string.fuel_fill_full_once),
                        stringResource(R.string.fuel_need_two_full, fullTanks)
                    )
                }
                Text(
                    (if (f.distance > 0) stringResource(R.string.fuel_logged, formatKm(f.distance)) + " " else "") + stringResource(R.string.fuel_rise_hint),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp)
                )
            }
            if (s.fuel.any { it.cost != null }) item { FuelMonthly(s.fuel, s.today) }
            if (s.fuel.isEmpty()) item {
                EmptyState(Icons.Filled.LocalGasStation, stringResource(R.string.fuel_empty_title), stringResource(R.string.fuel_empty_text))
            }
            items(s.fuel, key = { it.id }) { e ->
                Panel(onClick = { editing = e }, modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.LocalGasStation, MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                (e.cost?.let { formatMoney(it) + " · " } ?: "") + "%.2f L".format(Locale.US, e.liters),
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
                            )
                            val perLiter = e.cost?.takeIf { e.liters > 0 }?.let { " · %.2f %s/L".format(Locale.US, it / e.liters, money) } ?: ""
                            val full = if (e.full) " · " + stringResource(R.string.fuel_full_short) else ""
                            Text("${formatKm(e.km)} · ${Dates.display(e.date)}$perLiter$full", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    editing?.let { e ->
        FuelDialog(
            e, f.lastPricePerLiter, s.region.fuelChipsFor(s.spec.engineClass),
            onSave = { vm.saveFuel(it); editing = null }, onDelete = { vm.deleteFuel(e); editing = null }, onDismiss = { editing = null }
        )
    }
}

/** Locale-independent "15.18" / "2" for text fields (a French locale would print "15,18"). */
internal fun decimalText(v: Double, scale: Int = 2): String = BigDecimal(v).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/**
 * Most fill-ups are a fixed amount ("30 dh") rather than a full tank, so the form starts from
 * what the pump shows: amount paid × price per litre → litres. Any two give the third. The
 * amount chips come from the region pack, per engine class (10–50 dh for a 50 cc, up to
 * 200 dh for a maxi-scooter); "Full tank" marks the fill as full for the consumption figure.
 */
@Composable
internal fun FuelDialog(
    e: FuelEntity,
    defaultPrice: Double?,
    amounts: List<Double>,
    onSave: (FuelEntity) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val money = Money.symbol
    var date by remember(e) { mutableStateOf<Long?>(e.date) }
    var km by remember(e) { mutableStateOf(e.km.toString()) }
    var liters by remember(e) { mutableStateOf(if (e.liters > 0) decimalText(e.liters, scale = 4) else "") }
    var cost by remember(e) { mutableStateOf(e.cost?.let(::decimalText) ?: "") }
    var price by remember(e) {
        val p = if (e.liters > 0 && e.cost != null) e.cost / e.liters else defaultPrice
        mutableStateOf(p?.let(::decimalText) ?: "")
    }
    var full by remember(e) { mutableStateOf(e.full) }
    var lookingUp by remember { mutableStateOf(false) }
    var priceNote by remember { mutableStateOf<String?>(null) }
    val lookingUpText = stringResource(R.string.fuel_looking_up)
    val failedText = stringResource(R.string.fuel_price_failed, "%s")
    val noteText = stringResource(R.string.fuel_price_note, "%1\$s", "%2\$s", "%3\$s")

    fun litersFromCost() {
        val c = cost.toDoubleOrNull() ?: return
        val p = price.toDoubleOrNull() ?: return
        // 4 decimals so cost ÷ litres gives back the exact price next time; at 2 decimals
        // 30 DH @ 15.18 → 1.98 L → 15.15, and the remembered price would drift down.
        if (c > 0 && p > 0) liters = decimalText(c / p, scale = 4)
    }
    fun costFromLiters() {
        val l = liters.toDoubleOrNull() ?: return
        val p = price.toDoubleOrNull() ?: return
        if (l > 0 && p > 0) cost = decimalText(l * p)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (e.id == 0L) R.string.fuel_new else R.string.fuel_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(date, { date = it }, stringResource(R.string.date))
                NumberField(km, { km = it }, stringResource(R.string.odometer), suffix = "km")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    amounts.forEach { a ->
                        FilterChip(
                            selected = !full && cost.toDoubleOrNull() == a,
                            onClick = { full = false; cost = decimalText(a); litersFromCost() },
                            label = { Text("${decimalText(a)} $money".trim()) }
                        )
                    }
                    FilterChip(selected = full, onClick = { full = !full }, label = { Text(stringResource(R.string.fuel_full_chip)) })
                }
                Text(
                    stringResource(if (full) R.string.fuel_full_chip_hint else R.string.fuel_custom_hint),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NumberField(cost, { cost = it; litersFromCost() }, stringResource(R.string.fuel_paid), decimal = true, suffix = money.ifBlank { null })
                NumberField(price, {
                    price = it
                    if ((cost.toDoubleOrNull() ?: 0.0) > 0) litersFromCost() else costFromLiters()
                }, stringResource(R.string.fuel_price_per_l), decimal = true, suffix = "$money/L")
                OutlinedButton(
                    enabled = !lookingUp,
                    onClick = {
                        lookingUp = true
                        priceNote = lookingUpText
                        scope.launch {
                            FuelPriceClient.fetch()
                                .onSuccess { p ->
                                    price = decimalText(p.pricePerLiter)
                                    if ((cost.toDoubleOrNull() ?: 0.0) > 0) litersFromCost() else costFromLiters()
                                    priceNote = noteText.format(
                                        "${decimalText(p.pricePerLiter)} $money/L",
                                        Dates.fromIso(p.asOf)?.let(Dates::display) ?: p.asOf,
                                        p.source
                                    )
                                }
                                .onFailure { priceNote = failedText.format(it.message ?: "No connection") }
                            lookingUp = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CloudDownload, null)
                    Text(stringResource(R.string.fuel_today_price), Modifier.padding(start = 8.dp))
                }
                priceNote?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                NumberField(liters, { liters = it; costFromLiters() }, stringResource(R.string.fuel_liters), decimal = true, suffix = "L")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(full, { full = it })
                    Text(stringResource(R.string.fuel_full_tank))
                }
            }
        },
        confirmButton = {
            Button(
                enabled = km.toIntOrNull() != null && (liters.toDoubleOrNull() ?: 0.0) > 0 && date != null,
                onClick = { onSave(e.copy(date = date!!, km = km.toInt(), liters = liters.toDouble(), cost = cost.toDoubleOrNull(), full = full)) }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (e.id != 0L) TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete), color = LocalStatusColors.current.due) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )
}

/** What fuel cost each of the last 12 months, as bars; tap one for its total. */
@Composable
private fun FuelMonthly(fuel: List<FuelEntity>, today: Long) {
    val nowKey = Dates.monthKey(today)
    val keys = (nowKey - 11..nowKey).toList()
    val totals = remember(fuel, nowKey) {
        val byMonth = fuel.groupBy { Dates.monthKey(it.date) }
        keys.map { k -> byMonth[k].orEmpty().sumOf { it.cost ?: 0.0 } }
    }
    val monthShort = remember { utc("MMM") }
    val monthLong = remember { utc("MMMM yyyy") }
    var selected by remember { mutableStateOf<Int?>(null) }
    val avg = totals.filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()

    Panel(modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.fuel_monthly), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MonthBarChart(
                labels = keys.map { monthShort.format(Dates.monthStart(it)) },
                values = totals,
                color = chartColors().of(ChartGroup.ENERGY),
                reference = avg,
                referenceLabel = stringResource(R.string.stats_average),
                selected = selected,
                onSelect = { selected = it }
            )
            selected?.let { i ->
                Text(
                    stringResource(R.string.fuel_bar_detail, monthLong.format(Dates.monthStart(keys[i])), formatMoney(totals[i])),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
