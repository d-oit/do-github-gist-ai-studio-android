package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import kotlinx.coroutines.flow.Flow

@Dao
interface GistDao {

  @Transaction
  @Query("SELECT * FROM gists WHERE isDeleted = 0 ORDER BY createdAt DESC")
  fun observeAllGists(): Flow<List<GistWithFiles>>

  @Transaction
  @Query("SELECT * FROM gists WHERE id = :id")
  fun observeGistById(id: String): Flow<GistWithFiles?>

  @Transaction
  @Query("SELECT * FROM gists WHERE id = :id")
  suspend fun getGistById(id: String): GistWithFiles?

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGist(gist: GistEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFiles(files: List<GistFileEntity>)

  @Query("DELETE FROM gist_files WHERE gistId = :gistId")
  suspend fun deleteFilesByGistId(gistId: String)

  @Query("DELETE FROM gists WHERE id = :id") suspend fun deleteGistById(id: String)

  @Query("UPDATE gists SET tags = :tags WHERE id = :id")
  suspend fun updateTags(id: String, tags: List<String>)

  @Transaction
  suspend fun upsertGistWithFiles(gist: GistEntity, files: List<GistFileEntity>) {
    deleteFilesByGistId(gist.id)
    insertGist(gist)
    insertFiles(files)
  }

  @Transaction
  @Query(
    "SELECT * FROM gists WHERE isDirty = 1 OR isLocalOnly = 1 OR isDeleted = 1 OR isStarredDirty = 1"
  )
  suspend fun getUnsynchronizedGists(): List<GistWithFiles>

  @Transaction
  @Query(
    "SELECT * FROM gists WHERE isDirty = 1 OR isLocalOnly = 1 OR isDeleted = 1 OR isStarredDirty = 1"
  )
  fun observeUnsynchronizedGists(): Flow<List<GistWithFiles>>

  @Transaction
  @Query(
    """
        SELECT DISTINCT gists.* FROM gists
        LEFT JOIN gist_files ON gists.id = gist_files.gistId
        WHERE gists.isDeleted = 0 AND (gists.description LIKE '%' || :query || '%'
           OR gist_files.filename LIKE '%' || :query || '%')
        ORDER BY gists.createdAt DESC
    """
  )
  fun searchLocalGists(query: String): Flow<List<GistWithFiles>>

  @Query("DELETE FROM gists") suspend fun deleteAllGists()

  @Query("DELETE FROM gist_files") suspend fun deleteAllFiles()

  @Transaction
  suspend fun clearAllData() {
    deleteAllFiles()
    deleteAllGists()
  }
}
