package com.example.kymcoagility50.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.kymcoagility50.data.model.PdfSource
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE pdf = :pdf ORDER BY createdAt DESC")
    fun observeForPdf(pdf: PdfSource): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE pdf = :pdf AND page = :page LIMIT 1")
    suspend fun findByPage(pdf: PdfSource, page: Int): BookmarkEntity?

    @Query("DELETE FROM bookmarks")
    suspend fun clearAll()
}
