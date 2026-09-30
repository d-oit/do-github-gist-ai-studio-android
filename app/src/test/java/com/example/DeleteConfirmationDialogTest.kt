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
    org.robolectric.shadows.ShadowLog.stream = System.out
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
      composeTestRule
        .onNodeWithTag("delete_button_$gistId", useUnmergedTree = true)
        .performScrollTo()
        .performClick()
      composeTestRule.waitForIdle()

      // Verify the confirmation dialog is displayed
      composeTestRule.onNodeWithTag("delete_confirm_dialog", useUnmergedTree = true).assertExists()

      // Tap cancel button
      composeTestRule.onNodeWithTag("delete_confirm_cancel", useUnmergedTree = true).performClick()
      composeTestRule.waitForIdle()

      // Dialog should be dismissed
      composeTestRule
        .onNodeWithTag("delete_confirm_dialog", useUnmergedTree = true)
        .assertDoesNotExist()

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
      composeTestRule
        .onNodeWithTag("delete_button_$gistId", useUnmergedTree = true)
        .performScrollTo()
        .performClick()
      composeTestRule.waitForIdle()

      // Verify confirmation dialog is displayed
      composeTestRule.onNodeWithTag("delete_confirm_dialog", useUnmergedTree = true).assertExists()

      // Tap confirm/delete button
      composeTestRule.onNodeWithTag("delete_confirm_confirm", useUnmergedTree = true).performClick()
      composeTestRule.waitForIdle()

      // Dialog should be dismissed
      composeTestRule
        .onNodeWithTag("delete_confirm_dialog", useUnmergedTree = true)
        .assertDoesNotExist()

      // Gist should be deleted from the database/viewModel
      testDispatcher.scheduler.advanceUntilIdle()
      assertTrue(viewModel.gists.value.isEmpty())
      composeTestRule.onNodeWithTag("gist_card_$gistId").assertDoesNotExist()
    }

  @Test
  fun test_deleteConfirmationFlow_fromDetailScreen() =
    runTest(testDispatcher) {
      var deleteClicked = false
      val sampleGist =
        com.example.data.local.entity.GistWithFiles(
          gist =
            com.example.data.local.entity.GistEntity(
              id = "detail_test_id",
              description = "Test Detail Delete Flow",
              htmlUrl = "https://gist.github.com/detail_test_id",
              url = "https://api.github.com/gists/detail_test_id",
              createdAt = "2026-01-01T00:00:00Z",
              updatedAt = "2026-01-01T00:00:00Z",
              nodeId = "node_123",
              isPublic = false,
              isPinned = false,
              isLocalOnly = false,
              isDirty = false,
              ownerLogin = "testuser",
              ownerId = 1,
              ownerAvatarUrl = ""
            ),
          files =
            listOf(
              com.example.data.local.entity.GistFileEntity(
                fileId = "detail_test_id_detail_delete.kt",
                gistId = "detail_test_id",
                filename = "detail_delete.kt",
                type = "text/plain",
                language = "Kotlin",
                rawUrl = "",
                size = 10,
                content = "val x = 42"
              )
            )
        )

      composeTestRule.setContent {
        MyApplicationTheme {
          com.example.ui.screens.GistDetailScreen(
            item = sampleGist,
            onBack = {},
            onEdit = {},
            onDelete = { deleteClicked = true },
            onTogglePin = {},
            onToggleStar = {}
          )
        }
      }

      // Verify detail screen is displayed
      composeTestRule.onNodeWithTag("gist_detail_screen", useUnmergedTree = true).assertExists()

      // Tap delete button in detail screen
      composeTestRule.onNodeWithTag("detail_delete_button", useUnmergedTree = true).performClick()
      composeTestRule.waitForIdle()

      // Verify onDelete callback was invoked
      assertTrue(deleteClicked)
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
