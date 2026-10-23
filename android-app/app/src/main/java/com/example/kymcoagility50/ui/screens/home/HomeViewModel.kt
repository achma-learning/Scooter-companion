package com.example.kymcoagility50.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kymcoagility50.AppContainer
import com.example.kymcoagility50.data.model.PdfSource
import com.example.kymcoagility50.privacy.AppMode
import com.example.kymcoagility50.privacy.PrivacyManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val mode: AppMode = AppMode.PRIVATE,
    val lastManualPage: Int? = null,
    val lastPassportPage: Int? = null,
    val recent: List<Pair<PdfSource, Int>> = emptyList()
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        PrivacyManager.mode,
        container.libraryRepository.observeRecent(6)
    ) { mode, recent ->
        HomeUiState(
            mode = mode,
            lastManualPage = recent.firstOrNull { it.pdf == PdfSource.MANUAL }?.page,
            lastPassportPage = recent.firstOrNull { it.pdf == PdfSource.PASSPORT }?.page,
            recent = recent.map { it.pdf to it.page }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            container.ocrRepository.ensureSeeded(PdfSource.MANUAL)
            container.ocrRepository.ensureSeeded(PdfSource.PASSPORT)
        }
    }
}
