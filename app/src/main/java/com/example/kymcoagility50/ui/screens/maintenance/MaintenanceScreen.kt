package com.example.kymcoagility50.ui.screens.maintenance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.kymcoagility50.privacy.AppMode
import com.example.kymcoagility50.ui.common.appViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MaintenanceScreen() {
    val context = LocalContext.current
    val vm = appViewModel { MaintenanceViewModel(context, it) }
    val state by vm.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, contentDescription = "Add entry") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Maintenance logbook", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                if (state.mode == AppMode.SYNC) {
                    AssistChip(
                        onClick = { vm.syncNow() },
                        label = { Text("Sync now") },
                        leadingIcon = { Icon(Icons.Filled.Sync, contentDescription = null) }
                    )
                }
            }
            Text(
                if (state.mode == AppMode.SYNC) "Entries sync to your account." else "Private Mode — entries stay on this device only.",
                style = MaterialTheme.typography.bodySmall
            )

            LazyColumn(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                items(state.entries, key = { it.id }) { entry ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${entry.km} km — ${dateFormat.format(Date(entry.date))}", style = MaterialTheme.typography.titleSmall)
                                if (entry.note.isNotBlank()) Text(entry.note, style = MaterialTheme.typography.bodySmall)
                                if (state.mode == AppMode.SYNC) {
                                    Text(
                                        if (entry.dirty) "Pending sync…" else "Synced",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            IconButton(onClick = { vm.deleteEntry(entry) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddEntryDialog(
            onDismiss = { showAdd = false },
            onAdd = { km, note -> vm.addEntry(km, System.currentTimeMillis(), note); showAdd = false }
        )
    }
}

@Composable
private fun AddEntryDialog(onDismiss: () -> Unit, onAdd: (Int, String) -> Unit) {
    var km by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New maintenance entry") },
        text = {
            Column {
                OutlinedTextField(value = km, onValueChange = { km = it.filter(Char::isDigit) }, label = { Text("Kilometrage") })
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Notes") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(km.toIntOrNull() ?: 0, note) }, enabled = km.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
