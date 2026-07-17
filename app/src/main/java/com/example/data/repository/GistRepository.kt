package com.example.data.repository

import com.example.data.local.dao.GistDao
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.data.local.pref.ConfigPrefs
import com.example.data.remote.api.GitHubApiService
import com.example.data.remote.model.GistResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class GistRepository(
  val gistDao: GistDao,
  val apiService: GitHubApiService,
  val configPrefs: ConfigPrefs
) {
  private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
  val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

  init {
    val lastError = configPrefs.getLastSyncError()
    val lastTime = configPrefs.getLastSyncTime()
    if (lastError != null) {
      _syncStatus.value = SyncStatus.Error(lastError, lastTime)
    } else if (lastTime > 0) {
      _syncStatus.value = SyncStatus.Success("Synced", lastTime)
    }
  }

  fun updateSyncStatus(status: SyncStatus) {
    _syncStatus.value = status
    when (status) {
      is SyncStatus.Error -> {
        configPrefs.setLastSyncError(status.errorMessage)
        configPrefs.setLastSyncTime(status.timestamp)
      }
      is SyncStatus.Success -> {
        configPrefs.setLastSyncError(null)
        configPrefs.setLastSyncTime(status.timestamp)
      }
      else -> {}
    }
  }

  fun clearSyncStatus() {
    _syncStatus.value = SyncStatus.Idle
    configPrefs.setLastSyncError(null)
  }

  val allGists: Flow<List<GistWithFiles>> =
    gistDao.observeAllGists().map { list -> list.filter { !it.gist.isDeleted } }
  val unsynchronizedGists: Flow<List<GistWithFiles>> = gistDao.observeUnsynchronizedGists()

  suspend fun getUnsynchronizedGists(): List<GistWithFiles> = gistDao.getUnsynchronizedGists()

  fun searchLocalGists(query: String): Flow<List<GistWithFiles>> =
    gistDao.searchLocalGists(query).map { list -> list.filter { !it.gist.isDeleted } }

  fun observeGist(id: String): Flow<GistWithFiles?> = gistDao.observeGistById(id)

  suspend fun getGist(id: String): GistWithFiles? = gistDao.getGistById(id)

  suspend fun updateGistTags(id: String, tags: List<String>) = gistDao.updateTags(id, tags)

  suspend fun fetchUserProfile(): Result<com.example.data.remote.model.GistOwnerResponse> {
    val token = configPrefs.getGithubToken().trim()
    if (token.isEmpty()) {
      return Result.failure(Exception("GitHub token is not configured"))
    }
    return try {
      val userProfile = apiService.getAuthenticatedUser()
      userProfile.login?.let { configPrefs.setOwnerLogin(it) }
      userProfile.avatarUrl?.let { configPrefs.setOwnerAvatarUrl(it) }
      userProfile.id?.let { configPrefs.setOwnerId(it) }
      Result.success(userProfile)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun fetchGistsDirectly(): Result<List<com.example.data.remote.model.GistResponse>> {
    val token = configPrefs.getGithubToken().trim()
    if (token.isEmpty()) {
      return Result.failure(Exception("GitHub token is not configured"))
    }
    return try {
      val responses = apiService.getGists()
      Result.success(responses)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun fetchRemoteGist(id: String): Result<com.example.data.remote.model.GistResponse> {
    val token = configPrefs.getGithubToken().trim()
    if (token.isEmpty()) {
      return Result.failure(Exception("GitHub token is not configured"))
    }
    return try {
      val response = apiService.getGist(id)
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun fetchRemoteGistRevision(
    id: String,
    sha: String
  ): Result<com.example.data.remote.model.GistResponse> {
    val token = configPrefs.getGithubToken().trim()
    if (token.isEmpty()) {
      return Result.failure(Exception("GitHub token is not configured"))
    }
    return try {
      val response = apiService.getGistRevision(id, sha)
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun fetchFromRemote(): Result<Unit> {
    if (configPrefs.getGithubToken().trim().isEmpty()) {
      return Result.failure(Exception("GitHub token is not configured"))
    }
    return try {
      val responses = apiService.getGists()

      // Collect existing dirty / local states to avoid overwriting them
      val localUnsynced = gistDao.getUnsynchronizedGists()
      val unsyncedIds = localUnsynced.map { it.gist.id }.toSet()

      for (response in responses) {
        val id = response.id ?: continue
        if (unsyncedIds.contains(id)) continue

        // Fetch details for each gist to get full content of files
        val fullGist =
          try {
            apiService.getGist(id)
          } catch (e: Exception) {
            response // fallback to shallow info if details fail
          }

        // Check pinned state from previous local cache to preserve it
        val previousLocal = gistDao.getGistById(id)
        val isPinned = previousLocal?.gist?.isPinned ?: false

        val isStarred =
          if (previousLocal?.gist?.isStarredDirty == true) {
            previousLocal.gist.isStarred
          } else {
            try {
              apiService.checkIsStarred(id).code() == 204
            } catch (e: Exception) {
              previousLocal?.gist?.isStarred ?: false
            }
          }
        val isStarredDirty = previousLocal?.gist?.isStarredDirty ?: false

        saveResponseToDb(
          response = fullGist,
          isPinned = isPinned,
          isLocalOnly = false,
          isDirty = false,
          isStarred = isStarred,
          isStarredDirty = isStarredDirty
        )
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun fetchFromRemotePaginated(page: Int, perPage: Int): Result<Unit> {
    if (configPrefs.getGithubToken().trim().isEmpty()) {
      return Result.failure(Exception("GitHub token is not configured"))
    }
    return try {
      val responses = apiService.getGists(page = page, perPage = perPage)

      // Collect existing dirty / local states to avoid overwriting them
      val localUnsynced = gistDao.getUnsynchronizedGists()
      val unsyncedIds = localUnsynced.map { it.gist.id }.toSet()

      for (response in responses) {
        val id = response.id ?: continue
        if (unsyncedIds.contains(id)) continue

        val fullGist =
          try {
            apiService.getGist(id)
          } catch (e: Exception) {
            response
          }

        val previousLocal = gistDao.getGistById(id)
        val isPinned = previousLocal?.gist?.isPinned ?: false

        val isStarred =
          if (previousLocal?.gist?.isStarredDirty == true) {
            previousLocal.gist.isStarred
          } else {
            try {
              apiService.checkIsStarred(id).code() == 204
            } catch (e: Exception) {
              previousLocal?.gist?.isStarred ?: false
            }
          }
        val isStarredDirty = previousLocal?.gist?.isStarredDirty ?: false

        saveResponseToDb(
          response = fullGist,
          isPinned = isPinned,
          isLocalOnly = false,
          isDirty = false,
          isStarred = isStarred,
          isStarredDirty = isStarredDirty
        )
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun createLocalDraft(
    description: String,
    files: List<Pair<String, String>>, // filename to content
    isPublic: Boolean,
    isPinned: Boolean,
    tags: List<String> = emptyList()
  ): String {
    val tempId = "draft_" + UUID.randomUUID().toString()
    val now =
      SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        .format(Date())

    val gistEntity =
      GistEntity(
        id = tempId,
        description = description,
        htmlUrl = "",
        url = "",
        createdAt = now,
        updatedAt = now,
        nodeId = "",
        isPublic = isPublic,
        isPinned = isPinned,
        isLocalOnly = true,
        isDirty = false,
        tags = tags,
        ownerLogin = configPrefs.getOwnerLogin(),
        ownerId = configPrefs.getOwnerId(),
        ownerAvatarUrl = configPrefs.getOwnerAvatarUrl()
      )

    val fileEntities =
      files.map { (name, content) ->
        GistFileEntity(
          fileId = UUID.randomUUID().toString(),
          gistId = tempId,
          filename = name,
          type = "text/plain",
          language = detectLanguage(name),
          rawUrl = "",
          size = content.length.toLong(),
          content = content
        )
      }

    gistDao.upsertGistWithFiles(gistEntity, fileEntities)
    return tempId
  }

  suspend fun updateGistLocal(
    id: String,
    description: String,
    files: List<Pair<String, String>>,
    isPublic: Boolean,
    isPinned: Boolean,
    tags: List<String> = emptyList()
  ) {
    val previous = gistDao.getGistById(id) ?: return
    val now =
      SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        .format(Date())

    val gistEntity =
      previous.gist.copy(
        description = description,
        isPublic = isPublic,
        isPinned = isPinned,
        isDirty = !previous.gist.isLocalOnly, // only dirty if it was already synced
        tags = tags,
        updatedAt = now
      )

    val fileEntities =
      files.map { (name, content) ->
        GistFileEntity(
          fileId = UUID.randomUUID().toString(),
          gistId = id,
          filename = name,
          type = "text/plain",
          language = detectLanguage(name),
          rawUrl = "",
          size = content.length.toLong(),
          content = content
        )
      }

    gistDao.upsertGistWithFiles(gistEntity, fileEntities)
  }

  suspend fun togglePin(id: String) {
    val previous = gistDao.getGistById(id) ?: return
    val updated = previous.gist.copy(isPinned = !previous.gist.isPinned)
    gistDao.insertGist(updated)
  }

  suspend fun toggleStar(id: String): Result<Unit> {
    val previous = gistDao.getGistById(id) ?: return Result.failure(Exception("Gist not found"))
    val newStarred = !previous.gist.isStarred

    // 1. Immediately reflect state changes locally
    val updated =
      previous.gist.copy(isStarred = newStarred, isStarredDirty = !previous.gist.isLocalOnly)
    gistDao.insertGist(updated)

    if (previous.gist.isLocalOnly) {
      return Result.success(Unit)
    }

    // 2. Push to GitHub if network is available
    val token = configPrefs.getGithubToken().trim()
    if (token.isEmpty()) {
      return Result.success(Unit)
    }

    return try {
      val response =
        if (newStarred) {
          apiService.starGist(id)
        } else {
          apiService.unstarGist(id)
        }
      if (response.isSuccessful || response.code() == 204) {
        val synced = updated.copy(isStarredDirty = false)
        gistDao.insertGist(synced)
        Result.success(Unit)
      } else {
        Result.failure(Exception("Failed to update star on GitHub: ${response.message()}"))
      }
    } catch (e: Exception) {
      // Network failure: we keep isStarredDirty = true so it syncs later
      Result.success(Unit)
    }
  }

  suspend fun deleteGist(id: String): Result<Unit> {
    val gist = gistDao.getGistById(id) ?: return Result.failure(Exception("Gist not found"))
    if (gist.gist.isLocalOnly) {
      gistDao.deleteGistById(id)
      return Result.success(Unit)
    }

    val token = configPrefs.getGithubToken().trim()
    if (token.isEmpty()) {
      // Offline delete: flag as pending deletion locally
      val updated = gist.gist.copy(isDeleted = true)
      gistDao.insertGist(updated)
      return Result.success(Unit)
    }

    return try {
      val response = apiService.deleteGist(id)
      if (response.isSuccessful || response.code() == 404) {
        gistDao.deleteGistById(id)
        Result.success(Unit)
      } else {
        Result.failure(Exception("Failed to delete from GitHub: ${response.message()}"))
      }
    } catch (e: Exception) {
      // Network failure: flag as pending deletion locally
      val updated = gist.gist.copy(isDeleted = true)
      gistDao.insertGist(updated)
      Result.success(Unit)
    }
  }

  suspend fun forkGist(id: String): Result<Unit> {
    if (configPrefs.getGithubToken().trim().isEmpty()) {
      return Result.failure(Exception("GitHub token is not configured"))
    }
    return try {
      val response = apiService.forkGist(id)
      val newId = response.id ?: return Result.failure(Exception("Forked Gist response has no ID"))

      // Fetch details for the newly created fork to get the full contents of the files
      val fullGist =
        try {
          apiService.getGist(newId)
        } catch (e: Exception) {
          response
        }

      saveResponseToDb(
        response = fullGist,
        isPinned = false,
        isLocalOnly = false,
        isDirty = false,
        isStarred = false,
        isStarredDirty = false
      )
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun clearAllLocalData() {
    gistDao.clearAllData()
  }

  fun detectLanguage(filename: String): String {
    return LanguageDetector.detectLanguage(filename)
  }
}
