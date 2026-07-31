package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.components.GistListEmptyState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GistListEmptyStateTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun gistListEmptyState_displaysTitleMessageAndPrimaryCta() {
    composeTestRule.setContent { GistListEmptyState(onFetchClick = {}) }

    composeTestRule.onNodeWithTag("gist_list_empty_state").assertIsDisplayed()
    composeTestRule.onNodeWithTag("empty_state_title").assertIsDisplayed()
    composeTestRule.onNodeWithText("Your Gist Library is Empty").assertIsDisplayed()
    composeTestRule.onNodeWithTag("empty_state_message").assertIsDisplayed()
    composeTestRule.onNodeWithTag("fetch_from_github_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("empty_state_helper_text").assertIsDisplayed()
  }

  @Test
  fun gistListEmptyState_triggersFetchClickOnCtaTap() {
    var fetchClicked = false

    composeTestRule.setContent { GistListEmptyState(onFetchClick = { fetchClicked = true }) }

    composeTestRule.onNodeWithTag("fetch_from_github_btn").performClick()
    assertTrue(fetchClicked)
  }

  @Test
  fun gistListEmptyState_showsRefreshingIndicatorWhenSyncing() {
    composeTestRule.setContent { GistListEmptyState(onFetchClick = {}, isRefreshing = true) }

    composeTestRule.onNodeWithTag("fetch_from_github_btn").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("empty_state_progress_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("Fetching Gists...").assertIsDisplayed()
  }

  @Test
  fun gistListEmptyState_showsCreateDraftButtonWhenCallbackProvided() {
    var draftClicked = false

    composeTestRule.setContent {
      GistListEmptyState(onFetchClick = {}, onCreateDraftClick = { draftClicked = true })
    }

    composeTestRule.onNodeWithTag("empty_state_create_draft_btn").assertIsDisplayed()
    composeTestRule.onNodeWithText("Create Local Draft").assertIsDisplayed()
    composeTestRule.onNodeWithTag("empty_state_create_draft_btn").performClick()
    assertTrue(draftClicked)
  }
}
