package io.github.achmalearning.scootercompanion.ui.screens.bookmarks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.achmalearning.scootercompanion.data.model.PdfSource
import io.github.achmalearning.scootercompanion.ui.common.appViewModel

@Composable
fun BookmarksScreen(onOpen: (PdfSource, Int) -> Unit) {
    val vm = appViewModel { BookmarksViewModel(it) }
    val bookmarks by vm.bookmarks.collectAsState()

    LazyColumn(Modifier.fillMaxWidth().padding(16.dp)) {
        items(bookmarks, key = { it.id }) { bm ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = { onOpen(bm.pdf, bm.page) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(bm.title, style = MaterialTheme.typography.titleSmall)
                        Text("${bm.pdf.displayName} — page ${bm.page}", style = MaterialTheme.typography.bodySmall)
                        bm.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                    IconButton(onClick = { vm.delete(bm) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete bookmark")
                    }
                }
            }
        }
    }
}
