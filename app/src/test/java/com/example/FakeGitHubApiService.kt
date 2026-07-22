package com.example

import com.example.data.remote.api.GitHubApiService
import com.example.data.remote.model.GistOwnerResponse
import com.example.data.remote.model.GistRequest
import com.example.data.remote.model.GistResponse
import java.util.UUID
import retrofit2.Response

class FakeGitHubApiService : GitHubApiService {
  var userResponse =
    GistOwnerResponse(login = "testUser", id = 12345, avatarUrl = "https://github.com/testUser.png")
  val gistsList = mutableListOf<GistResponse>()
  var shouldFailUser = false
  var shouldFailForkWith422 = false

  override suspend fun getGists(page: Int?, perPage: Int?): List<GistResponse> = gistsList

  override suspend fun getGist(id: String): GistResponse {
    return gistsList.find { it.id == id } ?: throw Exception("Gist Not Found")
  }

  override suspend fun getGistRevision(id: String, sha: String): GistResponse {
    return getGist(id)
  }

  override suspend fun getAuthenticatedUser(): GistOwnerResponse {
    if (shouldFailUser) throw Exception("Unauthorized")
    return userResponse
  }

  override suspend fun createGist(request: GistRequest): GistResponse {
    val newGist =
      GistResponse(
        id = UUID.randomUUID().toString(),
        description = request.description,
        htmlUrl = "https://gist.github.com/testUser/some_id",
        url = "https://api.github.com/gists/some_id",
        createdAt = "2026-07-09T09:00:00Z",
        updatedAt = "2026-07-09T09:00:00Z",
        nodeId = "node_id",
        isPublic = request.isPublic,
        owner = userResponse,
        files =
          request.files.mapValues { (name, fileReq) ->
            com.example.data.remote.model.GistFileResponse(
              filename = name,
              type = "text/plain",
              language = "Kotlin",
              rawUrl = "https://gist.github.com/raw/some_id",
              size = fileReq?.content?.length?.toLong() ?: 0L,
              content = fileReq?.content
            )
          }
      )
    gistsList.add(newGist)
    return newGist
  }

  override suspend fun updateGist(id: String, request: GistRequest): GistResponse {
    val updatedGist =
      GistResponse(
        id = id,
        description = request.description,
        htmlUrl = "https://gist.github.com/testUser/$id",
        url = "https://api.github.com/gists/$id",
        createdAt = "2026-07-09T09:00:00Z",
        updatedAt = "2026-07-09T09:00:00Z",
        nodeId = "node_id",
        isPublic = request.isPublic,
        owner = userResponse,
        files =
          request.files.mapValues { (name, fileReq) ->
            com.example.data.remote.model.GistFileResponse(
              filename = name,
              type = "text/plain",
              language = "Kotlin",
              rawUrl = "https://gist.github.com/raw/$id",
              size = fileReq?.content?.length?.toLong() ?: 0L,
              content = fileReq?.content
            )
          }
      )
    gistsList.removeAll { it.id == id }
    gistsList.add(updatedGist)
    return updatedGist
  }

  override suspend fun deleteGist(id: String): Response<Unit> {
    gistsList.removeAll { it.id == id }
    return Response.success(Unit)
  }

  val starredGistIds = mutableSetOf<String>()

  override suspend fun checkIsStarred(id: String): Response<Unit> {
    return if (starredGistIds.contains(id)) {
      Response.success(204, Unit)
    } else {
      Response.error(404, okhttp3.ResponseBody.create(null, ""))
    }
  }

  override suspend fun starGist(id: String): Response<Unit> {
    starredGistIds.add(id)
    return Response.success(204, Unit)
  }

  override suspend fun unstarGist(id: String): Response<Unit> {
    starredGistIds.remove(id)
    return Response.success(204, Unit)
  }

  override suspend fun forkGist(id: String): GistResponse {
    if (shouldFailForkWith422) {
      throw retrofit2.HttpException(
        retrofit2.Response.error<Any>(
          422,
          okhttp3.ResponseBody.create(
            null,
            "{\"message\": \"Validation Failed\", \"errors\": [{\"message\": \"You cannot fork your own gist.\"}]}"
          )
        )
      )
    }
    val original = getGist(id)
    val newId = "fork_" + UUID.randomUUID().toString()
    val forked =
      original.copy(
        id = newId,
        htmlUrl = "https://gist.github.com/testUser/$newId",
        url = "https://api.github.com/gists/$newId"
      )
    gistsList.add(forked)
    return forked
  }
}
