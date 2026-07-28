package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.ui.screens.HomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PullToRefreshHomeScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun homeScreen_rendersPullRefreshIndicatorWhenRefreshing() {
    composeTestRule.setContent {
      HomeScreen(
        gists = emptyList(),
        searchQuery = "",
        onSearchQueryChange = {},
        isRefreshing = true,
        onRefresh = {},
        onTogglePin = {},
        onToggleStar = {},
        onEdit = {},
        onDelete = {},
        onPreview = {}
      )
    }

    composeTestRule.onNodeWithTag("pull_refresh_indicator").assertIsDisplayed()
  }

  @Test
  fun homeScreen_pullRefreshIndicatorStateWhenNotRefreshing() {
    composeTestRule.setContent {
      HomeScreen(
        gists = emptyList(),
        searchQuery = "",
        onSearchQueryChange = {},
        isRefreshing = false,
        onRefresh = {},
        onTogglePin = {},
        onToggleStar = {},
        onEdit = {},
        onDelete = {},
        onPreview = {}
      )
    }

    composeTestRule.onNodeWithTag("pull_refresh_indicator").assertExists()
  }
}
