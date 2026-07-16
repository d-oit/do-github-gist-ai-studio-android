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
class DeleteConfirmationDialogTest {

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
  fun test_deleteConfirmationFlow_cancel() =
    runTest(testDispatcher) {
      // Create local draft
      viewModel.createGist(
        description = "Test Draft to Delete Cancel",
        filename = "cancel_me.py",
        content = "print('stay')",
        isPublic = false,
        isPinned = false
      )
      testDispatcher.scheduler.advanceUntilIdle()

      val originalList = viewModel.gists.value
      assertEquals(1, originalList.size)
      val gistId = originalList.first().gist.id

      composeTestRule.setContent { MyApplicationTheme { GistHubAppScreen(viewModel = viewModel) } }

      // Verify the Gist card is displayed
      composeTestRule.onNodeWithTag("gist_card_$gistId").assertExists()

      // Dialog should NOT be displayed initially
      composeTestRule.onNodeWithTag("delete_confirm_dialog").assertDoesNotExist()

      // Tap the delete button
      composeTestRule.onNodeWithTag("delete_button_$gistId").performClick()

      // Verify the confirmation dialog is displayed
      composeTestRule.onNodeWithTag("delete_confirm_dialog").assertExists()

      // Tap cancel button
      composeTestRule.onNodeWithTag("delete_confirm_cancel").performClick()

      // Dialog should be dismissed
      composeTestRule.onNodeWithTag("delete_confirm_dialog").assertDoesNotExist()

      // Gist should NOT be deleted from the database/viewModel
      testDispatcher.scheduler.advanceUntilIdle()
      assertEquals(1, viewModel.gists.value.size)
      composeTestRule.onNodeWithTag("gist_card_$gistId").assertExists()
    }

  @Test
  fun test_deleteConfirmationFlow_confirm() =
    runTest(testDispatcher) {
      // Create local draft
      viewModel.createGist(
        description = "Test Draft to Delete Confirm",
        filename = "confirm_me.py",
        content = "print('goodbye')",
        isPublic = false,
        isPinned = false
      )
      testDispatcher.scheduler.advanceUntilIdle()

      val originalList = viewModel.gists.value
      assertEquals(1, originalList.size)
      val gistId = originalList.first().gist.id

      composeTestRule.setContent { MyApplicationTheme { GistHubAppScreen(viewModel = viewModel) } }

      // Verify the Gist card is displayed
      composeTestRule.onNodeWithTag("gist_card_$gistId").assertExists()

      // Dialog should NOT be displayed initially
      composeTestRule.onNodeWithTag("delete_confirm_dialog").assertDoesNotExist()

      // Tap the delete button
      composeTestRule.onNodeWithTag("delete_button_$gistId").performClick()

      // Verify confirmation dialog is displayed
      composeTestRule.onNodeWithTag("delete_confirm_dialog").assertExists()

      // Tap confirm/delete button
      composeTestRule.onNodeWithTag("delete_confirm_confirm").performClick()

      // Dialog should be dismissed
      composeTestRule.onNodeWithTag("delete_confirm_dialog").assertDoesNotExist()

      // Gist should be deleted from the database/viewModel
      testDispatcher.scheduler.advanceUntilIdle()
      assertTrue(viewModel.gists.value.isEmpty())
      composeTestRule.onNodeWithTag("gist_card_$gistId").assertDoesNotExist()
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

    override suspend fun forkGist(id: String): com.example.data.remote.model.GistResponse = throw Exception()
  }
}
