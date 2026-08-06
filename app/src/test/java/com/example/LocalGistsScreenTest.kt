package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.ui.screens.LocalGistsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalGistsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val sampleGist1 =
    GistWithFiles(
      gist =
        GistEntity(
          id = "local_gist_1",
          description = "First Local Gist Title",
          htmlUrl = "https://gist.github.com/1",
          url = "https://api.github.com/gists/1",
          createdAt = "2026-01-01T00:00:00Z",
          updatedAt = "2026-01-01T00:00:00Z",
          nodeId = "node_1",
          isPublic = true,
          isPinned = true,
          isLocalOnly = true,
          isDirty = false,
          isDeleted = false,
          isStarred = false,
          ownerLogin = "user1",
          ownerId = 100,
          ownerAvatarUrl = "https://avatars.githubusercontent.com/u/100"
        ),
      files =
        listOf(
          GistFileEntity(
            fileId = "file_1",
            gistId = "local_gist_1",
            filename = "Main.kt",
            type = "text/plain",
            language = "Kotlin",
            rawUrl = "https://gist.githubusercontent.com/raw/1",
            size = 100L,
            content = "fun main() {\n  println(\"Hello World\")\n}"
          )
        )
    )

  private val sampleGist2 =
    GistWithFiles(
      gist =
        GistEntity(
          id = "local_gist_2",
          description = "Second Synced Gist",
          htmlUrl = "https://gist.github.com/2",
          url = "https://api.github.com/gists/2",
          createdAt = "2026-01-02T00:00:00Z",
          updatedAt = "2026-01-02T00:00:00Z",
          nodeId = "node_2",
          isPublic = false,
          isPinned = false,
          isLocalOnly = false,
          isDirty = false,
          isDeleted = false,
          isStarred = true,
          ownerLogin = "user2",
          ownerId = 200,
          ownerAvatarUrl = "https://avatars.githubusercontent.com/u/200"
        ),
      files =
        listOf(
          GistFileEntity(
            fileId = "file_2",
            gistId = "local_gist_2",
            filename = "Utils.kt",
            type = "text/plain",
            language = "Kotlin",
            rawUrl = "https://gist.githubusercontent.com/raw/2",
            size = 200L,
            content = "class Utils {\n  fun calculate() = 42\n}"
          )
        )
    )

  @Test
  fun testLocalGistsScreenRendersTitleSnippetAndSyncIcon() {
    composeTestRule.setContent {
      LocalGistsScreen(
        gists = listOf(sampleGist1, sampleGist2),
        searchQuery = "",
        onSearchQueryChange = {},
        onTogglePin = {},
        onToggleStar = {},
        onEdit = {},
        onDelete = {},
        onPreview = {}
      )
    }

    composeTestRule.onNodeWithTag("local_gists_screen").assertIsDisplayed()
    composeTestRule.onNodeWithText("Local Gists Repository").assertIsDisplayed()

    // Title verification
    composeTestRule.onNodeWithText("First Local Gist Title").assertIsDisplayed()
    composeTestRule.onNodeWithText("Second Synced Gist").assertIsDisplayed()

    // Snippet verification
    composeTestRule
      .onNodeWithTag("local_gist_snippet_local_gist_1", useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithTag("local_gist_snippet_local_gist_2", useUnmergedTree = true)
      .assertExists()

    // Sync status icon verification
    composeTestRule
      .onNodeWithTag("local_gist_sync_icon_local_gist_1", useUnmergedTree = true)
      .assertExists()
    composeTestRule
      .onNodeWithTag("local_gist_sync_icon_local_gist_2", useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun testFilterChipsFilterBySyncStatus() {
    composeTestRule.setContent {
      LocalGistsScreen(
        gists = listOf(sampleGist1, sampleGist2),
        searchQuery = "",
        onSearchQueryChange = {},
        onTogglePin = {},
        onToggleStar = {},
        onEdit = {},
        onDelete = {},
        onPreview = {}
      )
    }

    // Click "Local Only" filter chip
    composeTestRule.onNodeWithTag("filter_chip_local_only").performClick()
    composeTestRule.onNodeWithText("First Local Gist Title").assertIsDisplayed()

    // Click "Synced" filter chip
    composeTestRule.onNodeWithTag("filter_chip_synced").performClick()
    composeTestRule.onNodeWithText("Second Synced Gist").assertIsDisplayed()
  }

  @Test
  fun testClickCallbacksTriggersExpectedActions() {
    var editedGistId = ""
    var deletedGistId = ""

    composeTestRule.setContent {
      LocalGistsScreen(
        gists = listOf(sampleGist1),
        searchQuery = "",
        onSearchQueryChange = {},
        onTogglePin = {},
        onToggleStar = {},
        onEdit = { editedGistId = it.gist.id },
        onDelete = { deletedGistId = it },
        onPreview = {}
      )
    }

    composeTestRule
      .onNodeWithTag("local_edit_button_local_gist_1", useUnmergedTree = true)
      .performClick()
    assertEquals("local_gist_1", editedGistId)

    composeTestRule
      .onNodeWithTag("local_delete_button_local_gist_1", useUnmergedTree = true)
      .performClick()
    assertEquals("local_gist_1", deletedGistId)
  }

  @Test
  fun testPullToRefreshIndicatorRenderedWhenRefreshing() {
    composeTestRule.setContent {
      LocalGistsScreen(
        gists = listOf(sampleGist1),
        searchQuery = "",
        onSearchQueryChange = {},
        onTogglePin = {},
        onToggleStar = {},
        onEdit = {},
        onDelete = {},
        onPreview = {},
        isRefreshing = true,
        onRefresh = {}
      )
    }

    composeTestRule.onNodeWithTag("local_gists_pull_refresh_indicator").assertIsDisplayed()
  }
}
