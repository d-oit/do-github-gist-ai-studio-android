package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GistDaoTest {

  private lateinit var db: AppDatabase

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun insertAndRetrieveGistWithFiles() = runBlocking {
    val dao = db.gistDao()
    val gist =
      GistEntity(
        id = "test-id",
        description = "Test description",
        isPublic = true,
        createdAt = "2026-07-17T09:00:00Z",
        updatedAt = "2026-07-17T09:00:00Z",
        ownerLogin = "test-user",
        ownerAvatarUrl = "https://test.com/avatar.png",
        htmlUrl = "https://test.com/gist-html",
        url = "https://test.com/api/gist",
        nodeId = "node-id-123",
        isPinned = false,
        isLocalOnly = false,
        isDirty = false,
        isDeleted = false,
        isStarred = false,
        isStarredDirty = false,
        tags = listOf("tag1", "tag2"),
        ownerId = 12345
      )

    val file =
      GistFileEntity(
        fileId = "file-id-1",
        gistId = "test-id",
        filename = "test.kt",
        type = "Kotlin",
        language = "Kotlin",
        rawUrl = "https://test.com/raw",
        size = 100,
        content = "fun main() {}"
      )

    dao.upsertGistWithFiles(gist, listOf(file))

    val retrieved = dao.getGistById("test-id")
    assertNotNull(retrieved)
    assertEquals("test-id", retrieved!!.gist.id)
    assertEquals("Test description", retrieved.gist.description)
    assertEquals(1, retrieved.files.size)
    assertEquals("test.kt", retrieved.files[0].filename)
    assertEquals("fun main() {}", retrieved.files[0].content)
  }

  @Test
  fun observeUnsynchronizedGistsOnlyShowsPendingChanges() = runBlocking {
    val dao = db.gistDao()
    val syncedGist =
      GistEntity(
        id = "synced-id",
        description = "Synced",
        isPublic = true,
        createdAt = "2026-07-17T09:00:00Z",
        updatedAt = "2026-07-17T09:00:00Z",
        ownerLogin = "test-user",
        ownerAvatarUrl = "https://test.com/avatar.png",
        htmlUrl = "https://test.com/gist-html",
        url = "https://test.com/api/synced",
        nodeId = "synced-node",
        isPinned = false,
        isLocalOnly = false,
        isDirty = false,
        isDeleted = false,
        isStarred = false,
        isStarredDirty = false,
        tags = emptyList(),
        ownerId = 111
      )

    val dirtyGist =
      GistEntity(
        id = "dirty-id",
        description = "Dirty",
        isPublic = true,
        createdAt = "2026-07-17T09:00:00Z",
        updatedAt = "2026-07-17T09:00:00Z",
        ownerLogin = "test-user",
        ownerAvatarUrl = "https://test.com/avatar.png",
        htmlUrl = "https://test.com/gist-html",
        url = "https://test.com/api/dirty",
        nodeId = "dirty-node",
        isPinned = false,
        isLocalOnly = false,
        isDirty = true,
        isDeleted = false,
        isStarred = false,
        isStarredDirty = false,
        tags = emptyList(),
        ownerId = 111
      )

    dao.insertGist(syncedGist)
    dao.insertGist(dirtyGist)

    val unsynced = dao.getUnsynchronizedGists()
    assertEquals(1, unsynced.size)
    assertEquals("dirty-id", unsynced[0].gist.id)
  }

  @Test
  fun searchLocalGistsFiltersByFilenameDescriptionAndContentSnippet() = runBlocking {
    val dao = db.gistDao()
    val gist1 =
      GistEntity(
        id = "gist-1",
        description = "Alpha project setup",
        isPublic = true,
        createdAt = "2026-08-01T10:00:00Z",
        updatedAt = "2026-08-01T10:00:00Z",
        ownerLogin = "test-user",
        ownerAvatarUrl = "https://test.com/avatar.png",
        htmlUrl = "",
        url = "",
        nodeId = "node-1",
        isPinned = false,
        isLocalOnly = false,
        isDirty = false,
        isDeleted = false,
        isStarred = false,
        isStarredDirty = false,
        tags = emptyList(),
        ownerId = 1
      )
    val file1 =
      GistFileEntity(
        fileId = "file-1",
        gistId = "gist-1",
        filename = "main.kt",
        type = "Kotlin",
        language = "Kotlin",
        rawUrl = "",
        size = 50,
        content = "fun helloWorld() { println(\"quantum computing\") }"
      )

    val gist2 =
      GistEntity(
        id = "gist-2",
        description = "Beta notes",
        isPublic = true,
        createdAt = "2026-08-02T10:00:00Z",
        updatedAt = "2026-08-02T10:00:00Z",
        ownerLogin = "test-user",
        ownerAvatarUrl = "https://test.com/avatar.png",
        htmlUrl = "",
        url = "",
        nodeId = "node-2",
        isPinned = false,
        isLocalOnly = false,
        isDirty = false,
        isDeleted = false,
        isStarred = false,
        isStarredDirty = false,
        tags = emptyList(),
        ownerId = 1
      )
    val file2 =
      GistFileEntity(
        fileId = "file-2",
        gistId = "gist-2",
        filename = "script_quantum.py",
        type = "Python",
        language = "Python",
        rawUrl = "",
        size = 30,
        content = "print('hello')"
      )

    dao.upsertGistWithFiles(gist1, listOf(file1))
    dao.upsertGistWithFiles(gist2, listOf(file2))

    // Search by content snippet ("quantum computing") -> Should find gist1
    val contentResults = dao.searchLocalGists("quantum computing").first()
    assertEquals(1, contentResults.size)
    assertEquals("gist-1", contentResults[0].gist.id)

    // Search by filename ("script_quantum") -> Should find gist2
    val filenameResults = dao.searchLocalGists("script_quantum").first()
    assertEquals(1, filenameResults.size)
    assertEquals("gist-2", filenameResults[0].gist.id)

    // Search by description ("Alpha") -> Should find gist1
    val descResults = dao.searchLocalGists("Alpha").first()
    assertEquals(1, descResults.size)
    assertEquals("gist-1", descResults[0].gist.id)
  }
}
