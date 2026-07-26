package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.dao.GistDao
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
class SwipeToDeleteAndUndoTest {

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
    Dispatchers.resetMain()
  }

  @Test
  fun test_deleteGist_and_undo_restoresItem() =
    runTest(testDispatcher) {
      viewModel.createGist(
        description = "Test Swipe Delete Undo",
        filename = "test_undo.kt",
        content = "fun test() {}",
        isPublic = true,
        isPinned = false
      )
      testDispatcher.scheduler.advanceUntilIdle()

      val originalList = viewModel.gists.value
      assertEquals(1, originalList.size)
      val item = originalList.first()
      val gistId = item.gist.id

      // Trigger deletion
      viewModel.deleteGist(gistId)
      testDispatcher.scheduler.advanceUntilIdle()

      // Should be removed from gists list
      assertTrue(viewModel.gists.value.isEmpty())

      // Pending delete event should hold the deleted item
      assertNotNull(viewModel.pendingDeleteEvent.value)
      assertEquals(
        "test_undo.kt",
        viewModel.pendingDeleteEvent.value?.files?.firstOrNull()?.filename
      )

      // Now restore
      viewModel.restoreGist(viewModel.pendingDeleteEvent.value!!)
      testDispatcher.scheduler.advanceUntilIdle()

      // Should be restored
      assertEquals(1, viewModel.gists.value.size)
      assertEquals(gistId, viewModel.gists.value.first().gist.id)
      assertEquals("Gist restored", viewModel.statusMessage.value)
    }

  @Test
  fun test_swipeToDismissContainer_existsInUI() =
    runTest(testDispatcher) {
      viewModel.createGist(
        description = "Test Swipe UI Container",
        filename = "swipe_me.kt",
        content = "val x = 1",
        isPublic = true,
        isPinned = false
      )
      testDispatcher.scheduler.advanceUntilIdle()

      val gistId = viewModel.gists.value.first().gist.id

      composeTestRule.setContent { MyApplicationTheme { GistHubAppScreen(viewModel = viewModel) } }

      // Verify SwipeToDismiss container with test tag exists
      composeTestRule.onNodeWithTag("swipe_to_dismiss_$gistId").assertExists()
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
  }
}
