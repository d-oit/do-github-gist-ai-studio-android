package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.security.GistContentEncryptor
import com.example.data.local.AppDatabase
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.data.local.pref.ConfigPrefs
import com.example.data.remote.model.GistCommentResponse
import com.example.data.remote.model.GistOwnerResponse
import com.example.data.repository.GistRepository
import com.example.ui.components.GistCommentsView
import com.example.ui.screens.GistDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GistViewModel
import com.example.ui.viewmodel.deleteComment
import com.example.ui.viewmodel.loadComments
import com.example.ui.viewmodel.postComment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GistCommentsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var db: AppDatabase
  private lateinit var fakeApi: FakeGitHubApiService
  private lateinit var configPrefs: ConfigPrefs
  private lateinit var repository: GistRepository
  private lateinit var viewModel: GistViewModel

  private val testOwner =
    GistOwnerResponse(
      login = "commenter_user",
      id = 1234,
      avatarUrl = "https://github.com/avatar.png"
    )

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    db =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    fakeApi = FakeGitHubApiService()
    configPrefs = ConfigPrefs(context)
    configPrefs.setOwnerLogin("commenter_user")
    configPrefs.setGithubToken("ghp_test_token_12345")
    val encryptor = GistContentEncryptor(context)

    repository =
      GistRepository(
        gistDao = db.gistDao(),
        apiService = fakeApi,
        configPrefs = configPrefs,
        contentEncryptor = encryptor
      )

    viewModel =
      GistViewModel(repository, configPrefs, com.example.core.config.AppConfigurationImpl())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun gistCommentsView_rendersEmptyState_whenNoComments() {
    composeTestRule.setContent {
      MyApplicationTheme {
        GistCommentsView(
          comments = emptyList(),
          isLoading = false,
          errorMessage = null,
          isPosting = false,
          currentUserLogin = "commenter_user",
          onPostComment = {},
          onDeleteComment = {},
          onRetry = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("gist_comments_view").assertIsDisplayed()
    composeTestRule.onNodeWithTag("comment_composer_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("comments_empty_state").assertIsDisplayed()
  }

  @Test
  fun gistCommentsView_rendersLoadingState() {
    composeTestRule.setContent {
      MyApplicationTheme {
        GistCommentsView(
          comments = emptyList(),
          isLoading = true,
          errorMessage = null,
          isPosting = false,
          currentUserLogin = "commenter_user",
          onPostComment = {},
          onDeleteComment = {},
          onRetry = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("comments_loading_indicator").assertIsDisplayed()
  }

  @Test
  fun gistCommentsView_rendersErrorState_andInvokesRetry() {
    var retried = false
    composeTestRule.setContent {
      MyApplicationTheme {
        GistCommentsView(
          comments = emptyList(),
          isLoading = false,
          errorMessage = "Network timed out",
          isPosting = false,
          currentUserLogin = "commenter_user",
          onPostComment = {},
          onDeleteComment = {},
          onRetry = { retried = true }
        )
      }
    }

    composeTestRule.onNodeWithTag("comments_error_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("comments_retry_button").performClick()
    assertTrue(retried)
  }

  @Test
  fun gistCommentsView_rendersCommentsList_andAllowsDeletingOwnComment() {
    val sampleComment =
      GistCommentResponse(
        id = 999L,
        nodeId = "node_999",
        url = "https://api.github.com/comments/999",
        body = "This is a great code snippet!",
        user = testOwner,
        createdAt = "2026-09-29T10:00:00Z",
        updatedAt = "2026-09-29T10:00:00Z"
      )

    var deletedCommentId: Long? = null

    composeTestRule.setContent {
      MyApplicationTheme {
        GistCommentsView(
          comments = listOf(sampleComment),
          isLoading = false,
          errorMessage = null,
          isPosting = false,
          currentUserLogin = "commenter_user",
          onPostComment = {},
          onDeleteComment = { deletedCommentId = it },
          onRetry = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("gist_comment_item_999").assertIsDisplayed()
    composeTestRule.onNodeWithText("This is a great code snippet!").assertIsDisplayed()
    composeTestRule.onNodeWithTag("delete_comment_999").performClick()
    assertEquals(999L, deletedCommentId)
  }

  @Test
  fun gistCommentsView_postingComment_triggersCallback() {
    var postedBody = ""

    composeTestRule.setContent {
      MyApplicationTheme {
        GistCommentsView(
          comments = emptyList(),
          isLoading = false,
          errorMessage = null,
          isPosting = false,
          currentUserLogin = "commenter_user",
          onPostComment = { postedBody = it },
          onDeleteComment = {},
          onRetry = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("comment_input").performTextInput("LGTM! Verified.")
    composeTestRule.onNodeWithTag("comment_submit_button").performClick()
    assertEquals("LGTM! Verified.", postedBody)
  }

  @Test
  fun gistViewModel_commentsLifecycle_endToEnd() = runTest {
    val gistId = "gist_with_comments"

    // Seed initial comments in fakeApi
    fakeApi.commentsMap[gistId] =
      mutableListOf(
        GistCommentResponse(
          id = 101L,
          nodeId = "node_101",
          url = "https://api.github.com/comments/101",
          body = "Initial comment",
          user = testOwner,
          createdAt = "2026-09-29T09:00:00Z",
          updatedAt = "2026-09-29T09:00:00Z"
        )
      )

    // 1. Load comments
    viewModel.loadComments(gistId)
    assertEquals(1, viewModel.commentsList.value.size)
    assertEquals("Initial comment", viewModel.commentsList.value[0].body)

    // 2. Post comment
    viewModel.postComment(gistId, "Second reply")
    assertEquals(2, viewModel.commentsList.value.size)
    assertEquals("Second reply", viewModel.commentsList.value[1].body)

    // 3. Delete comment
    viewModel.deleteComment(gistId, 101L)
    assertEquals(1, viewModel.commentsList.value.size)
    assertEquals("Second reply", viewModel.commentsList.value[0].body)
  }

  @Test
  fun gistDetailScreen_displaysCommentsTab_andLoadsComments() {
    val sampleGist =
      GistWithFiles(
        gist =
          GistEntity(
            id = "gist_detail_comments_test",
            description = "Gist with comments tab",
            htmlUrl = "https://gist.github.com/test",
            url = "https://api.github.com/gists/test",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            nodeId = "node_c",
            isPublic = true,
            isPinned = false,
            isLocalOnly = false,
            isDirty = false,
            ownerLogin = "commenter_user",
            ownerId = 1,
            ownerAvatarUrl = ""
          ),
        files =
          listOf(
            GistFileEntity(
              fileId = "file_c",
              gistId = "gist_detail_comments_test",
              filename = "main.py",
              type = "text/plain",
              language = "Python",
              rawUrl = "",
              size = 40,
              content = "print('hello')"
            )
          )
      )

    composeTestRule.setContent {
      MyApplicationTheme {
        GistDetailScreen(
          item = sampleGist,
          viewModel = viewModel,
          onBack = {},
          onEdit = {},
          onDelete = {},
          onTogglePin = {},
          onToggleStar = {}
        )
      }
    }

    // Scroll to and verify Comments tab button exists
    composeTestRule
      .onNodeWithTag("detail_screen_lazy_column")
      .performScrollToNode(hasTestTag("detail_tab_comments"))

    composeTestRule.onNodeWithTag("detail_tab_comments").assertIsDisplayed()

    // Tap Comments tab
    composeTestRule.onNodeWithTag("detail_tab_comments").performClick()
    composeTestRule.waitForIdle()

    // Scroll to comments view and verify it exists
    composeTestRule
      .onNodeWithTag("detail_screen_lazy_column")
      .performScrollToNode(hasTestTag("gist_comments_view"))

    composeTestRule.onNodeWithTag("gist_comments_view", useUnmergedTree = true).assertExists()
  }
}
