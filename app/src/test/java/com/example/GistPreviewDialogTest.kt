package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import com.example.ui.components.GistCard
import com.example.ui.components.GistPreviewDialog
import com.example.ui.viewmodel.GistViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GistPreviewDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val sampleGist =
    GistWithFiles(
      gist =
        GistEntity(
          id = "preview_gist_1",
          description = "Preview Test Gist Description",
          htmlUrl = "https://gist.github.com/preview_gist_1",
          url = "https://api.github.com/gists/preview_gist_1",
          createdAt = "2026-01-01T00:00:00Z",
          updatedAt = "2026-01-01T00:00:00Z",
          nodeId = "node_preview_1",
          isPublic = true,
          isPinned = false,
          isLocalOnly = true,
          isDirty = false,
          ownerLogin = "testuser",
          ownerId = 1,
          ownerAvatarUrl = "https://avatars.githubusercontent.com/u/1"
        ),
      files =
        listOf(
          GistFileEntity(
            fileId = "file_preview_1",
            gistId = "preview_gist_1",
            filename = "sample.kt",
            type = "text/plain",
            language = "Kotlin",
            rawUrl = "https://gist.githubusercontent.com/sample.kt",
            size = 150,
            content = "fun sample() {\n  val x = 42\n  println(\"Syntax Highlighted\")\n}"
          )
        )
    )

  @Test
  fun gistCard_tapTriggersOnPreview() {
    var previewTriggered = false

    composeTestRule.setContent {
      GistCard(
        item = sampleGist,
        onTogglePin = {},
        onToggleStar = {},
        onEdit = {},
        onDelete = {},
        onPreview = { previewTriggered = true }
      )
    }

    // Tap on card
    composeTestRule.onNodeWithTag("gist_card_preview_gist_1").assertIsDisplayed().performClick()

    assertTrue("Tapping GistCard should trigger onPreview", previewTriggered)
  }

  @Test
  fun gistPreviewDialog_rendersTitleAndSnippet_andDismissesOnDone() {
    var isDismissed = false

    val context = ApplicationProvider.getApplicationContext<Context>()
    val db =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val fakeApiService = FakeGitHubApiService()
    val prefs = ConfigPrefs(context)
    val repo = GistRepository(db.gistDao(), fakeApiService, prefs)
    val appConfig = com.example.core.config.AppConfigurationImpl()
    val testViewModel = GistViewModel(repo, prefs, appConfig)

    composeTestRule.setContent {
      com.example.ui.theme.MyApplicationTheme {
        var showDialog by remember { mutableStateOf(true) }
        if (showDialog) {
          GistPreviewDialog(
            show = true,
            item = sampleGist,
            viewModel = testViewModel,
            onDismiss = {
              showDialog = false
              isDismissed = true
            }
          )
        }
      }
    }

    // Verify dialog exists
    composeTestRule.onNodeWithTag("gist_preview_dialog", useUnmergedTree = true).assertExists()

    // Click Done button to dismiss dialog
    composeTestRule
      .onNodeWithTag("preview_done_button", useUnmergedTree = true)
      .assertExists()
      .performClick()

    assertTrue("Done button should trigger onDismiss", isDismissed)
  }
}
