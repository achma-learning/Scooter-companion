package io.github.achmalearning.scootercompanion.ui.screens.contents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.achmalearning.scootercompanion.AppContainer
import io.github.achmalearning.scootercompanion.data.model.TocDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContentsViewModel(container: AppContainer) : ViewModel() {
    private val _toc = MutableStateFlow<TocDocument?>(null)
    val toc: StateFlow<TocDocument?> = _toc.asStateFlow()

    init {
        viewModelScope.launch { _toc.value = container.tocRepository.load() }
    }
}
