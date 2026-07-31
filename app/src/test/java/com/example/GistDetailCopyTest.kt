package com.example

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.ui.screens.GistDetailScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GistDetailCopyTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun gistDetail_rendersCopyButton_andHandlesCopyClick() {
    val sampleGist =
      GistWithFiles(
        gist =
          GistEntity(
            id = "test_gist_id",
            description = "Test Gist Description",
            htmlUrl = "https://gist.github.com/test_gist_id",
            url = "https://api.github.com/gists/test_gist_id",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            nodeId = "node_123",
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
              fileId = "test_gist_id_test.kt",
              gistId = "test_gist_id",
              filename = "test.kt",
              type = "text/plain",
              language = "Kotlin",
              rawUrl = "https://gist.githubusercontent.com/test.kt",
              size = 100,
              content = "fun main() { println(\"Hello\") }"
            )
          )
      )

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
      .performScrollToNode(hasTestTag("detail_file_card_test.kt"))

    composeTestRule.onNodeWithTag("detail_file_card_test.kt", useUnmergedTree = true).assertExists()
    composeTestRule
      .onNodeWithTag("copy_file_button_test.kt", useUnmergedTree = true)
      .assertExists()
      .performClick()
  }
}
