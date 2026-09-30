package com.example.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.data.repository.deleteGistComment
import com.example.data.repository.fetchGistComments
import com.example.data.repository.postGistComment
import kotlinx.coroutines.launch

fun GistViewModel.loadComments(gistId: String) {
  viewModelScope.launch {
    _isLoadingComments.value = true
    _commentsError.value = null
    val result = repository.fetchGistComments(gistId)
    result
      .onSuccess { comments -> _commentsList.value = comments }
      .onFailure { error -> _commentsError.value = error.message ?: "Failed to load comments" }
    _isLoadingComments.value = false
  }
}

fun GistViewModel.postComment(gistId: String, body: String, onComplete: () -> Unit = {}) {
  if (body.isBlank()) return
  viewModelScope.launch {
    _isPostingComment.value = true
    _commentsError.value = null
    val result = repository.postGistComment(gistId, body)
    result
      .onSuccess { newComment ->
        _commentsList.value = _commentsList.value + newComment
        onComplete()
      }
      .onFailure { error -> _commentsError.value = error.message ?: "Failed to post comment" }
    _isPostingComment.value = false
  }
}

fun GistViewModel.deleteComment(gistId: String, commentId: Long) {
  viewModelScope.launch {
    _commentsError.value = null
    val result = repository.deleteGistComment(gistId, commentId)
    result
      .onSuccess { _commentsList.value = _commentsList.value.filter { it.id != commentId } }
      .onFailure { error -> _commentsError.value = error.message ?: "Failed to delete comment" }
  }
}

fun GistViewModel.clearCommentsState() {
  _commentsList.value = emptyList()
  _commentsError.value = null
  _isLoadingComments.value = false
  _isPostingComment.value = false
}
