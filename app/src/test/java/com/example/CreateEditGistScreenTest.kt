package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.ui.screens.CreateEditGistScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CreateEditGistScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun createEditGistScreen_rendersCreationFields_andEncryptionCard() {
    composeTestRule.setContent {
      CreateEditGistScreen(existingGist = null, onBack = {}, onSave = { _, _, _, _, _, _ -> })
    }

    composeTestRule.onNodeWithTag("create_edit_gist_screen").assertExists()
    composeTestRule.onNodeWithTag("create_edit_encryption_card").assertExists()
    composeTestRule.onNodeWithTag("create_edit_encryption_badge").assertExists()
    composeTestRule.onNodeWithTag("create_edit_title_input").assertExists()
    composeTestRule.onNodeWithTag("create_edit_description_input").assertExists()
    composeTestRule.onNodeWithTag("create_edit_content_input").assertExists()
    composeTestRule.onNodeWithTag("create_edit_save_button").assertExists()
  }

  @Test
  fun createEditGistScreen_triggersOnSaveWithFilledValues() {
    var savedTitle = ""
    var savedDescription = ""
    var savedContent = ""
    var savedIsPublic = false
    var isSaved = false

    composeTestRule.setContent {
      CreateEditGistScreen(
        existingGist = null,
        onBack = {},
        onSave = { title, description, content, isPublic, _, _ ->
          savedTitle = title
          savedDescription = description
          savedContent = content
          savedIsPublic = isPublic
          isSaved = true
        }
      )
    }

    composeTestRule.onNodeWithTag("create_edit_title_input").performTextClearance()
    composeTestRule.onNodeWithTag("create_edit_title_input").performTextInput("security_snippet.kt")

    composeTestRule
      .onNodeWithTag("create_edit_description_input")
      .performTextInput("AES Encryption Test Gist")

    composeTestRule
      .onNodeWithTag("create_edit_content_input")
      .performTextInput("val secret = \"EncryptedValue\"")

    composeTestRule.onNodeWithTag("create_edit_save_button").performClick()

    assertTrue("onSave should be triggered", isSaved)
    assertEquals("security_snippet.kt", savedTitle)
    assertEquals("AES Encryption Test Gist", savedDescription)
    assertEquals("val secret = \"EncryptedValue\"", savedContent)
    assertTrue("Default isPublic should be true", savedIsPublic)
  }

  @Test
  fun createEditGistScreen_loadsExistingGistForEditing() {
    val existing =
      GistWithFiles(
        gist =
          GistEntity(
            id = "existing_123",
            description = "Existing Security Description",
            htmlUrl = "",
            url = "",
            createdAt = "",
            updatedAt = "",
            nodeId = "",
            isPublic = false,
            isPinned = false,
            isLocalOnly = true,
            isDirty = false,
            ownerLogin = "test",
            ownerId = 1,
            ownerAvatarUrl = ""
          ),
        files =
          listOf(
            GistFileEntity(
              fileId = "existing_123_f1",
              gistId = "existing_123",
              filename = "existing_code.kt",
              type = "text/plain",
              language = "Kotlin",
              rawUrl = "",
              size = 50,
              content = "fun existing() = true"
            )
          )
      )

    composeTestRule.setContent {
      CreateEditGistScreen(existingGist = existing, onBack = {}, onSave = { _, _, _, _, _, _ -> })
    }

    composeTestRule.onNodeWithTag("create_edit_gist_screen").assertExists()
    composeTestRule.onNodeWithTag("create_edit_title_input").assertExists()
    composeTestRule.onNodeWithTag("create_edit_description_input").assertExists()
    composeTestRule.onNodeWithTag("create_edit_content_input").assertExists()
  }
}
