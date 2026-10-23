package io.github.achmalearning.scootercompanion.ui.screens.garage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.achmalearning.scootercompanion.data.db.DocumentEntity
import io.github.achmalearning.scootercompanion.data.db.FuelEntity
import io.github.achmalearning.scootercompanion.data.fuel.FuelPriceClient
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.MilestoneStatus
import io.github.achmalearning.scootercompanion.domain.StampState
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import io.github.achmalearning.scootercompanion.ui.components.BackTopBar
import io.github.achmalearning.scootercompanion.ui.components.Callout
import io.github.achmalearning.scootercompanion.ui.components.DateField
import io.github.achmalearning.scootercompanion.ui.components.EmptyState
import io.github.achmalearning.scootercompanion.ui.components.IconBadge
import io.github.achmalearning.scootercompanion.ui.components.NumberField
import io.github.achmalearning.scootercompanion.ui.components.Panel
import io.github.achmalearning.scootercompanion.ui.components.ScreenPadding
import io.github.achmalearning.scootercompanion.ui.components.SectionTitle
import io.github.achmalearning.scootercompanion.ui.components.StatTile
import io.github.achmalearning.scootercompanion.ui.components.StatusPill
import io.github.achmalearning.scootercompanion.ui.components.VSpace
import io.github.achmalearning.scootercompanion.ui.components.formatKm
import io.github.achmalearning.scootercompanion.ui.components.formatMad
import io.github.achmalearning.scootercompanion.ui.theme.LocalStatusColors
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.launch

// ------------------------------------------------------------------------------ Passport

