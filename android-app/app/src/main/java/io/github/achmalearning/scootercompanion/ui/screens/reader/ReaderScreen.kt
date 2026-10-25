package io.github.achmalearning.scootercompanion.ui.screens.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.achmalearning.scootercompanion.data.model.PdfSource
import io.github.achmalearning.scootercompanion.ui.components.DocDownloadCard
import io.github.achmalearning.scootercompanion.ui.common.appViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun ReaderScreen(source: PdfSource, startPage: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel { ReaderViewModel(context, it, source, startPage) }
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showJump by remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .map { it + 1 }
            .collect { vm.goToPage(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${source.displayName} — ${state.currentPage}/${state.pageCount}") },
                actions = {
                    IconButton(onClick = { vm.toggleBookmark() }) {
                        Icon(
                            if (state.isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (state.pageCount > 0) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Slider(
                        value = state.currentPage.toFloat(),
                        onValueChange = { vm.goToPage(it.toInt()) },
                        valueRange = 1f..state.pageCount.toFloat().coerceAtLeast(1f)
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.needsDownload -> DocDownloadCard(
                    source.displayName, state.downloadBytes, state.downloadProgress, state.error, vm::download,
                    Modifier.align(Alignment.Center).padding(16.dp)
                )
                state.error != null -> Text(
                    "Couldn't load this document: ${state.error}",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
                else -> {
                    val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
                    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                        scale = (scale * zoomChange).coerceIn(1f, 4f)
                        offset += panChange
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(transformState)
                            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                    ) {
                        items(state.pageCount) { index ->
                            val page = index + 1
                            PageImage(vm, page, screenWidthPx)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageImage(vm: ReaderViewModel, page: Int, widthPx: Int) {
    var bitmap by remember(page) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(page) { bitmap = vm.bitmapFor(page, widthPx) }
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = "Page $page")
        } ?: CircularProgressIndicator()
    }
}
