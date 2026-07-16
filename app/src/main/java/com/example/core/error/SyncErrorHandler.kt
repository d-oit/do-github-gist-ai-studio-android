package com.example.core.error

import com.example.core.security.PrivacySanitizer
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

object SyncErrorHandler {

  fun classifyError(throwable: Throwable?): String {
    if (throwable == null) return "Unknown error occurred during synchronization."

    val message = throwable.message ?: ""

    return when (throwable) {
      is UnknownHostException -> {
        "Network Error: Unable to connect to GitHub. Please check your internet connection."
      }
      is SocketTimeoutException -> {
        "Network Timeout: The connection to GitHub timed out. Please try again."
      }
      is IOException -> {
        "Connection Failure: A network error occurred during synchronization."
      }
      is HttpException -> {
        classifyHttpError(throwable.code(), message)
      }
      else -> {
        classifyStringError(message)
      }
    }
  }

  fun classifyError(message: String?): String {
    if (message == null || message.isBlank()) {
      return "An unknown API error occurred during synchronization."
    }
    return classifyStringError(message)
  }

  private fun classifyHttpError(code: Int, message: String): String {
    return when (code) {
      401 ->
        "Authentication Failed: Your GitHub Personal Access Token (PAT) is invalid or expired. Please verify it on the Config screen."
      403 -> {
        if (message.contains("rate limit", ignoreCase = true)) {
          "API Limit Reached: GitHub API rate limit exceeded. Please wait a few minutes before retrying."
        } else {
          "Access Denied: You do not have permission to modify this Gist. Please verify your token permissions."
        }
      }
      404 -> "Not Found: The requested Gist could not be found on GitHub. It may have been deleted."
      409 ->
        "Conflict Detected: Local edits conflict with remote modifications. Please refresh your Gists to reconcile."
      in 500..599 ->
        "GitHub Server Error: The GitHub Gist service is temporarily experiencing issues. Please try again later."
      else -> "API Error ($code): ${PrivacySanitizer.redact(message)}"
    }
  }

  private fun classifyStringError(message: String): String {
    val lower = message.lowercase()
    return when {
      lower.contains("token", ignoreCase = true) &&
        (lower.contains("invalid", ignoreCase = true) ||
          lower.contains("bad credential", ignoreCase = true)) -> {
        "Authentication Failed: Your GitHub token is invalid or expired. Please update your PAT on the Config screen."
      }
      lower.contains("token is not configured") -> {
        "Configuration Missing: No GitHub Personal Access Token configured. Please add one on the Config screen."
      }
      lower.contains("rate limit", ignoreCase = true) || lower.contains("403") -> {
        "API Limit Reached: GitHub API rate limit exceeded. Please try again later."
      }
      lower.contains("401") || lower.contains("unauthorized", ignoreCase = true) -> {
        "Authentication Failed: Invalid or expired GitHub Personal Access Token."
      }
      lower.contains("404") || lower.contains("not found", ignoreCase = true) -> {
        "Not Found: One or more Gists could not be found on GitHub."
      }
      lower.contains("500") ||
        lower.contains("502") ||
        lower.contains("503") ||
        lower.contains("504") -> {
        "GitHub Server Error: The GitHub Gist service is currently down or unreachable."
      }
      lower.contains("timeout", ignoreCase = true) ||
        lower.contains("timed out", ignoreCase = true) -> {
        "Network Timeout: The request to GitHub timed out. Please check your connection."
      }
      lower.contains("unknownhost", ignoreCase = true) ||
        lower.contains("unable to resolve host", ignoreCase = true) -> {
        "Network Error: Unable to resolve GitHub API. Please check your internet connection."
      }
      else -> "Sync Error: ${PrivacySanitizer.redact(message)}"
    }
  }
}
