package com.example

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.components.DataLoadingState
import com.example.ui.components.LoadingFeedbackBar
import com.example.ui.components.LoadingFeedbackBox
import com.example.ui.components.LoadingFeedbackOverlay
import com.example.ui.components.SkeletonLoadingList
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LoadingFeedbackTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun loadingFeedbackBar_rendersWhenLoading() {
    composeTestRule.setContent {
      LoadingFeedbackBar(isLoading = true, message = "Synchronizing Gists with GitHub...")
    }

    composeTestRule.onNodeWithTag("loading_feedback_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_feedback_bar_spinner").assertIsDisplayed()
    composeTestRule.onNodeWithText("Synchronizing Gists with GitHub...").assertIsDisplayed()
  }

  @Test
  fun loadingFeedbackOverlay_rendersTitleMessageAndCancel() {
    var cancelClicked = false

    composeTestRule.setContent {
      LoadingFeedbackOverlay(
        isLoading = true,
        title = "Verifying Token",
        message = "Contacting GitHub API...",
        isCancelable = true,
        onCancel = { cancelClicked = true }
      )
    }

    composeTestRule.onNodeWithTag("loading_feedback_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_feedback_overlay_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("Verifying Token").assertIsDisplayed()
    composeTestRule.onNodeWithText("Contacting GitHub API...").assertIsDisplayed()

    composeTestRule.onNodeWithTag("loading_feedback_overlay_cancel_btn").performClick()
    assertTrue(cancelClicked)
  }

  @Test
  fun skeletonLoadingList_rendersSkeletonItems() {
    composeTestRule.setContent { SkeletonLoadingList(count = 3) }

    composeTestRule.onNodeWithTag("skeleton_loading_list").assertIsDisplayed()
  }

  @Test
  fun loadingFeedbackBox_rendersSuccessContentWhenDataLoaded() {
    val state = DataLoadingState.Success("Hello World")

    composeTestRule.setContent { LoadingFeedbackBox(state = state) { text -> Text(text = text) } }

    composeTestRule.onNodeWithText("Hello World").assertIsDisplayed()
  }

  @Test
  fun loadingFeedbackBox_rendersEmptyStateWhenEmpty() {
    val state =
      DataLoadingState.Empty(title = "No Local Gists", description = "Your local cache is empty.")

    composeTestRule.setContent { LoadingFeedbackBox(state = state) { Text("Content") } }

    composeTestRule.onNodeWithTag("loading_feedback_empty_view").assertIsDisplayed()
    composeTestRule.onNodeWithText("No Local Gists").assertIsDisplayed()
    composeTestRule.onNodeWithText("Your local cache is empty.").assertIsDisplayed()
  }

  @Test
  fun loadingFeedbackBox_rendersErrorStateAndTriggersRetry() {
    var retryTriggered = false
    val state =
      DataLoadingState.Error(
        message = "Network connection timed out",
        onRetry = { retryTriggered = true }
      )

    composeTestRule.setContent { LoadingFeedbackBox(state = state) { Text("Content") } }

    composeTestRule.onNodeWithTag("loading_feedback_error_view").assertIsDisplayed()
    composeTestRule.onNodeWithText("Network connection timed out").assertIsDisplayed()
    composeTestRule.onNodeWithTag("loading_feedback_retry_btn").performClick()

    assertTrue(retryTriggered)
  }
}
