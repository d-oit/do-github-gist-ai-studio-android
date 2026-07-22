package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.config.AppConfiguration
import com.example.data.local.dao.GistDao
import com.example.data.local.entity.GistWithFiles
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import com.example.data.repository.syncWithGitHub
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GistViewModel(
  val repository: GistRepository,
  val configPrefs: ConfigPrefs,
  val appConfiguration: AppConfiguration
) : ViewModel() {

  val gistDao: GistDao
    get() = repository.gistDao

  private val _gists = MutableStateFlow<List<GistWithFiles>>(emptyList())
  val gists: StateFlow<List<GistWithFiles>> = _gists.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedTag = MutableStateFlow<String?>(null)
  val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

  val allTags: StateFlow<List<String>> =
    repository.allGists
      .map { list -> list.flatMap { it.gist.tags }.filter { it.isNotBlank() }.distinct().sorted() }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
      )

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  val syncStatus: StateFlow<com.example.data.repository.SyncStatus> = repository.syncStatus

  // Config form states
  private val _token = MutableStateFlow(configPrefs.getGithubToken())
  val token: StateFlow<String> = _token.asStateFlow()

  private val _ownerLogin = MutableStateFlow(configPrefs.getOwnerLogin())
  val ownerLogin: StateFlow<String> = _ownerLogin.asStateFlow()

  private val _ownerAvatar = MutableStateFlow(configPrefs.getOwnerAvatarUrl())
  val ownerAvatar: StateFlow<String> = _ownerAvatar.asStateFlow()

  private val _isFetchingProfile = MutableStateFlow(false)
  val isFetchingProfile: StateFlow<Boolean> = _isFetchingProfile.asStateFlow()

  private val _tokenVerificationState =
    MutableStateFlow<TokenVerificationState>(TokenVerificationState.Idle)
  val tokenVerificationState: StateFlow<TokenVerificationState> =
    _tokenVerificationState.asStateFlow()

  private val _appTheme = MutableStateFlow(configPrefs.getTheme())
  val appTheme: StateFlow<String> = _appTheme.asStateFlow()

  // Remote direct API states
  private val _remoteGists =
    MutableStateFlow<List<com.example.data.remote.model.GistResponse>>(emptyList())
  val remoteGists: StateFlow<List<com.example.data.remote.model.GistResponse>> =
    _remoteGists.asStateFlow()

  private val _isFetchingRemote = MutableStateFlow(false)
  val isFetchingRemote: StateFlow<Boolean> = _isFetchingRemote.asStateFlow()

  private val _remoteError = MutableStateFlow<String?>(null)
  val remoteError: StateFlow<String?> = _remoteError.asStateFlow()

  init {
    viewModelScope.launch {
      combine(_searchQuery, _selectedTag) { query, tag -> query to tag }
        .collectLatest { (query, tag) ->
          val flow =
            if (query.isBlank()) {
              repository.allGists
            } else {
              repository.searchLocalGists(query.trim())
            }
          flow.collectLatest { list ->
            val filteredList =
              if (tag == null) {
                list
              } else {
                list.filter { it.gist.tags.contains(tag) }
              }
            _gists.value = filteredList
          }
        }
    }
    if (configPrefs.getGithubToken().trim().isNotEmpty()) {
      viewModelScope.launch {
        // Fetch user profile if details are anonymous or missing
        if (
          configPrefs.getOwnerLogin() == "anonymous" || configPrefs.getOwnerAvatarUrl().isEmpty()
        ) {
          repository.fetchUserProfile().onSuccess { profile ->
            val login = profile.login ?: "anonymous"
            val avatar = profile.avatarUrl ?: ""
            _ownerLogin.value = login
            _ownerAvatar.value = avatar
            configPrefs.setOwnerLogin(login)
            configPrefs.setOwnerAvatarUrl(avatar)
            profile.id?.let { configPrefs.setOwnerId(it) }
          }
        }
        // Automatically retrieve gists from remote if local database is empty on startup
        try {
          val currentGists = repository.allGists.first()
          if (currentGists.isEmpty()) {
            refreshGists()
          }
        } catch (e: Exception) {
          // Fail gracefully on startup connection issues
        }
        // Pre-populate the direct API list of remote Gists
        fetchRemoteGistsDirectly()
      }
    }
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun updateSelectedTag(tag: String?) {
    _selectedTag.value = tag
  }

  fun updateGistTags(id: String, tags: List<String>) {
    viewModelScope.launch { repository.updateGistTags(id, tags) }
  }

  fun clearMessages() {
    _statusMessage.value = null
    _errorMessage.value = null
  }

  fun updateToken(value: String) {
    _token.value = value
    _tokenVerificationState.value = TokenVerificationState.Idle
  }

  fun updateOwnerLogin(value: String) {
    _ownerLogin.value = value
  }

  fun updateOwnerAvatar(value: String) {
    _ownerAvatar.value = value
  }

  fun validateAndFetchProfile() {
    val tokenVal = _token.value.trim()
    if (tokenVal.isEmpty()) {
      _errorMessage.value = "Please enter a GitHub Personal Access Token"
      _tokenVerificationState.value =
        TokenVerificationState.Error("Please enter a GitHub Personal Access Token")
      return
    }
    viewModelScope.launch {
      _isFetchingProfile.value = true
      _tokenVerificationState.value = TokenVerificationState.Verifying
      clearMessages()
      val oldToken = configPrefs.getGithubToken()
      configPrefs.setGithubToken(tokenVal)

      val result = repository.fetchUserProfile()
      result
        .onSuccess { profile ->
          val login = profile.login ?: "anonymous"
          val avatar = profile.avatarUrl ?: ""
          _ownerLogin.value = login
          _ownerAvatar.value = avatar
          configPrefs.setGithubToken(tokenVal)
          configPrefs.setOwnerLogin(login)
          configPrefs.setOwnerAvatarUrl(avatar)
          profile.id?.let { configPrefs.setOwnerId(it) }
          _tokenVerificationState.value = TokenVerificationState.Success
          _statusMessage.value =
            "Token Verified & Saved Successfully! Connected as @${profile.login}"
        }
        .onFailure { error ->
          configPrefs.setGithubToken(oldToken)
          val errorMsg =
            "Verification failed: ${error.localizedMessage ?: "Check your token or connection"}"
          _tokenVerificationState.value = TokenVerificationState.Error(errorMsg)
          _errorMessage.value = errorMsg
        }
      _isFetchingProfile.value = false
    }
  }

  fun saveConfig() {
    configPrefs.setGithubToken(_token.value)
    configPrefs.setOwnerLogin(_ownerLogin.value)
    configPrefs.setOwnerAvatarUrl(_ownerAvatar.value)
    _statusMessage.value = "Configuration saved successfully"
  }

  fun clearConfig() {
    configPrefs.clear()
    _token.value = ""
    _ownerLogin.value = "anonymous"
    _ownerAvatar.value = ""
    _appTheme.value = "light"
    _tokenVerificationState.value = TokenVerificationState.Idle
    _statusMessage.value = "Configuration cleared"
    viewModelScope.launch {
      try {
        repository.clearAllLocalData()
      } catch (e: Exception) {
        // Log or handle any error if database clearing fails
      }
    }
  }

  fun updateAppTheme(theme: String) {
    configPrefs.setTheme(theme)
    _appTheme.value = theme
  }

  fun refreshGists() {
    viewModelScope.launch {
      _isRefreshing.value = true
      clearMessages()
      repository.updateSyncStatus(com.example.data.repository.SyncStatus.Syncing)
      val result = repository.fetchFromRemote()
      result
        .onSuccess {
          repository.updateSyncStatus(
            com.example.data.repository.SyncStatus.Success(
              "Gists updated successfully",
              System.currentTimeMillis()
            )
          )
          _statusMessage.value = "Gists updated successfully"
        }
        .onFailure { error ->
          val classified = com.example.core.error.SyncErrorHandler.classifyError(error)
          repository.updateSyncStatus(
            com.example.data.repository.SyncStatus.Error(classified, System.currentTimeMillis())
          )
          _errorMessage.value = classified
        }
      _isRefreshing.value = false
    }
  }

  private val _aiAnalysis = MutableStateFlow<com.example.ui.components.GistAiAnalysis?>(null)
  val aiAnalysis: StateFlow<com.example.ui.components.GistAiAnalysis?> = _aiAnalysis.asStateFlow()

  private val _isAnalyzingGist = MutableStateFlow(false)
  val isAnalyzingGist: StateFlow<Boolean> = _isAnalyzingGist.asStateFlow()

  fun analyzeGistContent(description: String, files: List<Pair<String, String>>) {
    viewModelScope.launch {
      _isAnalyzingGist.value = true
      _aiAnalysis.value = null
      android.util.Log.d("GistViewModel", "Triggered analyzeGistContent. File count: ${files.size}")
      try {
        val geminiKey = appConfiguration.geminiApiKeyOrNull() ?: ""
        val result =
          com.example.ui.components.LocalGistAiModel.analyzeGist(
            description = description,
            files = files,
            apiKey = geminiKey.ifBlank { null }
          )
        _aiAnalysis.value = result
      } catch (e: Exception) {
        val sanitizedError =
          com.example.core.security.PrivacySanitizer.redact(e.message ?: "Unknown error")
        android.util.Log.e("GistViewModel", "Error in analyzeGistContent: $sanitizedError", e)
      } finally {
        _isAnalyzingGist.value = false
      }
    }
  }

  fun clearAiAnalysis() {
    _aiAnalysis.value = null
  }

  fun createGist(
    description: String,
    files: List<Pair<String, String>>,
    isPublic: Boolean,
    isPinned: Boolean,
    tags: List<String> = emptyList()
  ) {
    viewModelScope.launch {
      clearMessages()
      if (files.isEmpty() || files.any { it.first.isBlank() }) {
        _errorMessage.value = "Filenames cannot be empty"
        return@launch
      }
      if (files.any { it.second.isBlank() }) {
        _errorMessage.value = "Content cannot be empty"
        return@launch
      }

      repository.createLocalDraft(
        description = description,
        files = files,
        isPublic = isPublic,
        isPinned = isPinned,
        tags = tags
      )
      clearAutoSavedDraft(null)
      _statusMessage.value = "Draft created successfully"
    }
  }

  fun createGist(
    description: String,
    filename: String,
    content: String,
    isPublic: Boolean,
    isPinned: Boolean,
    tags: List<String> = emptyList()
  ) = createGist(description, listOf(filename to content), isPublic, isPinned, tags)

  fun updateGist(
    id: String,
    description: String,
    files: List<Pair<String, String>>,
    isPublic: Boolean,
    isPinned: Boolean,
    tags: List<String> = emptyList()
  ) {
    viewModelScope.launch {
      clearMessages()
      if (files.isEmpty() || files.any { it.first.isBlank() }) {
        _errorMessage.value = "Filenames cannot be empty"
        return@launch
      }
      if (files.any { it.second.isBlank() }) {
        _errorMessage.value = "Content cannot be empty"
        return@launch
      }

      repository.updateGistLocal(
        id = id,
        description = description,
        files = files,
        isPublic = isPublic,
        isPinned = isPinned,
        tags = tags
      )
      clearAutoSavedDraft(id)
      _statusMessage.value = "Local changes saved"
    }
  }

  fun updateGist(
    id: String,
    description: String,
    filename: String,
    content: String,
    isPublic: Boolean,
    isPinned: Boolean,
    tags: List<String> = emptyList()
  ) = updateGist(id, description, listOf(filename to content), isPublic, isPinned, tags)

  fun togglePin(id: String) {
    viewModelScope.launch { repository.togglePin(id) }
  }

  fun toggleStar(id: String) {
    viewModelScope.launch {
      clearMessages()
      val result = repository.toggleStar(id)
      result.onFailure { error -> _errorMessage.value = "Star operation failed: ${error.message}" }
    }
  }

  fun deleteGist(id: String) {
    viewModelScope.launch {
      clearMessages()
      val result = repository.deleteGist(id)
      result
        .onSuccess { _statusMessage.value = "Gist deleted successfully" }
        .onFailure { error -> _errorMessage.value = "Delete failed: ${error.message}" }
    }
  }

  fun syncAll() {
    viewModelScope.launch {
      _isSyncing.value = true
      clearMessages()
      repository.updateSyncStatus(com.example.data.repository.SyncStatus.Syncing)
      val result = repository.syncWithGitHub()
      result
        .onSuccess { msg ->
          repository.updateSyncStatus(
            com.example.data.repository.SyncStatus.Success(msg, System.currentTimeMillis())
          )
          _statusMessage.value = msg
        }
        .onFailure { error ->
          val classified = com.example.core.error.SyncErrorHandler.classifyError(error)
          repository.updateSyncStatus(
            com.example.data.repository.SyncStatus.Error(classified, System.currentTimeMillis())
          )
          _errorMessage.value = classified
        }
      _isSyncing.value = false
    }
  }

  fun dismissSyncError() {
    repository.clearSyncStatus()
  }

  private val _isForking = MutableStateFlow<String?>(null)
  val isForking: StateFlow<String?> = _isForking.asStateFlow()

  fun forkGist(id: String) {
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

  fun fetchRemoteGistsDirectly() {
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

  // Gist Revision/History States for Preview Dialog
  @Suppress("VariableNaming")
  internal val _historyList =
    MutableStateFlow<List<com.example.data.remote.model.GistHistoryResponse>?>(null)
  val historyList: StateFlow<List<com.example.data.remote.model.GistHistoryResponse>?> =
    _historyList.asStateFlow()

  @Suppress("VariableNaming") internal val _isLoadingHistory = MutableStateFlow(false)
  val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

  @Suppress("VariableNaming") internal val _historyError = MutableStateFlow<String?>(null)
  val historyError: StateFlow<String?> = _historyError.asStateFlow()

  @Suppress("VariableNaming") internal val _selectedRevisionSha = MutableStateFlow<String?>(null)
  val selectedRevisionSha: StateFlow<String?> = _selectedRevisionSha.asStateFlow()

  @Suppress("VariableNaming")
  internal val _currentRevisionGist =
    MutableStateFlow<com.example.data.remote.model.GistResponse?>(null)
  val currentRevisionGist: StateFlow<com.example.data.remote.model.GistResponse?> =
    _currentRevisionGist.asStateFlow()

  @Suppress("VariableNaming")
  internal val _parentRevisionGist =
    MutableStateFlow<com.example.data.remote.model.GistResponse?>(null)
  val parentRevisionGist: StateFlow<com.example.data.remote.model.GistResponse?> =
    _parentRevisionGist.asStateFlow()

  @Suppress("VariableNaming") internal val _isLoadingRevisionContent = MutableStateFlow(false)
  val isLoadingRevisionContent: StateFlow<Boolean> = _isLoadingRevisionContent.asStateFlow()

  @Suppress("VariableNaming") internal val _revisionContentError = MutableStateFlow<String?>(null)
  val revisionContentError: StateFlow<String?> = _revisionContentError.asStateFlow()
}
