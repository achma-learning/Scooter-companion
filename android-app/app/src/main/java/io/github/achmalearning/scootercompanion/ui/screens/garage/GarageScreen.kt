package io.github.achmalearning.scootercompanion.ui.screens.garage

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
    val openSeasons: () -> Unit
)

@Composable
fun GarageScreen(nav: GarageNav) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    val content = vm.content
    var showKm by remember { mutableStateOf(false) }
    val status = LocalStatusColors.current

    LazyColumn(contentPadding = ScreenPadding) {
        item {
            // Hero card — bike identity, odometer, health.
            Box(
                Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(24.dp)).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.surface))
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.profile.nickname.ifBlank { "My Agility" },
                                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
                            )
                            Text(
                                content.bike.model + (if (s.profile.plate.isNotBlank()) " · ${s.profile.plate}" else ""),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HealthRing(s.health, size = 72.dp)
                    }
                    ScooterArt(Modifier.fillMaxWidth().height(130.dp).padding(vertical = 6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Odometer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatKm(s.currentKm), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(onClick = { showKm = true }) {
                            Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Update km")
                        }
                    }
                }
            }
        }

        if (!s.hasProfile) {
            item {
                VSpace(12.dp)
                Panel(onClick = nav.openProfile) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Edit, MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Set up your scooter", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Purchase date & km make every reminder accurate.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item {
            SectionTitle("Quick actions")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
                val preRideToday = s.lastPreRide == s.today
                QuickAction(Icons.Filled.Build, "Log service", Modifier.weight(1f), nav.logService)
                QuickAction(Icons.Filled.LocalGasStation, "Fuel", Modifier.weight(1f), nav.openFuel)
                QuickAction(Icons.Filled.ChecklistRtl, if (preRideToday) "Checked ✓" else "Pre-ride", Modifier.weight(1f), nav.openChecklist)
                QuickAction(Icons.Filled.Psychology, "Diagnose", Modifier.weight(1f), nav.openTroubleshoot)
                QuickAction(Icons.Filled.Verified, "Passport", Modifier.weight(1f), nav.openPassport)
                QuickAction(Icons.Filled.Sos, "SOS", Modifier.weight(1f), nav.openSos)
            }
        }

        val upcoming = s.statuses.filter { it.status != DueStatus.OK }.take(5).ifEmpty { s.statuses.take(3) }
        item {
            SectionTitle("Next up") { TextButton(onClick = nav.openService) { Text("All services") } }
        }
        items(upcoming, key = { it.task.id }) { st -> TaskRow(st, onClick = { nav.openTask(st.task.id) }) }

        // Passport: next stamp
        val next = s.milestones.firstOrNull { it.state == StampState.NEXT }
        if (next != null) {
            item {
                SectionTitle("Passport")
                Panel(onClick = nav.openPassport) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Verified, MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Next stamp: ${formatKm(next.milestone.km)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
            item { SectionTitle("Documents") }
            items(alerts, key = { "doc${it.first.id}" }) { (doc, days) ->
                Panel(onClick = nav.openWallet, modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Badge, if (days < 0) status.due else status.soon)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(doc.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(if (days < 0) "Expired ${-days} days ago" else "Expires in $days days · ${Dates.display(doc.expiry!!)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Season tip
        content.seasonForMonth(Dates.month(s.today))?.let { season ->
            item {
                SectionTitle("This season")
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
            Callout("Tip · ${w.title}", w.text)
        }
    }

    if (showKm) {
        var km by remember { mutableStateOf(s.currentKm.toString()) }
        AlertDialog(
            onDismissRequest = { showKm = false },
            title = { Text("Update odometer") },
            text = {
                Column {
                    Text("Reading on the dashboard today.", style = MaterialTheme.typography.bodySmall)
                    VSpace(8.dp)
                    NumberField(km, { km = it }, "Kilometres", suffix = "km")
                    val value = km.toIntOrNull()
                    if (value != null && value < s.currentKm) {
                        Text("Lower than the last known ${formatKm(s.currentKm)}.", color = status.due, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(enabled = (km.toIntOrNull() ?: -1) >= s.currentKm, onClick = { vm.updateKm(km.toInt()); showKm = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showKm = false }) { Text("Cancel") } }
        )
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
