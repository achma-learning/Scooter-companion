package com.example.kymcoagility50.ui.screens.contents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kymcoagility50.AppContainer
import com.example.kymcoagility50.data.model.TocDocument
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
