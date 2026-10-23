package io.github.achmalearning.scootercompanion.ui.screens.garage

import io.github.achmalearning.scootercompanion.data.db.ExpenseEntity
import io.github.achmalearning.scootercompanion.data.repository.GarageSnapshot
import io.github.achmalearning.scootercompanion.domain.SpendCategory
import io.github.achmalearning.scootercompanion.ui.components.Money
import io.github.achmalearning.scootercompanion.ui.components.formatMoney
import io.github.achmalearning.scootercompanion.ui.screens.money.ExpenseDialog
import io.github.achmalearning.scootercompanion.ui.screens.money.coverageLine
import io.github.achmalearning.scootercompanion.ui.screens.money.newExpense
import io.github.achmalearning.scootercompanion.ui.screens.profile.ScooterPicture
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.res.stringResource
import io.github.achmalearning.scootercompanion.R
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.DueStatus
import io.github.achmalearning.scootercompanion.domain.StampState
import io.github.achmalearning.scootercompanion.domain.TaskStatus
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import io.github.achmalearning.scootercompanion.ui.components.Callout
import io.github.achmalearning.scootercompanion.ui.components.HealthRing
import io.github.achmalearning.scootercompanion.ui.components.IconBadge
import io.github.achmalearning.scootercompanion.ui.components.NumberField
import io.github.achmalearning.scootercompanion.ui.components.Panel
import io.github.achmalearning.scootercompanion.ui.components.ScooterArt
import io.github.achmalearning.scootercompanion.ui.components.ScreenPadding
import io.github.achmalearning.scootercompanion.ui.components.SectionTitle
import io.github.achmalearning.scootercompanion.ui.components.StatusBar
import io.github.achmalearning.scootercompanion.ui.components.StatusPill
import io.github.achmalearning.scootercompanion.ui.components.VSpace
import io.github.achmalearning.scootercompanion.ui.components.dueText
import io.github.achmalearning.scootercompanion.ui.components.formatKm
import io.github.achmalearning.scootercompanion.ui.components.statusColor
import io.github.achmalearning.scootercompanion.ui.components.statusLabel
import io.github.achmalearning.scootercompanion.ui.theme.LocalStatusColors

data class GarageNav(
    val openTask: (String) -> Unit,
    val logService: () -> Unit,
    val openService: () -> Unit,
    val openPassport: () -> Unit,
    val openFuel: () -> Unit,
    val openWallet: () -> Unit,
    val openChecklist: () -> Unit,
    val openTroubleshoot: () -> Unit,
    val openSos: () -> Unit,
    val openProfile: () -> Unit,
    val openSeasons: () -> Unit,
    val openExpenses: () -> Unit = {},
    val openInsurance: () -> Unit = {},
    val openStats: () -> Unit = {}
)

