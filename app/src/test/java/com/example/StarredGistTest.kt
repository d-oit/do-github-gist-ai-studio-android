package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.dao.GistDao
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import com.example.ui.screens.GistHubAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StarredGistTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var db: AppDatabase
  private lateinit var gistDao: GistDao
  private lateinit var repository: GistRepository
  private lateinit var configPrefs: ConfigPrefs
  private lateinit var viewModel: GistViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    db =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor { it.run() }
        .setTransactionExecutor { it.run() }
        .build()
    gistDao = db.gistDao()
    configPrefs = ConfigPrefs(context)
    configPrefs.clear()
    repository = GistRepository(gistDao, FakeGitHubApiService(), configPrefs)
    viewModel =
      GistViewModel(
        repository = repository,
        configPrefs = configPrefs,
        appConfiguration =
          object : com.example.core.config.AppConfiguration {
            override fun geminiApiKeyOrNull(): String? = "fake_key"
          }
      )
  }

  @After
  fun tearDown() {
    db.close()
    configPrefs.clear()
    Dispatchers.resetMain()
  }

  @Test
  fun test_toggleStar_localDraft_updatesRoomDatabase() =
    runTest(testDispatcher) {
      // 1. Create a local draft gist
      viewModel.createGist(
        description = "Starred Draft Gist",
        filename = "draft.kt",
        content = "fun draft() {}",
        isPublic = true,
        isPinned = false
      )
      testDispatcher.scheduler.advanceUntilIdle()

      val gistsBefore = viewModel.gists.value
      assertEquals(1, gistsBefore.size)
      val gistId = gistsBefore.first().gist.id
      assertFalse(gistsBefore.first().gist.isStarred)

      // 2. Toggle star
      viewModel.toggleStar(gistId)
      testDispatcher.scheduler.advanceUntilIdle()

      // 3. Verify state persisted in Room DB
      val updatedFromDb = gistDao.getGistById(gistId)
      assertNotNull(updatedFromDb)
      assertTrue("Gist must be starred in Room database", updatedFromDb!!.gist.isStarred)
      assertFalse("Local-only draft must not set isStarredDirty", updatedFromDb.gist.isStarredDirty)

      // 4. Toggle star off
      viewModel.toggleStar(gistId)
      testDispatcher.scheduler.advanceUntilIdle()

      val unstarredFromDb = gistDao.getGistById(gistId)
      assertNotNull(unstarredFromDb)
      assertFalse(
        "Gist star must be toggled off in Room database",
        unstarredFromDb!!.gist.isStarred
      )
    }

  @Test
  fun test_toggleStar_offlineSyncedGist_setsStarredDirty() =
    runTest(testDispatcher) {
      // Insert a synced remote gist directly into Room
      val syncedEntity =
        GistEntity(
          id = "synced_123",
          description = "Synced Remote Gist",
          htmlUrl = "https://gist.github.com/synced_123",
          url = "https://api.github.com/gists/synced_123",
          createdAt = "2026-01-01T00:00:00Z",
          updatedAt = "2026-01-01T00:00:00Z",
          nodeId = "node_123",
          isPublic = true,
          isPinned = false,
          isLocalOnly = false,
          isDirty = false,
          isDeleted = false,
          isStarred = false,
          isStarredDirty = false,
          tags = emptyList(),
          ownerLogin = "testUser",
          ownerId = 12345,
          ownerAvatarUrl = "https://avatar.com/user"
        )
      val syncedFile =
        GistFileEntity(
          fileId = "file_123",
          gistId = "synced_123",
          filename = "main.kt",
          type = "text/plain",
          language = "Kotlin",
          rawUrl = "",
          size = 10L,
          content = "val x = 1"
        )
      gistDao.upsertGistWithFiles(syncedEntity, listOf(syncedFile))
      testDispatcher.scheduler.advanceUntilIdle()

      // Toggle star while offline (no token configured)
      viewModel.toggleStar("synced_123")
      testDispatcher.scheduler.advanceUntilIdle()

      // Verify state in Room DB
      val dbItem = gistDao.getGistById("synced_123")
      assertNotNull(dbItem)
      assertTrue("Synced gist must be marked isStarred = true", dbItem!!.gist.isStarred)
      assertTrue(
        "Synced gist must be marked isStarredDirty = true for future sync",
        dbItem.gist.isStarredDirty
      )
    }

  @Test
  fun test_starredFilter_filtersGistsInUI() =
    runTest(testDispatcher) {
      // Create one starred and one unstarred gist
      viewModel.createGist(
        description = "First Unstarred Gist",
        filename = "a.kt",
        content = "fun a() {}",
        isPublic = true,
        isPinned = false
      )
      viewModel.createGist(
        description = "Second Important Gist",
        filename = "b.kt",
        content = "fun b() {}",
        isPublic = true,
        isPinned = false
      )
      testDispatcher.scheduler.advanceUntilIdle()

      val gists = viewModel.gists.value
      assertEquals(2, gists.size)

      // Star the second gist
      val secondId = gists.find { it.gist.description == "Second Important Gist" }!!.gist.id
      viewModel.toggleStar(secondId)
      testDispatcher.scheduler.advanceUntilIdle()

      // Render Composable UI
      composeTestRule.setContent { MyApplicationTheme { GistHubAppScreen(viewModel = viewModel) } }

      // Click "Starred" filter chip
      composeTestRule.onNodeWithTag("tag_filter_starred").assertExists().performClick()
      testDispatcher.scheduler.advanceUntilIdle()
      composeTestRule.waitForIdle()

      // Verify only starred gist is displayed
      composeTestRule.onNodeWithText("Second Important Gist").assertExists()
      composeTestRule.onNodeWithText("First Unstarred Gist").assertDoesNotExist()
    }

  private class FakeGitHubApiService : com.example.data.remote.api.GitHubApiService {
    override suspend fun getGists(
      page: Int?,
      perPage: Int?
    ): List<com.example.data.remote.model.GistResponse> = emptyList()

    override suspend fun getGist(id: String) = throw Exception()

    override suspend fun getGistRevision(id: String, sha: String) = throw Exception()

    override suspend fun getAuthenticatedUser() = throw Exception()

    override suspend fun createGist(request: com.example.data.remote.model.GistRequest) =
      throw Exception()

    override suspend fun updateGist(
      id: String,
      request: com.example.data.remote.model.GistRequest
    ) = throw Exception()

    override suspend fun deleteGist(id: String): retrofit2.Response<Unit> =
      retrofit2.Response.success(Unit)

    override suspend fun checkIsStarred(id: String) = throw Exception()

    override suspend fun starGist(id: String) = throw Exception()

    override suspend fun unstarGist(id: String) = throw Exception()

    override suspend fun forkGist(id: String): com.example.data.remote.model.GistResponse =
      throw Exception()

    override suspend fun getGistComments(
      id: String
    ): List<com.example.data.remote.model.GistCommentResponse> = emptyList()

    override suspend fun createGistComment(
      id: String,
      request: com.example.data.remote.model.GistCommentRequest
    ): com.example.data.remote.model.GistCommentResponse = throw Exception()

    override suspend fun deleteGistComment(id: String, commentId: Long): retrofit2.Response<Unit> =
      retrofit2.Response.success(Unit)
  }
}
