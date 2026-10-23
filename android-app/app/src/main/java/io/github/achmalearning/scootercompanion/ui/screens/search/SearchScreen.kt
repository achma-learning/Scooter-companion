package io.github.achmalearning.scootercompanion.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.achmalearning.scootercompanion.data.model.PdfSource
import io.github.achmalearning.scootercompanion.ui.common.appViewModel

@Composable
fun SearchScreen(onOpenResult: (PdfSource, Int) -> Unit) {
    val vm = appViewModel { SearchViewModel(it) }
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            label = { Text("Search the manual and passeport") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            FilterChip(selected = state.filter == null, onClick = { vm.setFilter(null) }, label = { Text("All") })
            FilterChip(
                selected = state.filter == PdfSource.MANUAL,
                onClick = { vm.setFilter(PdfSource.MANUAL) },
                label = { Text("Manual") },
                modifier = Modifier.padding(start = 8.dp)
            )
            FilterChip(
                selected = state.filter == PdfSource.PASSPORT,
                onClick = { vm.setFilter(PdfSource.PASSPORT) },
                label = { Text("Passeport") },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (state.searching) CircularProgressIndicator(Modifier.padding(8.dp))
        LazyColumn {
            items(state.results) { hit ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { onOpenResult(hit.pdf, hit.page) }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${hit.pdf.displayName} — page ${hit.page}", style = MaterialTheme.typography.titleSmall)
                        Text(hit.snippet, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
