package com.example.kymcoagility50.ui.screens.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kymcoagility50.AppContainer
import com.example.kymcoagility50.data.db.BookmarkEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookmarksViewModel(private val container: AppContainer) : ViewModel() {
    val bookmarks: StateFlow<List<BookmarkEntity>> = container.libraryRepository.observeBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun rename(bookmark: BookmarkEntity, note: String?) {
        viewModelScope.launch { container.libraryRepository.renameBookmark(bookmark, note) }
    }

    fun delete(bookmark: BookmarkEntity) {
        viewModelScope.launch { container.libraryRepository.deleteBookmark(bookmark) }
    }
}
