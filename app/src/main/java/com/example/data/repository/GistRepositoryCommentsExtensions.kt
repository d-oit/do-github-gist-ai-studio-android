package com.example.data.repository

import com.example.data.remote.model.GistCommentRequest
import com.example.data.remote.model.GistCommentResponse

suspend fun GistRepository.fetchGistComments(gistId: String): Result<List<GistCommentResponse>> {
  val token = configPrefs.getGithubToken().trim()
  if (token.isEmpty()) {
    return Result.failure(Exception("GitHub token is not configured"))
  }
  return try {
    val comments = apiService.getGistComments(gistId)
    Result.success(comments)
  } catch (e: Exception) {
    Result.failure(e)
  }
}

suspend fun GistRepository.postGistComment(
  gistId: String,
  body: String
): Result<GistCommentResponse> {
  val token = configPrefs.getGithubToken().trim()
  if (token.isEmpty()) {
    return Result.failure(Exception("GitHub token is not configured"))
  }
  if (body.isBlank()) {
    return Result.failure(IllegalArgumentException("Comment body cannot be blank"))
  }
  return try {
    val comment = apiService.createGistComment(gistId, GistCommentRequest(body.trim()))
    Result.success(comment)
  } catch (e: Exception) {
    Result.failure(e)
  }
}

suspend fun GistRepository.deleteGistComment(gistId: String, commentId: Long): Result<Unit> {
  val token = configPrefs.getGithubToken().trim()
  if (token.isEmpty()) {
    return Result.failure(Exception("GitHub token is not configured"))
  }
  return try {
    val response = apiService.deleteGistComment(gistId, commentId)
    if (response.isSuccessful || response.code() == 204) {
      Result.success(Unit)
    } else {
      Result.failure(Exception("Failed to delete comment: HTTP ${response.code()}"))
    }
  } catch (e: Exception) {
    Result.failure(e)
  }
}
