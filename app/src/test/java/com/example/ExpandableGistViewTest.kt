package com.example

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.ui.components.GistCard
import com.example.ui.screens.GistDetailScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExpandableGistViewTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val sampleGist =
    GistWithFiles(
      gist =
        GistEntity(
          id = "expandable_gist_123",
          description = "Expandable Gist Test Description",
          htmlUrl = "https://gist.github.com/expandable_gist_123",
          url = "https://api.github.com/gists/expandable_gist_123",
          createdAt = "2026-01-01T00:00:00Z",
          updatedAt = "2026-01-01T00:00:00Z",
          nodeId = "node_456",
          isPublic = true,
          isPinned = false,
          isLocalOnly = false,
          isDirty = false,
          ownerLogin = "testuser",
          ownerId = 1,
          ownerAvatarUrl = "https://avatars.githubusercontent.com/u/1"
        ),
      files =
        listOf(
          GistFileEntity(
            fileId = "expandable_gist_123_main.kt",
            gistId = "expandable_gist_123",
            filename = "main.kt",
            type = "text/plain",
            language = "Kotlin",
            rawUrl = "https://gist.githubusercontent.com/main.kt",
            size = 120,
            content = "fun main() { println(\"Decrypted Secret Code\") }"
          )
        )
    )

  @Test
  fun gistCard_expandsInlineContent_andShowsDecryptedBadge() {
    composeTestRule.setContent {
      GistCard(
        item = sampleGist,
        onTogglePin = {},
        onToggleStar = {},
        onEdit = {},
        onDelete = {},
        onPreview = {}
      )
    }

    // Verify initially expanded content does not exist
    composeTestRule
      .onNodeWithTag("expanded_content_expandable_gist_123", useUnmergedTree = true)
      .assertDoesNotExist()

    // Perform click on expand button
    composeTestRule
      .onNodeWithTag("expand_gist_button_expandable_gist_123", useUnmergedTree = true)
      .assertExists()
      .performClick()

    // Verify expanded content box and auto-decrypted badge appear
    composeTestRule
      .onNodeWithTag("expanded_content_expandable_gist_123", useUnmergedTree = true)
      .assertExists()

    composeTestRule
      .onNodeWithTag("auto_decrypted_badge_expandable_gist_123", useUnmergedTree = true)
      .assertExists()
  }

  @Test
  fun gistDetailScreen_displaysDecryptedBanner_andFullFileContent() {
    composeTestRule.setContent {
      GistDetailScreen(
        item = sampleGist,
        onBack = {},
        onEdit = {},
        onDelete = {},
        onTogglePin = {},
        onToggleStar = {}
      )
    }

    composeTestRule
      .onNodeWithTag("detail_screen_lazy_column")
      .performScrollToNode(hasTestTag("detail_decrypted_banner"))

    composeTestRule.onNodeWithTag("detail_decrypted_banner", useUnmergedTree = true).assertExists()

    composeTestRule
      .onNodeWithTag("detail_screen_lazy_column")
      .performScrollToNode(hasTestTag("detail_file_card_main.kt"))

    composeTestRule.onNodeWithTag("detail_file_card_main.kt", useUnmergedTree = true).assertExists()
  }
}
