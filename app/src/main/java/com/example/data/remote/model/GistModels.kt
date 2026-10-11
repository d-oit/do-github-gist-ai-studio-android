package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Model representing an individual file in a Gist creation or update request. */
@JsonClass(generateAdapter = true)
data class GistFileRequest(
  @param:Json(name = "content") val content: String,
  @param:Json(name = "filename") val filename: String? = null
)

/** Model for creating or updating a Gist. */
@JsonClass(generateAdapter = true)
data class GistRequest(
  @param:Json(name = "description") val description: String? = null,
  @param:Json(name = "public") val isPublic: Boolean = false,
  @param:Json(name = "files") val files: Map<String, GistFileRequest?> = emptyMap()
)

/** Type aliases for Gist creation requests. */
typealias CreateGistRequest = GistRequest

typealias CreateGistFileRequest = GistFileRequest

typealias GistCreationRequest = GistRequest

/** Model representing the result of a Gist deletion operation. */
@JsonClass(generateAdapter = true)
data class GistDeleteResponse(
  @param:Json(name = "id") val id: String? = null,
  @param:Json(name = "success") val success: Boolean = true,
  @param:Json(name = "message") val message: String? = null
)

/** Type aliases for Gist deletion response. */
typealias DeleteGistResponse = GistDeleteResponse

typealias GistDeletionResponse = GistDeleteResponse

/** Model representing a Gist deletion request parameter. */
@JsonClass(generateAdapter = true)
data class DeleteGistRequest(@param:Json(name = "id") val id: String)

typealias GistDeleteRequest = DeleteGistRequest

@JsonClass(generateAdapter = true)
data class GistOwnerResponse(
  @param:Json(name = "login") val login: String? = null,
  @param:Json(name = "id") val id: Int? = null,
  @param:Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GistFileResponse(
  @param:Json(name = "filename") val filename: String? = null,
  @param:Json(name = "type") val type: String? = null,
  @param:Json(name = "language") val language: String? = null,
  @param:Json(name = "raw_url") val rawUrl: String? = null,
  @param:Json(name = "size") val size: Long? = null,
  @param:Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class GistResponse(
  @param:Json(name = "id") val id: String? = null,
  @param:Json(name = "description") val description: String? = null,
  @param:Json(name = "html_url") val htmlUrl: String? = null,
  @param:Json(name = "url") val url: String? = null,
  @param:Json(name = "created_at") val createdAt: String? = null,
  @param:Json(name = "updated_at") val updatedAt: String? = null,
  @param:Json(name = "node_id") val nodeId: String? = null,
  @param:Json(name = "public") val isPublic: Boolean? = null,
  @param:Json(name = "owner") val owner: GistOwnerResponse? = null,
  @param:Json(name = "files") val files: Map<String, GistFileResponse>? = null,
  @param:Json(name = "history") val history: List<GistHistoryResponse>? = null
)

/** Type aliases for Gist listing responses. */
typealias GistListResponse = List<GistResponse>

typealias ListGistsResponse = List<GistResponse>

@JsonClass(generateAdapter = true)
data class GistHistoryChangeStatus(
  @param:Json(name = "deletions") val deletions: Int?,
  @param:Json(name = "additions") val additions: Int?,
  @param:Json(name = "total") val total: Int?
)

@JsonClass(generateAdapter = true)
data class GistHistoryResponse(
  @param:Json(name = "url") val url: String?,
  @param:Json(name = "version") val version: String?,
  @param:Json(name = "user") val user: GistOwnerResponse?,
  @param:Json(name = "change_status") val changeStatus: GistHistoryChangeStatus?,
  @param:Json(name = "committed_at") val committedAt: String?
)

/** Model for creating a comment on a Gist. */
@JsonClass(generateAdapter = true)
data class GistCommentRequest(@param:Json(name = "body") val body: String)

/** Model representing a comment on a Gist from GitHub. */
@JsonClass(generateAdapter = true)
data class GistCommentResponse(
  @param:Json(name = "id") val id: Long? = null,
  @param:Json(name = "node_id") val nodeId: String? = null,
  @param:Json(name = "url") val url: String? = null,
  @param:Json(name = "body") val body: String? = null,
  @param:Json(name = "user") val user: GistOwnerResponse? = null,
  @param:Json(name = "created_at") val createdAt: String? = null,
  @param:Json(name = "updated_at") val updatedAt: String? = null
)
