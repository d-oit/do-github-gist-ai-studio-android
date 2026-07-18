package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.DraftEditorDialog
import com.example.ui.components.GistAiAnalysis
import com.example.ui.components.GistAiAssistantCardView
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w800dp-h1600dp", sdk = [36])
class DraftEditorAiSuggestionsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val mockAiAnalysis = GistAiAnalysis(
    summary = "Provides Jetpack Compose layout snippets with typical debug statements.",
    complexityScore = 5,
    complexityLevel = "Medium",
    maintainabilityIndex = "Good",
    recommendedTags = listOf("#Kotlin", "#Compose", "#Networking"),
    optimizationSuggestions = listOf(
      "Remove debug print/log (found Log.d/println)",
      "mutableStateOf should be wrapped in remember",
      "One or more files do not have an extension"
    ),
    isOnlineGenerated = false
  )

  @Test
  fun test_gistAiAssistantCardView_standaloneCallbacks() {
    var appendedTag = ""
    var appendAllCalled = false
    var applyFixesCalled = false

    composeTestRule.setContent {
      MyApplicationTheme {
        GistAiAssistantCardView(
          isAnalyzing = false,
          aiAnalysis = mockAiAnalysis,
          onAnalyzeClick = {},
          onClearAiClick = {},
          onAppendRecommendedTag = { appendedTag = it },
          onAppendAllTags = { appendAllCalled = true },
          onApplyFixes = { applyFixesCalled = true }
        )
      }
    }

    // Verify card rendered
    composeTestRule.onNodeWithText("Gist AI Assistant").assertExists()

    // Tap #Compose tag button
    composeTestRule.onNodeWithText("#Compose").performClick()
    assertEquals("#Compose", appendedTag)

    // Tap "Append All"
    composeTestRule.onNodeWithTag("append_all_tags_btn").performClick()
    assertTrue(appendAllCalled)

    // Tap "Apply AI Fixes & Recommendations"
    composeTestRule.onNodeWithTag("apply_ai_fixes_btn").performClick()
    assertTrue(applyFixesCalled)
  }

  @Test
  fun test_individualTagAppend_addsToTagsFieldWithoutHashAndAvoidsDuplicates() {
    composeTestRule.setContent {
      MyApplicationTheme {
        DraftEditorDialog(
          show = true,
          editingGistId = null,
          onDismiss = {},
          onSave = { _, _, _, _, _ -> },
          initialTags = listOf("initial"),
          isAnalyzing = false,
          aiAnalysis = mockAiAnalysis
        )
      }
    }

    // Verify initial tags
    composeTestRule.onNodeWithTag("editor_tags").assertTextContains("initial")

    // Tap #Compose tag button
    composeTestRule.onNodeWithText("#Compose").performClick()

    // Verify it is appended cleanly without hash
    composeTestRule.onNodeWithTag("editor_tags").assertTextContains("initial, Compose")

    // Tap #Compose again to verify duplicate prevention
    composeTestRule.onNodeWithText("#Compose").performClick()
    composeTestRule.onNodeWithTag("editor_tags").assertTextContains("initial, Compose")

    // Tap #Kotlin tag button
    composeTestRule.onNodeWithText("#Kotlin").performClick()
    composeTestRule.onNodeWithTag("editor_tags").assertTextContains("initial, Compose, Kotlin")
  }

  @Test
  fun test_appendAllTags_addsAllCleanTagsAtOnce() {
    composeTestRule.setContent {
      MyApplicationTheme {
        DraftEditorDialog(
          show = true,
          editingGistId = null,
          onDismiss = {},
          onSave = { _, _, _, _, _ -> },
          initialTags = listOf("initial", "Compose"),
          isAnalyzing = false,
          aiAnalysis = mockAiAnalysis
        )
      }
    }

    // Tap "Append All" button
    composeTestRule.onNodeWithTag("append_all_tags_btn").performClick()

    // Compose was already there, so only Kotlin and Networking should be appended
    composeTestRule.onNodeWithTag("editor_tags").assertTextContains("initial, Compose, Kotlin, Networking")
  }

  @Test
  fun test_applyAiFixes_optimizesCodeAndAppendsAllTags() {
    var savedFiles = emptyList<Pair<String, String>>()
    var savedTags = emptyList<String>()

    val initialFiles = listOf(
      "UnfinishedClass" to """
        fun debugLog() {
          println("Entering function")
          Log.d("TAG", "processing")
          val value = mutableStateOf("test")
        }
      """.trimIndent()
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        DraftEditorDialog(
          show = true,
          editingGistId = null,
          onDismiss = {},
          onSave = { _, files, _, _, tags ->
            savedFiles = files
            savedTags = tags
          },
          initialFiles = initialFiles,
          initialTags = listOf("existing"),
          isAnalyzing = false,
          aiAnalysis = mockAiAnalysis
        )
      }
    }

    // Verify file name has no extension initially
    composeTestRule.onNodeWithText("UnfinishedClass").assertExists()

    // Tap "Apply AI Fixes & Recommendations"
    composeTestRule.onNodeWithTag("apply_ai_fixes_btn").performScrollTo().performClick()

    // Verify tags are appended
    composeTestRule.onNodeWithTag("editor_tags").performScrollTo().assertTextContains("existing, Kotlin, Compose, Networking")

    // Click "Save Draft" to capture updated state
    composeTestRule.onNodeWithText("Save Draft").performClick()

    // Assert files are modified according to recommendations:
    // 1. Extension fix applied (since it has Composable/fun -> .kt)
    // 2. println / Log.d removed
    // 3. mutableStateOf wrapped in remember
    val savedFileMap = savedFiles.toMap()
    assertTrue(savedFileMap.containsKey("UnfinishedClass.kt"))
    assertFalse(savedFileMap.containsKey("UnfinishedClass"))

    val content = savedFileMap["UnfinishedClass.kt"] ?: ""
    println("DEBUG - ACTUAL PROCESSED CONTENT:\n$content")
    assertFalse(content.contains("println"))
    assertFalse(content.contains("Log.d"))
    assertTrue(content.contains("remember { mutableStateOf(\"test\") }"))
  }
}
