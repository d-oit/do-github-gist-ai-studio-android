package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.data.repository.SyncStatus
import com.example.ui.components.SyncStatusDashboardView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SyncStatusDashboardTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun createSampleGist(
    id: String,
    filename: String,
    isLocalOnly: Boolean = false,
    isDirty: Boolean = false,
    isDeleted: Boolean = false
  ): GistWithFiles {
    val gist =
      GistEntity(
        id = id,
        description = "Sample Gist $id",
        htmlUrl = "https://gist.github.com/$id",
        url = "https://api.github.com/gists/$id",
        createdAt = "2026-07-28T00:00:00Z",
        updatedAt = "2026-07-28T00:00:00Z",
        nodeId = "node_$id",
        isPublic = true,
        isPinned = false,
        isLocalOnly = isLocalOnly,
        isDirty = isDirty,
        isDeleted = isDeleted,
        ownerLogin = "testuser",
        ownerId = 1,
        ownerAvatarUrl = "https://github.com/avatar.png"
      )
    val files =
      listOf(
        GistFileEntity(
          fileId = "${id}_file",
          gistId = id,
          filename = filename,
          type = "text/plain",
          language = "Kotlin",
          rawUrl = "https://gist.githubusercontent.com/$id/raw/$filename",
          size = 100L,
          content = "println(\"Hello\")"
        )
      )
    return GistWithFiles(gist = gist, files = files)
  }

  @Test
  fun syncStatusDashboard_rendersSyncedState_whenOnlineAndNoPendingChanges() {
    val gists = listOf(createSampleGist("1", "main.kt", isLocalOnly = false, isDirty = false))

    composeTestRule.setContent {
      SyncStatusDashboardView(
        gists = gists,
        isSyncing = false,
        isOnline = true,
        onSyncClick = {},
        lastSyncTime = System.currentTimeMillis(),
        syncStatus = SyncStatus.Idle
      )
    }

    composeTestRule.onNodeWithTag("sync_status_dashboard").assertIsDisplayed()
    composeTestRule.onNodeWithTag("overall_status_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("status_text_synced").assertIsDisplayed()
    composeTestRule.onNodeWithTag("metric_total_gists").assertIsDisplayed()
    composeTestRule.onNodeWithTag("metric_pending_sync").assertIsDisplayed()
    composeTestRule.onNodeWithText("0").assertIsDisplayed()
  }

  @Test
  fun syncStatusDashboard_rendersPendingState_whenLocalModificationsExist() {
    val gists =
      listOf(
        createSampleGist("1", "draft.kt", isLocalOnly = true),
        createSampleGist("2", "edited.kt", isDirty = true)
      )

    composeTestRule.setContent {
      SyncStatusDashboardView(
        gists = gists,
        isSyncing = false,
        isOnline = true,
        onSyncClick = {},
        lastSyncTime = System.currentTimeMillis(),
        syncStatus = SyncStatus.Idle
      )
    }

    composeTestRule.onNodeWithTag("sync_status_dashboard").assertIsDisplayed()
    composeTestRule.onNodeWithTag("status_text_pending").assertIsDisplayed()
    composeTestRule.onNodeWithTag("locally_modified_gist_item_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("locally_modified_gist_item_2").assertIsDisplayed()
    composeTestRule.onNodeWithText("draft.kt", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("edited.kt", substring = true).assertIsDisplayed()
  }

  @Test
  fun syncStatusDashboard_rendersOfflineState_whenDeviceIsOffline() {
    val gists = listOf(createSampleGist("1", "offline_draft.kt", isLocalOnly = true))

    composeTestRule.setContent {
      SyncStatusDashboardView(
        gists = gists,
        isSyncing = false,
        isOnline = false,
        onSyncClick = {},
        lastSyncTime = System.currentTimeMillis(),
        syncStatus = SyncStatus.Idle
      )
    }

    composeTestRule.onNodeWithTag("sync_status_dashboard").assertIsDisplayed()
    composeTestRule.onNodeWithTag("status_text_offline").assertIsDisplayed()
    composeTestRule.onNodeWithTag("metric_network_state").assertIsDisplayed()
  }
}