@Composable
fun GarageScreen(nav: GarageNav) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    val content = vm.content
    var showKm by remember { mutableStateOf(false) }
    var addExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    val status = LocalStatusColors.current
    val name = s.profile.nickname.ifBlank { listOf(s.profile.brand, s.profile.model).filter { it.isNotBlank() }.joinToString(" ") }
        .ifBlank { stringResource(R.string.home_my_scooter) }

    LazyColumn(contentPadding = ScreenPadding) {
        item {
            // Hero card: the scooter, its photo, odometer and maintenance health.
            Box(
                Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(24.dp)).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.surface))
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            val sub = listOf(
                                if (s.profile.nickname.isNotBlank()) listOf(s.profile.brand, s.profile.model).filter { it.isNotBlank() }.joinToString(" ") else "",
                                s.profile.plate
                            ).filter { it.isNotBlank() }.joinToString(" · ")
                            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HealthRing(s.health, size = 72.dp)
                    }
                    ScooterPicture(s.profile.photo, Modifier.fillMaxWidth().height(140.dp).padding(vertical = 6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.odometer), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatKm(s.currentKm), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(onClick = { showKm = true }) {
                            Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  " + stringResource(R.string.home_update_km))
                        }
                    }
                }
            }
        }

        if (!s.hasProfile || s.profile.brand.isBlank()) {
            item {
                VSpace(12.dp)
                Panel(onClick = nav.openProfile) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Edit, MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(stringResource(R.string.home_setup), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.home_setup_text), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item { MoneyCard(s, onClick = nav.openStats, openInsurance = nav.openInsurance) }

        item {
            SectionTitle(stringResource(R.string.home_quick))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
                val preRideToday = s.lastPreRide == s.today
                if (s.spec.isElectric) {
                    QuickAction(Icons.Filled.ElectricBolt, stringResource(R.string.home_charging), Modifier.weight(1f)) {
                        addExpense = newExpense(SpendCategory.CHARGING, s.currentKm)
                    }
                } else {
                    QuickAction(Icons.Filled.LocalGasStation, stringResource(R.string.fuel_add), Modifier.weight(1f), nav.openFuel)
                }
                QuickAction(Icons.Filled.Build, stringResource(R.string.nav_service), Modifier.weight(1f), nav.logService)
                QuickAction(Icons.Filled.Receipt, stringResource(R.string.home_expense), Modifier.weight(1f)) {
                    addExpense = newExpense(SpendCategory.ACCESSORY, s.currentKm)
                }
                QuickAction(Icons.Filled.Shield, stringResource(R.string.ins_title), Modifier.weight(1f), nav.openInsurance)
                QuickAction(Icons.Filled.ChecklistRtl, stringResource(if (preRideToday) R.string.home_checked else R.string.home_preride), Modifier.weight(1f), nav.openChecklist)
                if (s.isAgility) QuickAction(Icons.Filled.Verified, stringResource(R.string.home_passport), Modifier.weight(1f), nav.openPassport)
                else QuickAction(Icons.Filled.Sos, "SOS", Modifier.weight(1f), nav.openSos)
            }
        }

        val upcoming = s.statuses.filter { it.status != DueStatus.OK }.take(5).ifEmpty { s.statuses.take(3) }
        item {
            SectionTitle(stringResource(R.string.home_next_up)) { TextButton(onClick = nav.openService) { Text(stringResource(R.string.home_all_services)) } }
        }
        items(upcoming, key = { it.task.id }) { st -> TaskRow(st, onClick = { nav.openTask(st.task.id) }) }

        // Passport: next stamp (Agility only; other scooters have no milestones)
        val next = s.milestones.firstOrNull { it.state == StampState.NEXT }
        if (next != null) {
            item {
                SectionTitle(stringResource(R.string.home_passport))
                Panel(onClick = nav.openPassport) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Verified, MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(stringResource(R.string.home_next_stamp, formatKm(next.milestone.km)), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(next.milestone.actions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                    }
                }
            }
        }

        // Wallet alerts
        val alerts = s.documents.mapNotNull { d ->
            d.expiry?.let { exp ->
                val days = Dates.daysBetween(s.today, exp)
                if (days <= content.documentType(d.type).remindDays) d to days else null
            }
        }
        if (alerts.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.home_documents)) }
            items(alerts, key = { "doc${it.first.id}" }) { (doc, days) ->
                Panel(onClick = nav.openWallet, modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Badge, if (days < 0) status.due else status.soon)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(doc.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (days < 0) stringResource(R.string.home_doc_expired, -days) else stringResource(R.string.home_doc_expires, days, Dates.display(doc.expiry!!)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Season tip and warnings come from the Agility / Marrakech knowledge base.
        if (s.isAgility) {
            content.seasonForMonth(Dates.month(s.today))?.let { season ->
                item {
                    SectionTitle(stringResource(R.string.home_season))
                    Panel(onClick = nav.openSeasons) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.WbSunny, status.soon)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("${season.name} · ${season.range}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(season.headline + " " + season.rows.first().let { "${it[0]}: ${it[1]}" }, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            item {
                val w = content.warnings[(Dates.daysBetween(0, s.today) % content.warnings.size + content.warnings.size) % content.warnings.size]
                VSpace(12.dp)
                Callout(stringResource(R.string.home_tip, w.title), w.text)
            }
        }
    }

    if (showKm) {
        var km by remember { mutableStateOf(s.currentKm.toString()) }
        AlertDialog(
            onDismissRequest = { showKm = false },
            title = { Text(stringResource(R.string.home_km_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.home_km_text), style = MaterialTheme.typography.bodySmall)
                    VSpace(8.dp)
                    NumberField(km, { km = it }, stringResource(R.string.home_km_field), suffix = "km")
                    val value = km.toIntOrNull()
                    if (value != null && value < s.currentKm) {
                        Text(stringResource(R.string.home_km_lower, formatKm(s.currentKm)), color = status.due, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(enabled = (km.toIntOrNull() ?: -1) >= s.currentKm, onClick = { vm.updateKm(km.toInt()); showKm = false }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { showKm = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    addExpense?.let { x ->
        ExpenseDialog(x, s.region, onSave = { vm.saveExpense(it); addExpense = null }, onDelete = { addExpense = null }, onDismiss = { addExpense = null })
    }
}

/** This month against the target, the insurance line, and the long-run averages. */
@Composable
private fun MoneyCard(s: GarageSnapshot, onClick: () -> Unit, openInsurance: () -> Unit) {
    val l = s.ledger
    val status = LocalStatusColors.current
    VSpace(12.dp)
    Panel(onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.home_this_month), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatMoney(l.thisMonth), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                l.budget?.let { b ->
                    Text(
                        "  " + stringResource(R.string.home_of_target, formatMoney(b.target)),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            val b = l.budget
            if (b != null) {
                val color = when { b.over -> status.due; b.projectedOver -> status.soon; else -> status.ok }
                VSpace(6.dp)
                StatusBar(b.fraction.toFloat(), color)
                VSpace(4.dp)
                Text(
                    if (b.over) stringResource(R.string.home_over, formatMoney(-b.remaining)) else stringResource(R.string.home_left, formatMoney(b.remaining)),
                    style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold
                )
                if (!b.over && b.projectedOver) {
                    Text(stringResource(R.string.home_projected, formatMoney(b.projected)), style = MaterialTheme.typography.bodySmall, color = status.soon)
                }
            } else {
                Text(stringResource(R.string.home_no_target), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            VSpace(10.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat(formatMoney(l.avgPerMonth), stringResource(R.string.home_avg_month), Modifier.weight(1f))
                MiniStat(formatMoney(l.total), stringResource(R.string.home_total_with_purchase), Modifier.weight(1f))
                MiniStat(l.costPerKm?.let { "%.2f".format(java.util.Locale.US, it) } ?: "—", stringResource(R.string.home_per_km, Money.symbol), Modifier.weight(1f))
            }
            VSpace(10.dp)
            val (text, color) = coverageLine(s.coverage.state, s.coverage.daysLeft)
            TextButton(onClick = openInsurance, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Icon(Icons.Filled.Shield, null, tint = color, modifier = Modifier.size(18.dp))
                Text("  $text", color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Panel(onClick = onClick, modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(icon, MaterialTheme.colorScheme.primary, 36.dp)
            VSpace(6.dp)
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun TaskRow(st: TaskStatus, onClick: () -> Unit) {
    val color = statusColor(st.status)
    Panel(onClick = onClick, modifier = Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(st.task.localName(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StatusPill(statusLabel(st.status), color)
            }
            VSpace(6.dp)
            StatusBar(st.progress, color)
            VSpace(6.dp)
            Row {
                Text(dueText(st.kmLeft, st.daysLeft), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                st.dueKm?.let {
                    Text(stringResource(R.string.due_at_km, formatKm(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}
