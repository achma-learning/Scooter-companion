package io.github.achmalearning.scootercompanion.ui.screens.reader

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.data.model.PdfSource
import io.github.achmalearning.scootercompanion.pdf.PdfPageRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReaderUiState(
    val pageCount: Int = 0,
    val currentPage: Int = 1,
    val isBookmarked: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null
)

class ReaderViewModel(
    private val context: Context,
    private val container: AppContainer,
    private val source: PdfSource,
    startPage: Int
) : ViewModel() {

    private val renderer = PdfPageRenderer(context, source)
    private val _state = MutableStateFlow(ReaderUiState(currentPage = startPage))
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                renderer.open()
                _state.value = _state.value.copy(
                    pageCount = renderer.pageCount,
                    loading = false,
                    isBookmarked = container.libraryRepository.isBookmarked(source, _state.value.currentPage)
                )
                container.libraryRepository.recordViewed(source, _state.value.currentPage)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Could not open PDF")
            }
        }
    }

    suspend fun bitmapFor(page: Int, widthPx: Int): Bitmap = renderer.renderPage(page, widthPx)

    fun goToPage(page: Int) {
        val clamped = page.coerceIn(1, _state.value.pageCount.coerceAtLeast(1))
        _state.value = _state.value.copy(currentPage = clamped)
        viewModelScope.launch {
            container.libraryRepository.recordViewed(source, clamped)
            _state.value = _state.value.copy(isBookmarked = container.libraryRepository.isBookmarked(source, clamped))
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            container.libraryRepository.toggleBookmark(source, _state.value.currentPage, "${source.displayName} p.${_state.value.currentPage}")
            _state.value = _state.value.copy(isBookmarked = container.libraryRepository.isBookmarked(source, _state.value.currentPage))
        }
    }

    override fun onCleared() {
        viewModelScope.launch { renderer.close() }
    }
}
