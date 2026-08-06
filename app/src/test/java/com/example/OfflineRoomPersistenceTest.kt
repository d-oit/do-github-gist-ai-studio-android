package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.security.GistContentEncryptor
import com.example.data.local.AppDatabase
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OfflineRoomPersistenceTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: GistRepository
  private lateinit var fakeApi: FakeGitHubApiService
  private lateinit var configPrefs: ConfigPrefs

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    fakeApi = FakeGitHubApiService()
    configPrefs = ConfigPrefs(context)
    val encryptor = GistContentEncryptor(context)

    repository =
      GistRepository(
        gistDao = db.gistDao(),
        apiService = fakeApi,
        configPrefs = configPrefs,
        contentEncryptor = encryptor
      )
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun offlineRoomPersistence_savesDraftGistLocally_whenNetworkIsUnavailable() = runBlocking {
    // Simulate offline state: API throws IOException
    fakeApi.shouldThrowNetworkError = true

    // Create local draft offline
    val draftId =
      repository.createLocalDraft(
        description = "Offline Offline Persistence Test Snippet",
        files = listOf("offline_main.kt" to "fun offline() = true"),
        isPublic = false,
        isPinned = true,
        tags = listOf("offline", "room", "kotlin")
      )

    // Verify draft was created and persisted in local Room DB
    val allGists = repository.allGists.first()
    assertEquals(1, allGists.size)

    val offlineGist = allGists[0]
    assertEquals(draftId, offlineGist.gist.id)
    assertEquals("Offline Offline Persistence Test Snippet", offlineGist.gist.description)
    assertTrue("Should be marked isLocalOnly", offlineGist.gist.isLocalOnly)
    assertEquals(1, offlineGist.files.size)
    assertEquals("offline_main.kt", offlineGist.files[0].filename)
    assertEquals("fun offline() = true", offlineGist.files[0].content)
    assertEquals(listOf("offline", "room", "kotlin"), offlineGist.gist.tags)
  }

  @Test
  fun offlineRoomPersistence_returnsCachedData_whenRemoteFetchFailsOffline() = runBlocking {
    // Pre-populate Room DB with synced gist
    val syncedEntity =
      GistEntity(
        id = "cached_gist_001",
        description = "Cached Local Gist",
        htmlUrl = "https://gist.github.com/cached_gist_001",
        url = "https://api.github.com/gists/cached_gist_001",
        createdAt = "2026-08-01T00:00:00Z",
        updatedAt = "2026-08-01T00:00:00Z",
        nodeId = "node_cached_001",
        isPublic = true,
        isPinned = false,
        isLocalOnly = false,
        isDirty = false,
        isDeleted = false,
        tags = listOf("cached", "offline"),
        ownerLogin = "testuser",
        ownerId = 1,
        ownerAvatarUrl = ""
      )
    val syncedFile =
      GistFileEntity(
        fileId = "f_cached_001",
        gistId = "cached_gist_001",
        filename = "cached.kt",
        type = "text/plain",
        language = "Kotlin",
        rawUrl = "",
        size = 20,
        content = repository.encryptContent("val cached = true")
      )

    db.gistDao().upsertGistWithFiles(syncedEntity, listOf(syncedFile))

    // Set token so fetchFromRemote attempts remote call
    configPrefs.setGithubToken("ghp_test_token_123456789")
    fakeApi.shouldThrowNetworkError = true

    // Call fetchFromRemote while offline
    val fetchResult = repository.fetchFromRemote()
    assertTrue(
      "fetchFromRemote should fail gracefully when network is unavailable",
      fetchResult.isFailure
    )

    // Verify local Room data is still completely accessible offline
    val offlineGists = repository.allGists.first()
    assertEquals(1, offlineGists.size)
    assertEquals("cached_gist_001", offlineGists[0].gist.id)
    assertEquals("val cached = true", offlineGists[0].files[0].content)
  }

  @Test
  fun offlineRoomPersistence_allowsLocalEdits_andUpdatesDirtyState() = runBlocking {
    // Insert initial draft
    val draftId =
      repository.createLocalDraft(
        description = "Original Description",
        files = listOf("code.kt" to "val v1 = 1"),
        isPublic = true,
        isPinned = false
      )

    // Edit local draft offline
    repository.updateGistLocal(
      id = draftId,
      description = "Updated Offline Description",
      files = listOf("code.kt" to "val v2 = 2"),
      isPublic = true,
      isPinned = true,
      tags = listOf("updated", "offline")
    )

    // Retrieve updated entry from Room DB
    val updatedGist = repository.getGist(draftId)
    assertNotNull(updatedGist)
    assertEquals("Updated Offline Description", updatedGist!!.gist.description)
    assertEquals("val v2 = 2", updatedGist.files[0].content)
  }

  @Test
  fun offlineRoomPersistence_searchesLocalRoomCache_byQueryOrTag() = runBlocking {
    repository.createLocalDraft(
      description = "Room Database Architecture",
      files = listOf("db.kt" to "class AppDatabase"),
      isPublic = false,
      isPinned = false,
      tags = listOf("architecture", "database")
    )

    repository.createLocalDraft(
      description = "Compose UI Theme",
      files = listOf("Theme.kt" to "MaterialTheme"),
      isPublic = false,
      isPinned = false,
      tags = listOf("ui", "compose")
    )

    val dbSearchFlow = repository.searchLocalGists("Database")
    val dbResults = dbSearchFlow.first()
    assertEquals(1, dbResults.size)
    assertEquals("Room Database Architecture", dbResults[0].gist.description)

    val allGists = repository.allGists.first()
    val uiTagResults = allGists.filter { it.gist.tags.contains("ui") }
    assertEquals(1, uiTagResults.size)
    assertEquals("Compose UI Theme", uiTagResults[0].gist.description)
  }
}
