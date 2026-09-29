package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.ui.components.GistListView
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GistListViewTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createSampleGist(
    id: String,
    filename: String = "script.kt",
    description: String = "Test Description for $id",
    isLocalOnly: Boolean = false,
    isDirty: Boolean = false,
    isDeleted: Boolean = false,
    isPinned: Boolean = false,
    isStarred: Boolean = false
  ): GistWithFiles {
    return GistWithFiles(
      gist =
        GistEntity(
          id = id,
          description = description,
          htmlUrl = "https://gist.github.com/$id",
          url = "https://api.github.com/gists/$id",
          createdAt = "2026-01-01T00:00:00Z",
          updatedAt = "2026-01-01T00:00:00Z",
          nodeId = "node_$id",
          isPublic = true,
          isPinned = isPinned,
          isLocalOnly = isLocalOnly,
          isDirty = isDirty,
          isDeleted = isDeleted,
          isStarred = isStarred,
          ownerLogin = "octocat",
          ownerId = 1,
          ownerAvatarUrl = "https://avatars.githubusercontent.com/u/1"
        ),
      files =
        listOf(
          GistFileEntity(
            fileId = "file_$id",
            gistId = id,
            filename = filename,
            type = "text/plain",
            language = "Kotlin",
            rawUrl = "https://gist.githubusercontent.com/raw/$id",
            size = 120L,
            content = "println(\"Hello from $id\")"
          )
        )
    )
  }

  @Test
  fun testGistListViewRendersItems() {
    val items =
      listOf(
        createSampleGist("gist_1", "Main.kt", "Main file description"),
        createSampleGist("gist_2", "Utils.kt", "Utils file description")
      )

    composeTestRule.setContent { GistListView(gists = items) }

    composeTestRule.onNodeWithTag("gist_list_view").assertIsDisplayed()
    composeTestRule.onNodeWithText("Main.kt").assertIsDisplayed()
    composeTestRule.onNodeWithText("Main file description").assertIsDisplayed()
    composeTestRule.onNodeWithText("Utils.kt").assertIsDisplayed()
    composeTestRule.onNodeWithText("Utils file description").assertIsDisplayed()
  }

  @Test
  fun testGistListViewDisplaysSyncedIndicator() {
    val syncedGist = createSampleGist("gist_synced", isLocalOnly = false, isDirty = false)

    composeTestRule.setContent { GistListView(gists = listOf(syncedGist)) }

    composeTestRule.onNodeWithTag("sync_status_synced", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Pushed to GitHub", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun testGistListViewDisplaysPendingIndicator() {
    val pendingGist = createSampleGist("gist_pending", isDirty = true)

    composeTestRule.setContent { GistListView(gists = listOf(pendingGist)) }

    composeTestRule.onNodeWithTag("sync_status_dirty", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Unpushed Edits", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun testGistListViewDisplaysErrorIndicator() {
    val errorGist = createSampleGist("gist_error", isLocalOnly = false, isDirty = false)

    composeTestRule.setContent {
      GistListView(gists = listOf(errorGist), errorGistIds = setOf("gist_error"))
    }

    composeTestRule.onNodeWithTag("sync_status_error", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Sync Error", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun testGistListViewEmptyState() {
    composeTestRule.setContent { GistListView(gists = emptyList()) }

    composeTestRule.onNodeWithTag("gist_list_empty_container").assertIsDisplayed()
  }

  @Test
  fun testGistListViewItemActions() {
    val gist = createSampleGist("gist_actions")
    var clickedGistId: String? = null
    var pinnedGistId: String? = null
    var starredGistId: String? = null

    composeTestRule.setContent {
      GistListView(
        gists = listOf(gist),
        onGistClick = { clickedGistId = it.gist.id },
        onTogglePin = { pinnedGistId = it },
        onToggleStar = { starredGistId = it }
      )
    }

    composeTestRule.onNodeWithTag("star_button_gist_actions").performClick()
    assertEquals("gist_actions", starredGistId)

    composeTestRule.onNodeWithTag("pin_button_gist_actions").performClick()
    assertEquals("gist_actions", pinnedGistId)

    composeTestRule.onNodeWithTag("gist_card_gist_actions").performClick()
    assertEquals("gist_actions", clickedGistId)
  }
}
