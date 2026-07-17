package com.example

import com.example.core.security.PrivacySanitizer
import org.junit.Assert.assertEquals
import org.junit.Test

class PrivacySanitizerTest {

  @Test
  fun redactNullReturnsEmptyString() {
    assertEquals("", PrivacySanitizer.redact(null))
  }

  @Test
  fun redactEmptyStringReturnsEmptyString() {
    assertEquals("", PrivacySanitizer.redact(""))
  }

  @Test
  fun redactNormalTextReturnsUnchanged() {
    val input = "This is a normal log message without any secrets."
    assertEquals(input, PrivacySanitizer.redact(input))
  }

  @Test
  fun redactGithubPat() {
    val pat = "ghp_1234567890abcdefghijklmnopqrstuvwxyz"
    val input = "Log with PAT: $pat"
    val expected = "Log with PAT: [REDACTED_PAT]"
    assertEquals(expected, PrivacySanitizer.redact(input))
  }

  @Test
  fun redactBearerAuthorizationHeader() {
    val input = "Authorization: Bearer ghp_12345"
    val expected = "Authorization: Bearer [REDACTED]"
    assertEquals(expected, PrivacySanitizer.redact(input))
  }

  @Test
  fun redactGeminiApiKey() {
    val key = "AIzaSyAz1234567890-_abcde_FGHIJKLMNOPQRST"
    val input = "Using gemini key: $key"
    val expected = "Using gemini key: [REDACTED_GEMINI_KEY]"
    assertEquals(expected, PrivacySanitizer.redact(input))
  }
}
