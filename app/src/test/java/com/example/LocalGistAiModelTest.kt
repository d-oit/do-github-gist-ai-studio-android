package com.example

import com.example.ui.components.LocalGistAiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalGistAiModelTest {

  @Test
  fun test_analyzeOffline_detectsKotlinAndCompose() {
    val description = "My Android Compose Gist"
    val files = listOf(
      "MainScreen.kt" to """
        package com.example
        import androidx.compose.runtime.Composable
        @Composable
        fun MainScreen() {
          // compose view code
        }
      """.trimIndent()
    )

    val result = LocalGistAiModel.analyzeOffline(description, files)
    
    assertTrue(result.recommendedTags.contains("#Kotlin"))
    assertTrue(result.recommendedTags.contains("#Compose"))
    assertFalse(result.isOnlineGenerated)
    assertEquals("Low", result.complexityLevel)
    assertEquals("Excellent", result.maintainabilityIndex)
  }

  @Test
  fun test_analyzeOffline_detectsComposeWithoutRemember() {
    val description = "Compose Missing Remember"
    val files = listOf(
      "Screen.kt" to """
        package com.example
        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.mutableStateOf
        @Composable
        fun Screen() {
          val state = mutableStateOf("bad")
        }
      """.trimIndent()
    )

    val result = LocalGistAiModel.analyzeOffline(description, files)
    
    val hasRememberWarning = result.optimizationSuggestions.any {
      it.contains("mutableStateOf") && it.contains("remember")
    }
    assertTrue("Should suggest adding 'remember'", hasRememberWarning)
  }

  @Test
  fun test_analyzeOffline_detectsHardcodedSecrets() {
    val description = "Secret Leakage"
    val files = listOf(
      "config.py" to """
        API_KEY = "ghp_abcdefghijklmnopqrstuvwxyz0123456789"
      """.trimIndent()
    )

    val result = LocalGistAiModel.analyzeOffline(description, files)
    
    val hasSecretWarning = result.optimizationSuggestions.any {
      it.contains("Security Warn") && it.contains("secrets")
    }
    assertTrue("Should suggest removing secrets", hasSecretWarning)
  }

  @Test
  fun test_analyzeOffline_detectsLogAndPrintStatements() {
    val description = "Logs"
    val files = listOf(
      "Helper.kt" to """
        fun helper() {
          println("I am printing some debug logs")
          Log.d("Helper", "debug message")
        }
      """.trimIndent()
    )

    val result = LocalGistAiModel.analyzeOffline(description, files)
    
    val hasPrintWarning = result.optimizationSuggestions.any {
      it.contains("debug print/log")
    }
    assertTrue("Should suggest removing prints", hasPrintWarning)
  }

  @Test
  fun test_analyzeOffline_detectsFilesWithoutExtension() {
    val description = "No Extension"
    val files = listOf(
      "Dockerfile" to "FROM openjdk:17",
      "helper_script" to "echo hello"
    )

    val result = LocalGistAiModel.analyzeOffline(description, files)
    
    val hasExtensionWarning = result.optimizationSuggestions.any {
      it.contains("Syntax Highlighting") && it.contains("extension")
    }
    assertTrue("Should suggest adding file extension", hasExtensionWarning)
  }

  @Test
  fun test_analyzeGist_gracefullyFallsBack_onGeminiFailure() = runTest {
    val description = "Gemini failure test"
    val files = listOf("test.txt" to "Simple file content")
    
    val result = LocalGistAiModel.analyzeGist(description, files, "AIzaSy_fake_invalid_key_for_testing")
    
    assertFalse(result.isOnlineGenerated)
    assertTrue(result.recommendedTags.contains("#Snippets"))
  }

  @Test
  fun test_analyzeGist_withBlankApiKey_runsOfflineInstantly() = runTest {
    val description = "Blank API key test"
    val files = listOf("test.txt" to "Simple file content")
    
    val result = LocalGistAiModel.analyzeGist(description, files, "")
    
    assertFalse(result.isOnlineGenerated)
    assertTrue(result.recommendedTags.contains("#Snippets"))
  }
}
