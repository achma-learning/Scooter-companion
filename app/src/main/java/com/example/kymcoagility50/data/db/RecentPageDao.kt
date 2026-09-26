package com.example.kymcoagility50.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kymcoagility50.data.model.PdfSource
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentPageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recent: RecentPageEntity)

    @Query("SELECT * FROM recent_pages ORDER BY lastViewedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<RecentPageEntity>>

    @Query("SELECT * FROM recent_pages WHERE pdf = :pdf ORDER BY lastViewedAt DESC LIMIT 1")
    suspend fun mostRecent(pdf: PdfSource): RecentPageEntity?

    @Query("DELETE FROM recent_pages")
    suspend fun clearAll()
}
