package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.config.AppConfiguration
import com.example.data.local.dao.GistDao
import com.example.data.local.entity.GistBackupFile
import com.example.data.local.entity.GistBackupItem
import com.example.data.local.entity.GistBackupPayload
import com.example.data.local.entity.GistWithFiles
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
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
  private val repository: GistRepository,
  private val configPrefs: ConfigPrefs,
  private val appConfiguration: AppConfiguration
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
      val result = repository.fetchFromRemote()
      result
        .onSuccess { _statusMessage.value = "Gists updated successfully" }
        .onFailure { error ->
          _errorMessage.value = "Refresh failed: ${error.localizedMessage ?: "Unknown error"}"
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
      val geminiKey = appConfiguration.geminiApiKeyOrNull() ?: ""

      val result =
        com.example.ui.components.LocalGistAiModel.analyzeGist(
          description = description,
          files = files,
          apiKey = geminiKey.ifBlank { null }
        )
      _aiAnalysis.value = result
      _isAnalyzingGist.value = false
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
  ) {
    createGist(description, listOf(filename to content), isPublic, isPinned, tags)
  }

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
  ) {
    updateGist(id, description, listOf(filename to content), isPublic, isPinned, tags)
  }

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
      val result = repository.syncWithGitHub()
      result
        .onSuccess { msg -> _statusMessage.value = msg }
        .onFailure { error ->
          _errorMessage.value = "Sync failed: ${error.localizedMessage ?: "Unknown error"}"
        }
      _isSyncing.value = false
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

  suspend fun getRemoteGistDetails(id: String): Result<com.example.data.remote.model.GistResponse> {
    return repository.fetchRemoteGist(id)
  }

  suspend fun getRemoteGistRevision(
    id: String,
    sha: String
  ): Result<com.example.data.remote.model.GistResponse> {
    return repository.fetchRemoteGistRevision(id, sha)
  }

  fun exportBackup(
    context: Context,
    uri: Uri,
    ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
    onResult: (Boolean, String) -> Unit
  ) {
    viewModelScope.launch(ioDispatcher) {
      try {
        val localGists = gists.value
        val backupItems =
          localGists.map { item ->
            GistBackupItem(
              id = item.gist.id,
              description = item.gist.description,
              htmlUrl = item.gist.htmlUrl,
              url = item.gist.url,
              createdAt = item.gist.createdAt,
              updatedAt = item.gist.updatedAt,
              isPublic = item.gist.isPublic,
              isPinned = item.gist.isPinned,
              isLocalOnly = item.gist.isLocalOnly,
              isDirty = item.gist.isDirty,
              isDeleted = item.gist.isDeleted,
              isStarred = item.gist.isStarred,
              isStarredDirty = item.gist.isStarredDirty,
              ownerLogin = item.gist.ownerLogin,
              ownerId = item.gist.ownerId,
              ownerAvatarUrl = item.gist.ownerAvatarUrl,
              files =
                item.files.map { file ->
                  GistBackupFile(
                    filename = file.filename,
                    content = file.content,
                    type = file.type,
                    language = file.language,
                    size = file.size
                  )
                }
            )
          }

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val exportedAt = sdf.format(Date())

        val payload =
          GistBackupPayload(backupVersion = 1, exportedAt = exportedAt, gists = backupItems)

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(GistBackupPayload::class.java).indent("  ")
        val jsonString = adapter.toJson(payload)

        if (uri.scheme == "file") {
          val file = java.io.File(uri.path ?: throw Exception("Invalid file path"))
          file.outputStream().use { outputStream ->
            outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
          }
        } else {
          context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
          } ?: throw Exception("Failed to open output stream")
        }

        launch(Dispatchers.Main) { onResult(true, "Backup completed successfully") }
      } catch (e: Exception) {
        launch(Dispatchers.Main) {
          onResult(false, e.localizedMessage ?: e.message ?: "Failed to save backup")
        }
      }
    }
  }

  fun getAutoSavedDraft(editingGistId: String?): com.example.data.local.pref.AutoSavedDraft? {
    return configPrefs.getAutoSavedDraft(editingGistId)
  }

  fun saveAutoSavedDraft(
    editingGistId: String?,
    description: String,
    files: List<Pair<String, String>>,
    isPublic: Boolean,
    isPinned: Boolean,
    tags: List<String>
  ) {
    val draftFiles = files.map { com.example.data.local.pref.DraftFile(it.first, it.second) }
    val draft =
      com.example.data.local.pref.AutoSavedDraft(
        editingGistId = editingGistId,
        description = description,
        files = draftFiles,
        isPublic = isPublic,
        isPinned = isPinned,
        tags = tags,
        timestamp = System.currentTimeMillis()
      )
    configPrefs.saveAutoSavedDraft(draft)
  }

  fun clearAutoSavedDraft(editingGistId: String?) {
    configPrefs.clearAutoSavedDraft(editingGistId)
  }

  fun detectLanguage(filename: String): String {
    return repository.detectLanguage(filename)
  }

  class Factory(
    private val repository: GistRepository,
    private val configPrefs: ConfigPrefs,
    private val appConfiguration: AppConfiguration
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(GistViewModel::class.java)) {
        return GistViewModel(repository, configPrefs, appConfiguration) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class")
    }
  }
}
