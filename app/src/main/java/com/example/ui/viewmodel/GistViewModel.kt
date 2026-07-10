package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.DoGistHubApp
import com.example.data.local.entity.GistWithFiles
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GistViewModel(
    private val repository: GistRepository,
    private val configPrefs: ConfigPrefs
) : ViewModel() {

    private val _gists = MutableStateFlow<List<GistWithFiles>>(emptyList())
    val gists: StateFlow<List<GistWithFiles>> = _gists.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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

    private val _appTheme = MutableStateFlow(configPrefs.getTheme())
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery.collectLatest { query ->
                val flow = if (query.isBlank()) {
                    repository.allGists
                } else {
                    repository.searchLocalGists(query.trim())
                }
                flow.collectLatest { list ->
                    _gists.value = list
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
    }

    fun updateToken(value: String) {
        _token.value = value
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
            return
        }
        viewModelScope.launch {
            _isFetchingProfile.value = true
            clearMessages()
            val oldToken = configPrefs.getGithubToken()
            configPrefs.setGithubToken(tokenVal)
            
            val result = repository.fetchUserProfile()
            result.onSuccess { profile ->
                val login = profile.login ?: "anonymous"
                val avatar = profile.avatarUrl ?: ""
                _ownerLogin.value = login
                _ownerAvatar.value = avatar
                configPrefs.setGithubToken(tokenVal)
                configPrefs.setOwnerLogin(login)
                configPrefs.setOwnerAvatarUrl(avatar)
                profile.id?.let { configPrefs.setOwnerId(it) }
                _statusMessage.value = "Successfully authenticated! Connected as @${profile.login}"
            }.onFailure { error ->
                configPrefs.setGithubToken(oldToken)
                _errorMessage.value = "Authentication failed: ${error.localizedMessage ?: "Check your token or connection"}"
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
        _statusMessage.value = "Configuration cleared"
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
            result.onSuccess {
                _statusMessage.value = "Gists updated successfully"
            }.onFailure { error ->
                _errorMessage.value = "Refresh failed: ${error.localizedMessage ?: "Unknown error"}"
            }
            _isRefreshing.value = false
        }
    }

    fun createGist(
        description: String,
        filename: String,
        content: String,
        isPublic: Boolean,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            clearMessages()
            if (filename.isBlank()) {
                _errorMessage.value = "Filename cannot be empty"
                return@launch
            }
            if (content.isBlank()) {
                _errorMessage.value = "Content cannot be empty"
                return@launch
            }
            
            repository.createLocalDraft(
                description = description,
                files = listOf(filename to content),
                isPublic = isPublic,
                isPinned = isPinned
            )
            _statusMessage.value = "Draft created successfully"
        }
    }

    fun updateGist(
        id: String,
        description: String,
        filename: String,
        content: String,
        isPublic: Boolean,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            clearMessages()
            if (filename.isBlank()) {
                _errorMessage.value = "Filename cannot be empty"
                return@launch
            }
            if (content.isBlank()) {
                _errorMessage.value = "Content cannot be empty"
                return@launch
            }

            repository.updateGistLocal(
                id = id,
                description = description,
                files = listOf(filename to content),
                isPublic = isPublic,
                isPinned = isPinned
            )
            _statusMessage.value = "Local changes saved"
        }
    }

    fun togglePin(id: String) {
        viewModelScope.launch {
            repository.togglePin(id)
        }
    }

    fun deleteGist(id: String) {
        viewModelScope.launch {
            clearMessages()
            val result = repository.deleteGist(id)
            result.onSuccess {
                _statusMessage.value = "Gist deleted successfully"
            }.onFailure { error ->
                _errorMessage.value = "Delete failed: ${error.message}"
            }
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            _isSyncing.value = true
            clearMessages()
            val result = repository.syncWithGitHub()
            result.onSuccess { msg ->
                _statusMessage.value = msg
            }.onFailure { error ->
                _errorMessage.value = "Sync failed: ${error.localizedMessage ?: "Unknown error"}"
            }
            _isSyncing.value = false
        }
    }

    class Factory(
        private val repository: GistRepository,
        private val configPrefs: ConfigPrefs
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GistViewModel::class.java)) {
                return GistViewModel(repository, configPrefs) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
