package com.example.kymcoagility50.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kymcoagility50.data.model.PdfSource
import com.example.kymcoagility50.privacy.AppMode
import com.example.kymcoagility50.ui.common.appViewModel
import com.example.kymcoagility50.ui.theme.PrivateGreen

@Composable
fun HomeScreen(
    onOpenManual: (Int) -> Unit,
    onOpenPassport: (Int) -> Unit,
    onOpenContents: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenMaintenance: () -> Unit
) {
    val vm = appViewModel { HomeViewModel(it) }
    val state by vm.uiState.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        item {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Kymco Agility 50", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                if (state.mode == AppMode.PRIVATE) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Private Mode") },
                        leadingIcon = { Icon(Icons.Filled.Shield, contentDescription = null, tint = PrivateGreen) }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Always perform the pre-ride inspection before every ride.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
        }

        item {
            QuickActionCard(
                icon = Icons.Filled.MenuBook,
                title = "Continue reading — Driver's Manual",
                subtitle = state.lastManualPage?.let { "Page $it" } ?: "Start from page 1",
                onClick = { onOpenManual(state.lastManualPage ?: 1) }
            )
            Spacer(Modifier.height(8.dp))
            QuickActionCard(
                icon = Icons.Filled.MenuBook,
                title = "Continue reading — Passeport",
                subtitle = state.lastPassportPage?.let { "Page $it" } ?: "Start from page 1",
                onClick = { onOpenPassport(state.lastPassportPage ?: 1) }
            )
            Spacer(Modifier.height(8.dp))
            QuickActionCard(Icons.Filled.MenuBook, "Table of Contents", "Both documents", onOpenContents)
            Spacer(Modifier.height(8.dp))
            QuickActionCard(Icons.Filled.Search, "Search", "Full text, both documents", onOpenSearch)
            Spacer(Modifier.height(8.dp))
            QuickActionCard(Icons.Filled.Bookmark, "Bookmarks", "Saved pages", onOpenBookmarks)
            Spacer(Modifier.height(8.dp))
            QuickActionCard(Icons.Filled.Build, "Maintenance logbook", "Date / km / notes", onOpenMaintenance)
            Spacer(Modifier.height(16.dp))
        }

        if (state.recent.isNotEmpty()) {
            item { Text("Recently viewed", style = MaterialTheme.typography.titleSmall) }
            items(state.recent) { (pdf, page) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { if (pdf == PdfSource.MANUAL) onOpenManual(page) else onOpenPassport(page) }
                ) {
                    Text(
                        "${pdf.displayName} — page $page",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick, colors = CardDefaults.cardColors()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
