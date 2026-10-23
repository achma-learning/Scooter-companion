package io.github.achmalearning.scootercompanion.ui.screens.garage

import io.github.achmalearning.scootercompanion.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import io.github.achmalearning.scootercompanion.ui.components.formatMoney
import io.github.achmalearning.scootercompanion.ui.theme.LocalStatusColors

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
        topBar = { BackTopBar(stringResource(R.string.more_wallet), onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = DocumentEntity(type = "insurance", title = content.documentType("insurance").label) },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.wal_add)) }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                Callout(
                    stringResource(R.string.wal_keep_title),
                    stringResource(R.string.wal_keep_text)
                )
            }
            if (s.documents.isEmpty()) item { EmptyState(Icons.Filled.Badge, stringResource(R.string.wal_empty_title), stringResource(R.string.wal_empty_text)) }
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
                            d.expiry?.let { Text(stringResource(R.string.wal_expires, Dates.display(it)), style = MaterialTheme.typography.bodySmall) }
                        }
                        if (days != null) StatusPill(if (days < 0) stringResource(R.string.wal_expired) else stringResource(R.string.wal_days, days), color)
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
            title = { Text(stringResource(if (d.id == 0L) R.string.wal_add else R.string.wal_edit)) },
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
                    OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.wal_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(number, { number = it }, label = { Text(stringResource(R.string.wal_number)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    DateField(expiry, { expiry = it }, stringResource(R.string.wal_expiry), clearable = true)
                    OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.service_notes)) }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(enabled = title.isNotBlank(), onClick = {
                    vm.saveDocument(d.copy(type = type, title = title.trim(), number = number.trim(), expiry = expiry, notes = notes.trim()))
                    editing = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                Row {
                    if (d.id != 0L) IconButton(onClick = { vm.deleteDocument(d); editing = null }) { Icon(Icons.Filled.Delete, stringResource(R.string.action_delete), tint = colors.due) }
                    TextButton(onClick = { editing = null }) { Text(stringResource(R.string.action_cancel)) }
                }
            }
        )
    }
}
