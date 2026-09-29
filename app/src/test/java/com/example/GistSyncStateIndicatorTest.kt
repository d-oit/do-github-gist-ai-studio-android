package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.ui.components.GistSyncState
import com.example.ui.components.GistSyncStateBanner
import com.example.ui.components.GistSyncStateIndicator
import com.example.ui.components.SyncStatusChip
import com.example.ui.components.SyncStatusIcon
import com.example.ui.components.SyncStatusType
import com.example.ui.components.resolveGistSyncState
import com.example.ui.components.resolveSyncStatusType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GistSyncStateIndicatorTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testResolveGistSyncStateLogic() {
    assertEquals(
      GistSyncState.LOCAL_ONLY,
      resolveGistSyncState(isLocalOnly = true, isDirty = false, isDeleted = false)
    )
    assertEquals(
      GistSyncState.DIRTY,
      resolveGistSyncState(isLocalOnly = false, isDirty = true, isDeleted = false)
    )
    assertEquals(
      GistSyncState.PENDING_DELETE,
      resolveGistSyncState(isLocalOnly = false, isDirty = false, isDeleted = true)
    )
    assertEquals(
      GistSyncState.SYNCED,
      resolveGistSyncState(isLocalOnly = false, isDirty = false, isDeleted = false)
    )
    assertEquals(
      GistSyncState.ERROR,
      resolveGistSyncState(isLocalOnly = false, isDirty = false, isDeleted = false, hasError = true)
    )
  }

  @Test
  fun testResolveSyncStatusTypeLogic() {
    assertEquals(
      SyncStatusType.SYNCED,
      resolveSyncStatusType(isLocalOnly = false, isDirty = false, isDeleted = false)
    )
    assertEquals(
      SyncStatusType.PENDING,
      resolveSyncStatusType(isLocalOnly = true, isDirty = false, isDeleted = false)
    )
    assertEquals(
      SyncStatusType.PENDING,
      resolveSyncStatusType(isLocalOnly = false, isDirty = true, isDeleted = false)
    )
    assertEquals(
      SyncStatusType.ERROR,
      resolveSyncStatusType(
        isLocalOnly = false,
        isDirty = false,
        isDeleted = false,
        hasError = true
      )
    )
  }

  @Test
  fun testCompactIndicatorDisplaysLocalOnlyState() {
    composeTestRule.setContent {
      GistSyncStateIndicator(isLocalOnly = true, isDirty = false, isDeleted = false, compact = true)
    }

    composeTestRule.onNodeWithTag("sync_status_local_only").assertIsDisplayed()
    composeTestRule.onNodeWithText("Local Only").assertIsDisplayed()
  }

  @Test
  fun testCompactIndicatorDisplaysDirtyState() {
    composeTestRule.setContent {
      GistSyncStateIndicator(isLocalOnly = false, isDirty = true, isDeleted = false, compact = true)
    }

    composeTestRule.onNodeWithTag("sync_status_dirty").assertIsDisplayed()
    composeTestRule.onNodeWithText("Unpushed Edits").assertIsDisplayed()
  }

  @Test
  fun testCompactIndicatorDisplaysSyncedState() {
    composeTestRule.setContent {
      GistSyncStateIndicator(
        isLocalOnly = false,
        isDirty = false,
        isDeleted = false,
        compact = true
      )
    }

    composeTestRule.onNodeWithTag("sync_status_synced").assertIsDisplayed()
    composeTestRule.onNodeWithText("Pushed to GitHub").assertIsDisplayed()
  }

  @Test
  fun testCompactIndicatorDisplaysErrorState() {
    composeTestRule.setContent {
      GistSyncStateIndicator(
        isLocalOnly = false,
        isDirty = false,
        isDeleted = false,
        hasError = true,
        compact = true
      )
    }

    composeTestRule.onNodeWithTag("sync_status_error").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sync Error").assertIsDisplayed()
  }

  @Test
  fun testThreeStateSyncStatusChip() {
    composeTestRule.setContent { SyncStatusChip(status = SyncStatusType.PENDING) }

    composeTestRule.onNodeWithTag("sync_status_pending").assertIsDisplayed()
    composeTestRule.onNodeWithText("Pending Sync").assertIsDisplayed()
  }

  @Test
  fun testSyncStatusIcons() {
    composeTestRule.setContent {
      SyncStatusIcon(status = SyncStatusType.SYNCED)
      SyncStatusIcon(status = SyncStatusType.PENDING)
      SyncStatusIcon(status = SyncStatusType.ERROR)
    }

    composeTestRule.onNodeWithTag("sync_status_icon_synced").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sync_status_icon_pending").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sync_status_icon_error").assertIsDisplayed()
  }

  @Test
  fun testDetailedBannerDisplaysStateInfo() {
    composeTestRule.setContent { GistSyncStateBanner(syncState = GistSyncState.DIRTY) }

    composeTestRule.onNodeWithTag("gist_sync_state_banner_dirty").assertIsDisplayed()
    composeTestRule.onNodeWithText("Unpushed Edits").assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Modified locally. Changes pending push to GitHub.")
      .assertIsDisplayed()
  }
}
