package io.github.achmalearning.scootercompanion.ui.screens.service

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.achmalearning.scootercompanion.data.db.MaintenanceEntryEntity
import io.github.achmalearning.scootercompanion.data.db.taskIds
import io.github.achmalearning.scootercompanion.domain.Dates
import io.github.achmalearning.scootercompanion.domain.DueStatus
import io.github.achmalearning.scootercompanion.domain.StampState
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import io.github.achmalearning.scootercompanion.ui.components.BackTopBar
import io.github.achmalearning.scootercompanion.ui.components.Callout
import io.github.achmalearning.scootercompanion.ui.components.DateField
import io.github.achmalearning.scootercompanion.ui.components.EmptyState
import io.github.achmalearning.scootercompanion.ui.components.InfoRow
import io.github.achmalearning.scootercompanion.ui.components.NavRow
import io.github.achmalearning.scootercompanion.ui.components.NumberField
import io.github.achmalearning.scootercompanion.ui.components.Panel
import io.github.achmalearning.scootercompanion.ui.components.ScreenPadding
import io.github.achmalearning.scootercompanion.ui.components.SectionTitle
import io.github.achmalearning.scootercompanion.ui.components.StatusBar
import io.github.achmalearning.scootercompanion.ui.components.StatusPill
import io.github.achmalearning.scootercompanion.ui.components.VSpace
import io.github.achmalearning.scootercompanion.ui.components.dueText
import io.github.achmalearning.scootercompanion.ui.components.formatKm
import io.github.achmalearning.scootercompanion.ui.components.Money
import io.github.achmalearning.scootercompanion.ui.components.formatMoney
import io.github.achmalearning.scootercompanion.ui.components.statusColor
import io.github.achmalearning.scootercompanion.ui.components.statusLabel
import io.github.achmalearning.scootercompanion.ui.screens.garage.GarageViewModel
import io.github.achmalearning.scootercompanion.ui.screens.garage.TaskRow
import io.github.achmalearning.scootercompanion.ui.theme.LocalStatusColors

