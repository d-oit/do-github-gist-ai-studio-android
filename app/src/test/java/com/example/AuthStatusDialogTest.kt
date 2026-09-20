package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.ui.components.AuthStatusDialog
import com.example.ui.components.AuthStatusIndicatorChip
import com.example.ui.viewmodel.TokenVerificationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AuthStatusDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun authStatusChip_displaysNoPatWhenTokenEmpty() {
    composeTestRule.setContent {
      AuthStatusIndicatorChip(token = "", ownerLogin = "anonymous", onClick = {})
    }

    composeTestRule.onNodeWithTag("auth_status_chip").assertIsDisplayed()
    composeTestRule.onNodeWithText("No PAT").assertIsDisplayed()
  }

  @Test
  fun authStatusChip_displaysUsernameWhenTokenPresent() {
    composeTestRule.setContent {
      AuthStatusIndicatorChip(token = "ghp_123456789", ownerLogin = "octocat", onClick = {})
    }

    composeTestRule.onNodeWithTag("auth_status_chip").assertIsDisplayed()
    composeTestRule.onNodeWithText("@octocat").assertIsDisplayed()
  }

  @Test
  fun authStatusChip_triggersOnClickWhenTapped() {
    var clicked = false
    composeTestRule.setContent {
      AuthStatusIndicatorChip(
        token = "ghp_123456789",
        ownerLogin = "octocat",
        onClick = { clicked = true }
      )
    }

    composeTestRule.onNodeWithTag("auth_status_chip").performClick()
    assertTrue(clicked)
  }

  @Test
  fun authStatusDialog_displaysStatusInfoAndButtons() {
    composeTestRule.setContent {
      AuthStatusDialog(
        show = true,
        token = "ghp_valid_token_123",
        ownerLogin = "octocat",
        verificationState = TokenVerificationState.Idle,
        isVerifying = false,
        onUpdateToken = {},
        onVerifyToken = {},
        onClearToken = {},
        onDismiss = {}
      )
    }

    composeTestRule.onNodeWithTag("auth_status_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("GitHub PAT Authentication").assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_status_info_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("Authenticated").assertIsDisplayed()
    composeTestRule.onNodeWithText("Linked as @octocat").assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_token_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_save_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_clear_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_close_button").assertIsDisplayed()
  }

  @Test
  fun authStatusDialog_updatesTokenAndTriggersVerification() {
    var updatedToken = ""
    var verifyTriggered = false

    composeTestRule.setContent {
      AuthStatusDialog(
        show = true,
        token = "",
        ownerLogin = "anonymous",
        verificationState = TokenVerificationState.Idle,
        isVerifying = false,
        onUpdateToken = { updatedToken = it },
        onVerifyToken = { verifyTriggered = true },
        onClearToken = {},
        onDismiss = {}
      )
    }

    composeTestRule.onNodeWithTag("auth_token_input").performTextInput("ghp_new_secret_token")
    assertEquals("ghp_new_secret_token", updatedToken)

    composeTestRule.onNodeWithTag("auth_save_button").performClick()
    assertTrue(verifyTriggered)
  }

  @Test
  fun authStatusDialog_triggersClearAndDismiss() {
    var cleared = false
    var dismissed = false

    composeTestRule.setContent {
      AuthStatusDialog(
        show = true,
        token = "ghp_some_token",
        ownerLogin = "octocat",
        verificationState = TokenVerificationState.Idle,
        isVerifying = false,
        onUpdateToken = {},
        onVerifyToken = {},
        onClearToken = { cleared = true },
        onDismiss = { dismissed = true }
      )
    }

    composeTestRule.onNodeWithTag("auth_clear_button").performClick()
    assertTrue(cleared)

    composeTestRule.onNodeWithTag("auth_close_button").performClick()
    assertTrue(dismissed)
  }
}
