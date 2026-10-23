package io.github.achmalearning.scootercompanion.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.data.model.PdfSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val lastManualPage: Int? = null,
    val lastPassportPage: Int? = null,
    val recent: List<Pair<PdfSource, Int>> = emptyList()
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = container.libraryRepository.observeRecent(6).map { recent ->
        HomeUiState(
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
