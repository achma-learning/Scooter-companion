package io.github.achmalearning.scootercompanion.ui.screens.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Toc
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.data.content.DocInfo
import io.github.achmalearning.scootercompanion.data.model.PdfSource
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import io.github.achmalearning.scootercompanion.ui.components.NavRow
import io.github.achmalearning.scootercompanion.ui.components.Panel
import io.github.achmalearning.scootercompanion.ui.components.ScreenPadding
import io.github.achmalearning.scootercompanion.ui.components.SectionTitle
import io.github.achmalearning.scootercompanion.ui.components.VSpace
import io.github.achmalearning.scootercompanion.ui.theme.PrivateGreen

/** "Manual" tab: the two official documents, offline, with search and bookmarks. */
@Composable
fun ManualHubScreen(
    onOpen: (PdfSource, Int) -> Unit,
    onOpenContents: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookmarks: () -> Unit
) {
    val context = LocalContext.current
    val vm = appViewModel { HomeViewModel(it) }
    val state by vm.uiState.collectAsState()
    val docs = remember { AppContainer.getInstance(context).contentRepository.content.docs }

    LazyColumn(contentPadding = ScreenPadding) {
        item {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Documents", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text("Offline") }, leadingIcon = { Icon(Icons.Filled.Shield, null, tint = PrivateGreen) })
            }
            Text(
                "The paperwork that came with the scooter — readable without a connection.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VSpace(8.dp)
        }
        items(docs, key = { it.id }) { doc ->
            val source = PdfSource.valueOf(doc.id)
            val last = if (source == PdfSource.MANUAL) state.lastManualPage else state.lastPassportPage
            DocCard(doc, last, onOpen = { page -> onOpen(source, page) })
        }
        item {
            SectionTitle("Find")
            NavRow(Icons.Filled.Search, "Search both documents", "Full text — French / Arabic / English OCR", onClick = onOpenSearch)
            NavRow(Icons.AutoMirrored.Filled.Toc, "Table of contents", "Jump to any chapter", onClick = onOpenContents)
            NavRow(Icons.Filled.Bookmark, "Bookmarks", "Pages you saved", onClick = onOpenBookmarks)
        }
        if (state.recent.isNotEmpty()) {
            item { SectionTitle("Recently viewed") }
            items(state.recent) { (pdf, page) ->
                NavRow(Icons.AutoMirrored.Filled.MenuBook, "${pdf.displayName} — page $page", null) { onOpen(pdf, page) }
            }
        }
    }
}

@Composable
private fun DocCard(doc: DocInfo, lastPage: Int?, onOpen: (Int) -> Unit) {
    val context = LocalContext.current
    val cover = remember(doc.cover) {
        runCatching { context.assets.open(doc.cover).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
    }
    Panel(onClick = { onOpen(lastPage ?: 1) }, modifier = Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.padding(12.dp)) {
            if (cover != null) {
                Image(
                    cover, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.width(84.dp).height(112.dp).clip(RoundedCornerShape(10.dp))
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(doc.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(doc.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    lastPage?.let { "Continue at page $it" } ?: "Open from page 1",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    items(doc.highlights) { h -> SuggestionChip(onClick = { onOpen(h.page) }, label = { Text("${h.label} · p.${h.page}") }) }
                }
            }
        }
    }
}
