package com.example.core.security

object PrivacySanitizer {
  private val PATTERN_GITHUB_PAT = Regex("ghp_[a-zA-Z0-9]{36}")
  private val PATTERN_BEARER_AUTH = Regex("(?i)Authorization:\\s*Bearer\\s+[a-zA-Z0-9._-]+")
  private val PATTERN_GEMINI_KEY = Regex("(?i)AIzaSy[a-zA-Z0-9_-]{35}")

  /**
   * Sanitizes sensitive information (like personal access tokens or API keys) from error messages
   * or logs.
   */
  fun redact(input: String?): String {
    if (input == null) return ""
    var redacted = input
    redacted = redacted.replace(PATTERN_GITHUB_PAT, "[REDACTED_PAT]")
    redacted = redacted.replace(PATTERN_BEARER_AUTH, "Authorization: Bearer [REDACTED]")
    redacted = redacted.replace(PATTERN_GEMINI_KEY, "[REDACTED_GEMINI_KEY]")
    return redacted
  }
}
