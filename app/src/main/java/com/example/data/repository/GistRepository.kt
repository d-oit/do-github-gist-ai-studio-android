package com.example.data.repository

import com.example.data.local.dao.GistDao
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.data.local.pref.ConfigPrefs
import com.example.data.remote.api.GitHubApiService
import com.example.data.remote.model.GistFileRequest
import com.example.data.remote.model.GistRequest
import com.example.data.remote.model.GistResponse
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GistRepository @Inject constructor(
    private val gistDao: GistDao,
    private val apiService: GitHubApiService,
    private val configPrefs: ConfigPrefs
) {
    val allGists: Flow<List<GistWithFiles>> = gistDao.observeAllGists()
    val unsynchronizedGists: Flow<List<GistWithFiles>> = gistDao.observeUnsynchronizedGists()

    fun searchLocalGists(query: String): Flow<List<GistWithFiles>> {
        return gistDao.searchLocalGists(query)
    }

    fun observeGist(id: String): Flow<GistWithFiles?> {
        return gistDao.observeGistById(id)
    }

    suspend fun getGist(id: String): GistWithFiles? {
        return gistDao.getGistById(id)
    }

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
                val fullGist = try {
                    apiService.getGist(id)
                } catch (e: Exception) {
                    response // fallback to shallow info if details fail
                }

                // Check pinned state from previous local cache to preserve it
                val previousLocal = gistDao.getGistById(id)
                val isPinned = previousLocal?.gist?.isPinned ?: false

                saveResponseToDb(fullGist, isPinned = isPinned, isLocalOnly = false, isDirty = false)
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
        isPinned: Boolean
    ): String {
        val tempId = "draft_" + UUID.randomUUID().toString()
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        
        val gistEntity = GistEntity(
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
            ownerLogin = configPrefs.getOwnerLogin(),
            ownerId = configPrefs.getOwnerId(),
            ownerAvatarUrl = configPrefs.getOwnerAvatarUrl()
        )

        val fileEntities = files.map { (name, content) ->
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
        isPinned: Boolean
    ) {
        val previous = gistDao.getGistById(id) ?: return
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        val gistEntity = previous.gist.copy(
            description = description,
            isPublic = isPublic,
            isPinned = isPinned,
            isDirty = !previous.gist.isLocalOnly, // only dirty if it was already synced
            updatedAt = now
        )

        val fileEntities = files.map { (name, content) ->
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

    suspend fun syncWithGitHub(): Result<String> {
        if (configPrefs.getGithubToken().trim().isEmpty()) {
            return Result.failure(Exception("GitHub token is not configured"))
        }
        val unsynced = gistDao.getUnsynchronizedGists()
        if (unsynced.isEmpty()) {
            return Result.success("Already fully synchronized")
        }

        var successCount = 0
        var failCount = 0

        for (item in unsynced) {
            try {
                if (item.gist.isDeleted) {
                    // Delete existing Gist on GitHub
                    val response = apiService.deleteGist(item.gist.id)
                    if (response.isSuccessful || response.code() == 404) {
                        gistDao.deleteGistById(item.gist.id)
                        successCount++
                    } else {
                        failCount++
                    }
                } else if (item.gist.isLocalOnly) {
                    // Create new gist on GitHub
                    val fileRequests = item.files.associate { file ->
                        file.filename to GistFileRequest(content = file.content)
                    }
                    val request = GistRequest(
                        description = item.gist.description,
                        isPublic = item.gist.isPublic,
                        files = fileRequests
                    )
                    val response = apiService.createGist(request)
                    
                    // Delete temporary local draft, save the synchronized response
                    gistDao.deleteGistById(item.gist.id)
                    saveResponseToDb(response, isPinned = item.gist.isPinned, isLocalOnly = false, isDirty = false)
                    successCount++
                } else if (item.gist.isDirty) {
                    // Update existing gist on GitHub
                    val fileRequests = item.files.associate { file ->
                        file.filename to GistFileRequest(content = file.content)
                    }
                    val request = GistRequest(
                        description = item.gist.description,
                        isPublic = item.gist.isPublic,
                        files = fileRequests
                    )
                    val response = apiService.updateGist(item.gist.id, request)
                    saveResponseToDb(response, isPinned = item.gist.isPinned, isLocalOnly = false, isDirty = false)
                    successCount++
                }
            } catch (e: Exception) {
                failCount++
            }
        }

        // Fetch latest gists to align state
        fetchFromRemote()

        return if (failCount == 0) {
            Result.success("Successfully synchronized $successCount items")
        } else {
            Result.success("Synchronized $successCount items ($failCount failed)")
        }
    }

    private suspend fun saveResponseToDb(
        response: GistResponse,
        isPinned: Boolean,
        isLocalOnly: Boolean,
        isDirty: Boolean
    ) {
        val id = response.id ?: return
        val entity = GistEntity(
            id = id,
            description = response.description,
            htmlUrl = response.htmlUrl ?: "",
            url = response.url ?: "",
            createdAt = response.createdAt ?: "",
            updatedAt = response.updatedAt ?: "",
            nodeId = response.nodeId ?: "",
            isPublic = response.isPublic ?: true,
            isPinned = isPinned,
            isLocalOnly = isLocalOnly,
            isDirty = isDirty,
            isDeleted = false,
            ownerLogin = response.owner?.login ?: configPrefs.getOwnerLogin(),
            ownerId = response.owner?.id ?: configPrefs.getOwnerId(),
            ownerAvatarUrl = response.owner?.avatarUrl ?: configPrefs.getOwnerAvatarUrl()
        )

        val fileEntities = response.files?.map { (name, file) ->
            GistFileEntity(
                fileId = UUID.randomUUID().toString(),
                gistId = id,
                filename = name,
                type = file.type ?: "text/plain",
                language = file.language ?: detectLanguage(name),
                rawUrl = file.rawUrl ?: "",
                size = file.size ?: 0L,
                content = file.content ?: ""
            )
        } ?: emptyList()

        gistDao.upsertGistWithFiles(entity, fileEntities)
    }

    private fun detectLanguage(filename: String): String {
        return when {
            filename.endsWith(".kt") -> "Kotlin"
            filename.endsWith(".java") -> "Java"
            filename.endsWith(".py") -> "Python"
            filename.endsWith(".js") -> "JavaScript"
            filename.endsWith(".ts") -> "TypeScript"
            filename.endsWith(".json") -> "JSON"
            filename.endsWith(".xml") -> "XML"
            filename.endsWith(".html") -> "HTML"
            filename.endsWith(".css") -> "CSS"
            filename.endsWith(".md") -> "Markdown"
            else -> "Text"
        }
    }
}
