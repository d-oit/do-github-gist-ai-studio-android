package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
  @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
  fun observeRecentSearchHistory(limit: Int = 10): Flow<List<SearchHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSearchQuery(entity: SearchHistoryEntity)

  @Query("DELETE FROM search_history WHERE query = :query")
  suspend fun deleteSearchQuery(query: String)

  @Query("DELETE FROM search_history") suspend fun clearAllSearchHistory()
}
