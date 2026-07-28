package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.repository.SyncStatus
import com.example.ui.components.SyncStatusBar
import com.example.ui.components.formatLastSyncTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SyncStatusBarTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun formatLastSyncTime_returnsNeverSynced_whenZeroOrNegative() {
    assertEquals("Never synced", formatLastSyncTime(0L))
    assertEquals("Never synced", formatLastSyncTime(-100L))
  }

  @Test
  fun formatLastSyncTime_returnsRelativeRanges() {
    val now = 1_700_000_000_000L // arbitrary base time

    assertEquals("Just now", formatLastSyncTime(now - 10_000L, now))
    assertEquals("5m ago", formatLastSyncTime(now - (5 * 60 * 1000L), now))
    assertEquals("2h ago", formatLastSyncTime(now - (2 * 3600 * 1000L), now))
    assertEquals("3d ago", formatLastSyncTime(now - (3 * 86400 * 1000L), now))
  }

  @Test
  fun syncStatusBar_rendersSuccessState() {
    val time = System.currentTimeMillis() - 120_000L // 2m ago

    composeTestRule.setContent {
      SyncStatusBar(lastSyncTime = time, syncStatus = SyncStatus.Success("Synced", time))
    }

    composeTestRule.onNodeWithTag("sync_status_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("last_synced_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("sync_status_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("Last Synced: 2m ago", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Synced", substring = false).assertIsDisplayed()
  }

  @Test
  fun syncStatusBar_rendersSyncingState() {
    val time = System.currentTimeMillis()

    composeTestRule.setContent {
      SyncStatusBar(lastSyncTime = time, syncStatus = SyncStatus.Syncing)
    }

    composeTestRule.onNodeWithTag("sync_status_bar").assertIsDisplayed()
    composeTestRule.onNodeWithText("Syncing...", substring = true).assertIsDisplayed()
  }

  @Test
  fun syncStatusBar_rendersErrorState() {
    val time = System.currentTimeMillis()

    composeTestRule.setContent {
      SyncStatusBar(lastSyncTime = time, syncStatus = SyncStatus.Error("Network error", time))
    }

    composeTestRule.onNodeWithTag("sync_status_bar").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sync Error", substring = true).assertIsDisplayed()
  }

  @Test
  fun syncStatusBar_rendersNeverSyncedState() {
    composeTestRule.setContent { SyncStatusBar(lastSyncTime = 0L, syncStatus = SyncStatus.Idle) }

    composeTestRule.onNodeWithTag("sync_status_bar").assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Last Synced: Never synced", substring = true)
      .assertIsDisplayed()
    composeTestRule.onNodeWithText("Not Synced", substring = true).assertIsDisplayed()
  }
}
