package io.github.achmalearning.scootercompanion.data.repository

import io.github.achmalearning.scootercompanion.data.db.BookmarkDao
import io.github.achmalearning.scootercompanion.data.db.BookmarkEntity
import io.github.achmalearning.scootercompanion.data.db.RecentPageDao
import io.github.achmalearning.scootercompanion.data.db.RecentPageEntity
import io.github.achmalearning.scootercompanion.data.model.PdfSource
import kotlinx.coroutines.flow.Flow

class LibraryRepository(
    private val bookmarkDao: BookmarkDao,
    private val recentPageDao: RecentPageDao
) {
    fun observeBookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()
    fun observeBookmarks(pdf: PdfSource): Flow<List<BookmarkEntity>> = bookmarkDao.observeForPdf(pdf)
    fun observeRecent(limit: Int = 10): Flow<List<RecentPageEntity>> = recentPageDao.observeRecent(limit)

    suspend fun toggleBookmark(pdf: PdfSource, page: Int, title: String) {
        val existing = bookmarkDao.findByPage(pdf, page)
        if (existing != null) {
            bookmarkDao.delete(existing)
        } else {
            bookmarkDao.insert(
                BookmarkEntity(pdf = pdf, page = page, title = title, note = null, createdAt = System.currentTimeMillis())
            )
        }
    }

    suspend fun renameBookmark(bookmark: BookmarkEntity, newNote: String?) {
        bookmarkDao.update(bookmark.copy(note = newNote))
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) = bookmarkDao.delete(bookmark)

    suspend fun isBookmarked(pdf: PdfSource, page: Int): Boolean = bookmarkDao.findByPage(pdf, page) != null

    suspend fun recordViewed(pdf: PdfSource, page: Int) {
        recentPageDao.upsert(RecentPageEntity(pdf = pdf, page = page, lastViewedAt = System.currentTimeMillis()))
    }

    suspend fun mostRecent(pdf: PdfSource): RecentPageEntity? = recentPageDao.mostRecent(pdf)

    suspend fun clearBookmarks() = bookmarkDao.clearAll()
    suspend fun clearRecent() = recentPageDao.clearAll()
}