@Composable
fun PassportScreen(onBack: () -> Unit, openProfile: () -> Unit, logMilestone: (Int) -> Unit, editEntry: (Long) -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    val p = s.profile
    val stamped = s.milestones.count { it.state == StampState.STAMPED }
    val warrantyEnd = s.warrantyEnd

    Scaffold(topBar = { BackTopBar("Digital passport", onBack) { IconButton(onClick = openProfile) { Icon(Icons.Filled.Edit, "Edit bike") } } }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.extraLarge) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("PASSEPORT D'ENTRETIEN", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f), letterSpacing = 2.sp)
                        Text(p.nickname.ifBlank { vm.content.bike.model }, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${vm.content.bike.model} · ${vm.content.bike.code}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                        VSpace(12.dp)
                        Row {
                            PassportField("Plate", p.plate.ifBlank { "—" }, Modifier.weight(1f))
                            PassportField("Chassis (VIN)", p.vin.ifBlank { "—" }, Modifier.weight(1.4f))
                        }
                        Row(Modifier.padding(top = 8.dp)) {
                            PassportField("Purchased", p.purchaseDate?.let(Dates::display) ?: "—", Modifier.weight(1f))
                            PassportField("Dealer", p.dealer.ifBlank { "—" }, Modifier.weight(1.4f))
                        }
                    }
                }
            }
            item {
                VSpace(12.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("$stamped", "dealer stamps", Modifier.weight(1f))
                    StatTile(formatKm(s.currentKm), "odometer", Modifier.weight(1f))
                }
                if (warrantyEnd != null) {
                    val days = Dates.daysBetween(s.today, warrantyEnd)
                    val c = LocalStatusColors.current
                    Callout(
                        if (days >= 0) "Warranty valid until ${Dates.display(warrantyEnd)}" else "Warranty ended ${Dates.display(warrantyEnd)}",
                        "Have every service stamped by an approved dealer (YOUKA Moto, Mob Shop…) — an unstamped service can void a claim. The stamped booklet is also your #1 resale argument.",
                        if (days >= 0) c.ok else c.muted
                    )
                }
            }
            item { SectionTitle("Service stamps") }
            items(s.milestones.chunked(3), key = { it.first().milestone.km }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 5.dp)) {
                    row.forEach { m ->
                        Stamp(m, Modifier.weight(1f)) { m.record?.let { s.entries.firstOrNull { it.milestoneKm == m.milestone.km }?.let { editEntry(it.id) } } ?: logMilestone(m.milestone.km) }
                    }
                    repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
            item {
                VSpace(8.dp)
                Text(
                    "Red = dealer-stamped · outline = done without stamp · dashed = next. Kymco's booklet stamps 300, 1,000, 3,000 km then every 2,000 km; the wiki adds 2,000 km plus 8,000/10,000 km wear checks.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PassportField(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun Stamp(m: MilestoneStatus, modifier: Modifier, onClick: () -> Unit) {
    val red = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.outline
    val (border, fill, textColor) = when (m.state) {
        StampState.STAMPED -> Triple(red, red.copy(alpha = 0.12f), red)
        StampState.DONE -> Triple(LocalStatusColors.current.ok, Color.Transparent, LocalStatusColors.current.ok)
        StampState.NEXT -> Triple(LocalStatusColors.current.soon, LocalStatusColors.current.soon.copy(alpha = 0.08f), MaterialTheme.colorScheme.onSurface)
        StampState.MISSED -> Triple(LocalStatusColors.current.due.copy(alpha = 0.6f), Color.Transparent, LocalStatusColors.current.due)
        StampState.UPCOMING -> Triple(muted, Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = fill,
        border = BorderStroke(if (m.state == StampState.STAMPED) 3.dp else 1.5.dp, border),
        modifier = modifier.aspectRatio(1f).rotate(if (m.state == StampState.STAMPED) -8f else 0f)
    ) {
        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("%,d".format(m.milestone.km).replace(',', ' '), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
            Text("km", style = MaterialTheme.typography.labelSmall, color = textColor)
            val sub = when (m.state) {
                StampState.STAMPED, StampState.DONE -> m.record?.let { Dates.short(it.date) } ?: ""
                StampState.NEXT -> "next"
                StampState.MISSED -> "missed"
                StampState.UPCOMING -> if (m.milestone.source == "wiki") "wiki" else ""
            }
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.labelSmall, color = textColor, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

// ------------------------------------------------------------------------------ Fuel

@Composable
fun FuelScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    var editing by remember { mutableStateOf<FuelEntity?>(null) }
    val f = s.fuelSummary
    val fullTanks = s.fuel.count { it.full }

    Scaffold(
        topBar = { BackTopBar("Fuel log", onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = FuelEntity(date = Dates.today(), km = s.currentKm, liters = 0.0, full = false) },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Fill-up") }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(f.litersPer100?.let { "%.2f".format(it) } ?: "—", "L/100 km avg", Modifier.weight(1f))
                    StatTile(f.costPerKm?.let { "%.2f".format(it) } ?: "—", "MAD / km", Modifier.weight(1f))
                }
                VSpace(8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(formatMad(f.totalCost), "total spent", Modifier.weight(1f))
                    StatTile(f.avgPricePerLiter?.let { "%.2f".format(it) } ?: "—", "MAD / L avg", Modifier.weight(1f))
                }
                if (s.fuel.isNotEmpty() && f.litersPer100 == null) {
                    VSpace(8.dp)
                    Callout(
                        if (fullTanks > 0) "One more full tank" else "Fill up to full once in a while",
                        "Consumption needs two full tanks to measure between ($fullTanks logged so far). Your 20-30 MAD top-ups in between still count."
                    )
                }
                Text(
                    (if (f.distance > 0) "${formatKm(f.distance)} logged. " else "") +
                        "Factory figure: < 2 L/100 km. A sudden rise often means a dirty air filter or a dragging brake.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp)
                )
            }
            if (s.fuel.isEmpty()) item { EmptyState(Icons.Filled.LocalGasStation, "No fill-ups yet", "Tap Fill-up at the pump: pick the amount, and the litres are worked out for you.") }
            items(s.fuel, key = { it.id }) { e ->
                Panel(onClick = { editing = e }, modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.LocalGasStation, MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text((e.cost?.let { formatMad(it) + " · " } ?: "") + "%.2f L".format(e.liters), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            val perLiter = e.cost?.takeIf { e.liters > 0 }?.let { " · %.2f MAD/L".format(it / e.liters) } ?: ""
                            Text("${formatKm(e.km)} · ${Dates.display(e.date)}$perLiter" + if (e.full) " · full" else "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    editing?.let { e -> FuelDialog(e, f.lastPricePerLiter, onSave = { vm.saveFuel(it); editing = null }, onDelete = { vm.deleteFuel(e); editing = null }, onDismiss = { editing = null }) }
}

private val FILL_AMOUNTS = listOf(20, 30, 50, 100)

/** Locale-independent "15.18" / "2" for text fields (a French locale would print "15,18"). */
private fun decimalText(v: Double, scale: Int = 2): String = BigDecimal(v).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/**
 * Most fill-ups are a fixed amount ("30 dh") rather than a full tank, so the form starts from
 * what the pump shows: amount paid × price per litre → litres. Any two give the third. Mirrors
 * fuelDialog in web/js/views-garage.js.
 */
@Composable
private fun FuelDialog(e: FuelEntity, defaultPrice: Double?, onSave: (FuelEntity) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
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

    fun litersFromCost() {
        val c = cost.toDoubleOrNull() ?: return
        val p = price.toDoubleOrNull() ?: return
        // 4 decimals so cost ÷ litres gives back the exact price next time; at 2 decimals
        // 30 MAD @ 15.18 → 1.98 L → 15.15, and the remembered price would drift down.
        if (c > 0 && p > 0) liters = decimalText(c / p, scale = 4)
    }
    fun costFromLiters() {
        val l = liters.toDoubleOrNull() ?: return
        val p = price.toDoubleOrNull() ?: return
        if (l > 0 && p > 0) cost = decimalText(l * p)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (e.id == 0L) "New fill-up" else "Edit fill-up") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(date, { date = it }, "Date")
                NumberField(km, { km = it }, "Odometer", suffix = "km")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FILL_AMOUNTS.forEach { a ->
                        FilterChip(
                            selected = cost.toDoubleOrNull() == a.toDouble(),
                            onClick = { cost = a.toString(); litersFromCost() },
                            label = { Text("$a MAD") }
                        )
                    }
                }
                NumberField(cost, { cost = it; litersFromCost() }, "Paid", decimal = true, suffix = "MAD")
                NumberField(price, {
                    price = it
                    if ((cost.toDoubleOrNull() ?: 0.0) > 0) litersFromCost() else costFromLiters()
                }, "Price per litre", decimal = true, suffix = "MAD/L")
                OutlinedButton(
                    enabled = !lookingUp,
                    onClick = {
                        lookingUp = true
                        priceNote = "Looking up…"
                        scope.launch {
                            FuelPriceClient.fetch()
                                .onSuccess { p ->
                                    price = decimalText(p.pricePerLiter)
                                    if ((cost.toDoubleOrNull() ?: 0.0) > 0) litersFromCost() else costFromLiters()
                                    priceNote = "National average ${decimalText(p.pricePerLiter)} MAD/L · ${Dates.fromIso(p.asOf)?.let(Dates::display) ?: p.asOf} · ${p.source}. Your station may differ."
                                }
                                .onFailure { priceNote = "${it.message ?: "No connection"} — type the pump price." }
                            lookingUp = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CloudDownload, null)
                    Text("Today's price", Modifier.padding(start = 8.dp))
                }
                priceNote?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                NumberField(liters, { liters = it; costFromLiters() }, "Litres", decimal = true, suffix = "L")
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(full, { full = it }); Text("Full tank (pump clicked off)") }
                Text(
                    "Consumption is measured between two full tanks; top-ups in between are counted.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                enabled = km.toIntOrNull() != null && (liters.toDoubleOrNull() ?: 0.0) > 0 && date != null,
                onClick = { onSave(e.copy(date = date!!, km = km.toInt(), liters = liters.toDouble(), cost = cost.toDoubleOrNull(), full = full)) }
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (e.id != 0L) TextButton(onClick = onDelete) { Text("Delete", color = LocalStatusColors.current.due) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

// ------------------------------------------------------------------------------ Wallet

@Composable
fun WalletScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    val content = vm.content
    var editing by remember { mutableStateOf<DocumentEntity?>(null) }
    val colors = LocalStatusColors.current

    Scaffold(
        topBar = { BackTopBar("Documents wallet", onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = DocumentEntity(type = "insurance", title = content.documentType("insurance").label) },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add document") }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                Callout(
                    "Keep these under the seat",
                    "Insurance certificate + carte grise must be on the bike. Riding with an expired licence or no certificate voids your insurance cover."
                )
            }
            if (s.documents.isEmpty()) item { EmptyState(Icons.Filled.Badge, "No documents yet", "Add your insurance, carte grise and licence to get expiry reminders.") }
            items(s.documents, key = { it.id }) { d ->
                val type = content.documentType(d.type)
                val days = d.expiry?.let { Dates.daysBetween(s.today, it) }
                val color = when {
                    days == null -> colors.muted
                    days < 0 -> colors.due
                    days <= type.remindDays -> colors.soon
                    else -> colors.ok
                }
                Panel(onClick = { editing = d }, modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Badge, color)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(d.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(listOf(type.fr, d.number).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            d.expiry?.let { Text("Expires ${Dates.display(it)}", style = MaterialTheme.typography.bodySmall) }
                        }
                        if (days != null) StatusPill(if (days < 0) "Expired" else "$days d", color)
                    }
                }
            }
        }
    }

    editing?.let { d ->
        var type by remember(d) { mutableStateOf(d.type) }
        var title by remember(d) { mutableStateOf(d.title) }
        var number by remember(d) { mutableStateOf(d.number) }
        var expiry by remember(d) { mutableStateOf(d.expiry) }
        var notes by remember(d) { mutableStateOf(d.notes) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (d.id == 0L) "Add document" else "Edit document") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(content.documentTypes) { t ->
                            FilterChip(selected = type == t.id, onClick = {
                                if (title.isBlank() || content.documentTypes.any { it.label == title }) title = t.label
                                type = t.id
                            }, label = { Text(t.label) })
                        }
                    }
                    content.documentType(type).hint.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    OutlinedTextField(title, { title = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(number, { number = it }, label = { Text("Number / policy") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    DateField(expiry, { expiry = it }, "Expiry date", clearable = true)
                    OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(enabled = title.isNotBlank(), onClick = {
                    vm.saveDocument(d.copy(type = type, title = title.trim(), number = number.trim(), expiry = expiry, notes = notes.trim()))
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (d.id != 0L) IconButton(onClick = { vm.deleteDocument(d); editing = null }) { Icon(Icons.Filled.Delete, "Delete", tint = colors.due) }
                    TextButton(onClick = { editing = null }) { Text("Cancel") }
                }
            }
        )
    }
}

// ------------------------------------------------------------------------------ Profile

