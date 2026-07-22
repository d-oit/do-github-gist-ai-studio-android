package com.example.ui.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

fun GistViewModel.loadGistHistory(gistId: String) {
  _historyError.value = null
  _isLoadingHistory.value = true
  _historyList.value = null
  viewModelScope.launch {
    repository
      .fetchRemoteGist(gistId)
      .onSuccess { details ->
        _historyList.value = details.history ?: emptyList()
        if (details.history.isNullOrEmpty()) {
          _historyError.value = "No revision history found for this Gist."
        }
      }
      .onFailure { err -> _historyError.value = "Failed to load revisions: ${err.message}" }
    _isLoadingHistory.value = false
  }
}

fun GistViewModel.selectRevision(gistId: String, sha: String?) {
  _selectedRevisionSha.value = sha
  if (sha == null) {
    _currentRevisionGist.value = null
    _parentRevisionGist.value = null
    _revisionContentError.value = null
    return
  }

  _isLoadingRevisionContent.value = true
  _revisionContentError.value = null
  _currentRevisionGist.value = null
  _parentRevisionGist.value = null

  viewModelScope.launch {
    repository
      .fetchRemoteGistRevision(gistId, sha)
      .onSuccess { curGist ->
        _currentRevisionGist.value = curGist
        val history = _historyList.value
        val currentIdx = history?.indexOfFirst { it.version == sha } ?: -1
        val parentRev =
          if (currentIdx != -1 && currentIdx + 1 < (history?.size ?: 0)) {
            history?.get(currentIdx + 1)
          } else {
            null
          }

        if (parentRev?.version != null) {
          repository
            .fetchRemoteGistRevision(gistId, parentRev.version)
            .onSuccess { parGist -> _parentRevisionGist.value = parGist }
            .onFailure { _parentRevisionGist.value = null }
        } else {
          _parentRevisionGist.value = null
        }
      }
      .onFailure { err ->
        _revisionContentError.value = "Failed to load revision files: ${err.message}"
      }
    _isLoadingRevisionContent.value = false
  }
}

fun GistViewModel.clearPreviewRevisionState() {
  _historyList.value = null
  _isLoadingHistory.value = false
  _historyError.value = null
  _selectedRevisionSha.value = null
  _currentRevisionGist.value = null
  _parentRevisionGist.value = null
  _isLoadingRevisionContent.value = false
  _revisionContentError.value = null
}
