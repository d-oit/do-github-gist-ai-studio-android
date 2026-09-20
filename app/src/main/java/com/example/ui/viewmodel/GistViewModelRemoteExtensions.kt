package com.example.ui.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

fun GistViewModel.forkGist(id: String) {
  if (isOfflineOnly.value) {
    _errorMessage.value = "Cannot fork while in Offline-Only mode."
    return
  }
  viewModelScope.launch {
    _isForking.value = id
    repository
      .forkGist(id)
      .onSuccess {
        repository.updateSyncStatus(
          com.example.data.repository.SyncStatus.Success(
            "Successfully forked and saved locally!",
            System.currentTimeMillis()
          )
        )
        fetchRemoteGistsDirectly()
      }
      .onFailure { error ->
        val classified = com.example.core.error.SyncErrorHandler.classifyError(error)
        repository.updateSyncStatus(
          com.example.data.repository.SyncStatus.Error(classified, System.currentTimeMillis())
        )
      }
    _isForking.value = null
  }
}

fun GistViewModel.fetchRemoteGistsDirectly() {
  if (isOfflineOnly.value) {
    _remoteError.value = "Offline-Only mode is enabled."
    return
  }
  viewModelScope.launch {
    _isFetchingRemote.value = true
    _remoteError.value = null
    repository
      .fetchGistsDirectly()
      .onSuccess { list -> _remoteGists.value = list }
      .onFailure { error ->
        _remoteError.value = error.localizedMessage ?: error.message ?: "Unknown error"
      }
    _isFetchingRemote.value = false
  }
}