/** "Service" tab: what's due (grouped by status) and the logbook history. */
@Composable
fun ServiceScreen(openTask: (String) -> Unit, logService: (entryId: Long?) -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    var tab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { logService(null) }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Log service") })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Due") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("History (${s.entries.size})") })
            }
            if (tab == 0) {
                LazyColumn(contentPadding = ScreenPadding) {
                    item {
                        Text(
                            "Intervals tuned for Marrakech heat & dust. At ${formatKm(s.currentKm)}.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    for (status in DueStatus.entries) {
                        val group = s.statuses.filter { it.status == status }
                        if (group.isEmpty()) continue
                        item(key = "h$status") {
                            SectionTitle(when (status) { DueStatus.OVERDUE -> "Due now"; DueStatus.SOON -> "Coming up"; DueStatus.OK -> "On schedule" } + " · ${group.size}")
                        }
                        items(group, key = { it.task.id }) { TaskRow(it) { openTask(it.task.id) } }
                    }
                }
            } else {
                LazyColumn(contentPadding = ScreenPadding) {
                    if (s.entries.isEmpty()) item {
                        EmptyState(Icons.Filled.Build, "No services logged yet", "Log oil changes and dealer visits to keep reminders accurate and your passport complete.")
                    }
                    items(s.entries, key = { it.id }) { entry ->
                        ServiceHistoryRow(entry, vm, onClick = { logService(entry.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceHistoryRow(entry: MaintenanceEntryEntity, vm: GarageViewModel, onClick: () -> Unit) {
    val names = entry.taskIds.mapNotNull { vm.content.task(it)?.name }
    Panel(onClick = onClick, modifier = Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatKm(entry.km), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("  ·  " + Dates.display(Dates.startOfDay(entry.date)), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                if (entry.stamped) StatusPill("Stamped", MaterialTheme.colorScheme.primary)
            }
            if (names.isNotEmpty()) Text(names.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            val meta = listOfNotNull(
                entry.milestoneKm?.let { "Passport ${formatKm(it)}" },
                entry.shop.takeIf { it.isNotBlank() },
                entry.cost?.let(::formatMoney)
            )
            if (meta.isNotEmpty()) Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (entry.note.isNotBlank()) Text(entry.note, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun TaskDetailScreen(taskId: String, onBack: () -> Unit, openGuide: (String) -> Unit, logService: (String) -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    val st = s.statuses.firstOrNull { it.task.id == taskId } ?: return
    val task = st.task
    val color = statusColor(st.status)
    var confirm by remember { mutableStateOf(false) }

    Scaffold(topBar = { BackTopBar(task.name, onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                Panel {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(task.fr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            StatusPill(statusLabel(st.status), color)
                        }
                        VSpace(10.dp)
                        Text(dueText(st.kmLeft, st.daysLeft), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
                        VSpace(8.dp)
                        StatusBar(st.progress, color)
                        VSpace(12.dp)
                        st.dueKm?.let { InfoRow("Due at", formatKm(it)) }
                        st.dueDate?.let { InfoRow("Due by", Dates.display(it)) }
                        InfoRow("Last done", if (st.doneBefore) listOfNotNull(st.lastKm?.let(::formatKm), st.lastDate?.let(Dates::display)).joinToString(" · ") else "Not logged yet")
                        InfoRow("Marrakech interval", task.label)
                        InfoRow("Factory interval", task.factory)
                        if (task.cost.isNotBlank()) InfoRow("Typical cost", task.cost)
                    }
                }
            }
            item {
                VSpace(12.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { confirm = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Check, null); Text(" Done today")
                    }
                    OutlinedButton(onClick = { logService(task.id) }, modifier = Modifier.weight(1f)) { Text("Log with details") }
                }
            }
            item { Callout("Why & how", task.notes) }
            task.guide?.let { g ->
                vm.content.guide(g)?.let { guide ->
                    item { NavRow(Icons.AutoMirrored.Filled.MenuBook, "Step-by-step: ${guide.title}", "${guide.time} · ${guide.difficulty}") { openGuide(g) } }
                }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Mark as done?") },
            text = { Text("Logs \"${task.name}\" today at ${formatKm(s.currentKm)}. Update the odometer first if it changed.") },
            confirmButton = { Button(onClick = { vm.markTaskDone(task.id); confirm = false }) { Text("Log it") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
        )
    }
}

/**
 * Add/edit a service record. [entryId] edits an existing one; [presetTask] / [presetMilestone]
 * pre-fill a new one from a task page or a passport stamp.
 */
@Composable
fun LogServiceScreen(entryId: Long?, presetTask: String?, presetMilestone: Int?, onDone: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { GarageViewModel(context, it) }
    val s = vm.state.collectAsStateWithLifecycle().value ?: return
    val content = vm.content
    val existing = entryId?.let { id -> s.entries.firstOrNull { it.id == id } }
    val presetMs = presetMilestone?.let { km -> content.milestones.firstOrNull { it.km == km } }

    var date by remember { mutableStateOf<Long?>(existing?.date?.let(Dates::startOfDay) ?: Dates.today()) }
    var km by remember { mutableStateOf((existing?.km ?: s.currentKm).toString()) }
    var milestone by remember { mutableStateOf(existing?.milestoneKm ?: presetMs?.km) }
    val selected = remember {
        mutableStateListOf<String>().apply {
            addAll(existing?.taskIds ?: presetMs?.tasks ?: listOfNotNull(presetTask))
        }
    }
    var shop by remember { mutableStateOf(existing?.shop ?: "") }
    var cost by remember { mutableStateOf(existing?.cost?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var stamped by remember { mutableStateOf(existing?.stamped ?: (presetMs != null)) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    val candidates = s.milestones.filter { it.state != StampState.UPCOMING || it.milestone.km <= s.currentKm + 2500 }.takeLast(8)

    Scaffold(topBar = {
        BackTopBar(if (existing != null) "Edit service" else "Log service", onDone) {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete") }
        }
    }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = ScreenPadding) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField(date, { date = it }, "Date", Modifier.weight(1f))
                    NumberField(km, { km = it }, "Odometer", Modifier.weight(1f), suffix = "km")
                }
            }
            item {
                SectionTitle("Passport milestone")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = milestone == null, onClick = { milestone = null }, label = { Text("None") }) }
                    items(candidates, key = { it.milestone.km }) { m ->
                        FilterChip(
                            selected = milestone == m.milestone.km,
                            onClick = {
                                milestone = m.milestone.km
                                m.milestone.tasks.forEach { if (it !in selected) selected.add(it) }
                            },
                            label = { Text(formatKm(m.milestone.km)) },
                            leadingIcon = if (m.state == StampState.STAMPED) ({ Icon(Icons.Filled.Verified, null) }) else null
                        )
                    }
                }
                milestone?.let { km -> content.milestones.firstOrNull { it.km == km }?.let { Text(it.actions, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) } }
            }
            item { SectionTitle("Work done (${selected.size})") }
            for (cat in content.categories) {
                val tasks = content.tasks.filter { it.cat == cat.id }
                item(key = "cat${cat.id}") {
                    Text(cat.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tasks.forEach { t ->
                            FilterChip(
                                selected = t.id in selected,
                                onClick = { if (t.id in selected) selected.remove(t.id) else selected.add(t.id) },
                                label = { Text(t.name) }
                            )
                        }
                    }
                }
            }
            item {
                SectionTitle("Where & cost")
                OutlinedTextField(shop, { shop = it }, label = { Text("Workshop / dealer") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    items(content.shops.map { it.name } + "DIY") { name ->
                        FilterChip(selected = shop == name, onClick = { shop = name }, label = { Text(name) })
                    }
                }
                VSpace(8.dp)
                NumberField(cost, { cost = it }, "Cost", decimal = true, suffix = Money.symbol)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Stamped in the paper passport", style = MaterialTheme.typography.bodyLarge)
                        Text("Approved-dealer stamps keep the warranty valid.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(stamped, { stamped = it })
                }
                OutlinedTextField(note, { note = it }, label = { Text("Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            item {
                VSpace(16.dp)
                val kmValue = km.toIntOrNull()
                Button(
                    enabled = kmValue != null && date != null && (selected.isNotEmpty() || note.isNotBlank()),
                    onClick = {
                        val base = existing ?: MaintenanceEntryEntity(km = 0, date = 0, note = "", updatedAt = 0)
                        vm.saveService(
                            base.copy(
                                km = kmValue!!, date = date!!, note = note.trim(), tasks = selected.joinToString(","),
                                milestoneKm = milestone, shop = shop.trim(), cost = cost.toDoubleOrNull(), stamped = stamped
                            )
                        )
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (existing != null) "Save changes" else "Save service") }
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this service?") },
            text = { Text("${formatKm(existing.km)} · ${Dates.display(Dates.startOfDay(existing.date))}") },
            confirmButton = {
                TextButton(onClick = { vm.deleteService(existing); confirmDelete = false; onDone() }) {
                    Text("Delete", color = LocalStatusColors.current.due)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
