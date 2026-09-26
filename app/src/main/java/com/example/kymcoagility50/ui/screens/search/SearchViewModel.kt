package com.example.kymcoagility50.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kymcoagility50.AppContainer
import com.example.kymcoagility50.data.model.PdfSource
import com.example.kymcoagility50.data.repository.OcrRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val filter: PdfSource? = null, // null = all
    val results: List<OcrRepository.SearchHit> = emptyList(),
    val searching: Boolean = false
)

@OptIn(FlowPreview::class)
class SearchViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private val filterFlow = MutableStateFlow<PdfSource?>(null)

    init {
        viewModelScope.launch {
            combine(queryFlow, filterFlow) { q, f -> q to f }
                .debounce(300)
                .collectLatest { (q, f) ->
                    if (q.isBlank()) {
                        _state.value = _state.value.copy(results = emptyList(), searching = false)
                        return@collectLatest
                    }
                    _state.value = _state.value.copy(searching = true)
                    val hits = container.ocrRepository.search(q, f)
                    _state.value = _state.value.copy(results = hits, searching = false)
                }
        }
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
        queryFlow.value = q
    }

    fun setFilter(filter: PdfSource?) {
        _state.value = _state.value.copy(filter = filter)
        filterFlow.value = filter
    }
}
