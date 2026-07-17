package com.example.data.repository

import com.example.data.remote.model.GistFileRequest
import com.example.data.remote.model.GistRequest
import com.example.data.remote.model.GistResponse

suspend fun GistRepository.syncWithGitHub(): Result<String> {
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
        // Check if there is an existing remote/synced Gist that matches this description and files
        val syncedGists = gistDao.getSyncedGists()
        val matchingGist =
          syncedGists.find { synced ->
            val descMatches =
              (synced.gist.description ?: "").trim() == (item.gist.description ?: "").trim()
            val filesMatch =
              if (synced.files.size == item.files.size) {
                val syncedFilesMap = synced.files.associate { it.filename to it.content }
                item.files.all { localFile ->
                  syncedFilesMap[localFile.filename] == localFile.content
                }
              } else false
            descMatches && filesMatch
          }

        if (matchingGist != null) {
          // Graceful deduplication merge: merge pinning & starring status
          val updatedSyncedGist =
            matchingGist.gist.copy(
              isPinned = item.gist.isPinned || matchingGist.gist.isPinned,
              isStarred = item.gist.isStarred || matchingGist.gist.isStarred
            )
          gistDao.deleteGistById(item.gist.id)
          gistDao.insertGist(updatedSyncedGist)
          successCount++
        } else {
          // Create new gist on GitHub
          val fileRequests =
            item.files.associate { file ->
              file.filename to GistFileRequest(content = file.content)
            }
          val request =
            GistRequest(
              description = item.gist.description,
              isPublic = item.gist.isPublic,
              files = fileRequests
            )
          val response = apiService.createGist(request)

          // If also starred locally, push that star
          if (item.gist.isStarred) {
            try {
              apiService.starGist(response.id!!)
            } catch (e: Exception) {}
          }

          // Delete temporary local draft, save the synchronized response
          gistDao.deleteGistById(item.gist.id)
          saveResponseToDb(
            response = response,
            isPinned = item.gist.isPinned,
            isLocalOnly = false,
            isDirty = false,
            isStarred = item.gist.isStarred,
            isStarredDirty = false
          )
          successCount++
        }
      } else {
        // It already exists on GitHub, check if there are edits (isDirty) or star modifications
        // (isStarredDirty)
        var isStarred = item.gist.isStarred
        var isStarredDirty = item.gist.isStarredDirty

        if (item.gist.isStarredDirty) {
          try {
            val response =
              if (item.gist.isStarred) {
                apiService.starGist(item.gist.id)
              } else {
                apiService.unstarGist(item.gist.id)
              }
            if (response.isSuccessful || response.code() == 204) {
              isStarredDirty = false
            }
          } catch (e: Exception) {
            // If push fails, keep it dirty
          }
        }

        if (item.gist.isDirty) {
          // Update existing gist on GitHub
          val fileRequests =
            item.files.associate { file ->
              file.filename to GistFileRequest(content = file.content)
            }
          val request =
            GistRequest(
              description = item.gist.description,
              isPublic = item.gist.isPublic,
              files = fileRequests
            )
          val response = apiService.updateGist(item.gist.id, request)
          saveResponseToDb(
            response = response,
            isPinned = item.gist.isPinned,
            isLocalOnly = false,
            isDirty = false,
            isStarred = isStarred,
            isStarredDirty = isStarredDirty
          )
          successCount++
        } else if (item.gist.isStarredDirty && !isStarredDirty) {
          // Star was modified and successfully updated but there were no description/file edits
          val updated = item.gist.copy(isStarred = isStarred, isStarredDirty = false)
          gistDao.insertGist(updated)
          successCount++
        } else if (item.gist.isStarredDirty && isStarredDirty) {
          // Star update failed
          failCount++
        }
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

suspend fun GistRepository.saveResponseToDb(
  response: GistResponse,
  isPinned: Boolean,
  isLocalOnly: Boolean,
  isDirty: Boolean,
  isStarred: Boolean = false,
  isStarredDirty: Boolean = false
) {
  val id = response.id ?: return
  val existing = gistDao.getGistById(id)
  val tags = existing?.gist?.tags ?: emptyList()

  val entity =
    GistMapper.mapToEntity(
      response = response,
      id = id,
      isPinned = isPinned,
      isLocalOnly = isLocalOnly,
      isDirty = isDirty,
      isStarred = isStarred,
      isStarredDirty = isStarredDirty,
      tags = tags,
      ownerLogin = configPrefs.getOwnerLogin(),
      ownerId = configPrefs.getOwnerId(),
      ownerAvatarUrl = configPrefs.getOwnerAvatarUrl()
    )

  val fileEntities = GistMapper.mapToFiles(response, id) { detectLanguage(it) }

  gistDao.upsertGistWithFiles(entity, fileEntities)
}
