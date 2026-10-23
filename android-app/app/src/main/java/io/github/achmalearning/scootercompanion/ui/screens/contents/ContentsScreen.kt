package io.github.achmalearning.scootercompanion.ui.screens.contents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.achmalearning.scootercompanion.data.model.PdfSource
import io.github.achmalearning.scootercompanion.data.model.TocItem
import io.github.achmalearning.scootercompanion.ui.common.appViewModel

@Composable
fun ContentsScreen(onOpenPage: (PdfSource, Int) -> Unit) {
    val vm = appViewModel { ContentsViewModel(it) }
    val toc by vm.toc.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Driver Manual") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Passeport") })
        }
        val entries = if (tab == 0) toc?.manual ?: emptyList() else toc?.passport ?: emptyList()
        val source = if (tab == 0) PdfSource.MANUAL else PdfSource.PASSPORT
        LazyColumn {
            items(entries) { entry -> TocRow(entry, depth = 0) { onOpenPage(source, it) } }
        }
    }
}

@Composable
private fun TocRow(item: TocItem, depth: Int, onOpen: (Int) -> Unit) {
    Column {
        Text(
            item.title,
            style = if (depth == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen(item.page) }
                .padding(start = (16 + depth * 16).dp, top = 10.dp, bottom = 10.dp, end = 16.dp)
        )
        item.children.forEach { TocRow(it, depth + 1, onOpen) }
    }
}
