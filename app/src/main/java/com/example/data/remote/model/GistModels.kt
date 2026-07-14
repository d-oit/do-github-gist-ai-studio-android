package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GistFileRequest(@param:Json(name = "content") val content: String)

@JsonClass(generateAdapter = true)
data class GistRequest(
  @param:Json(name = "description") val description: String?,
  @param:Json(name = "public") val isPublic: Boolean,
  @param:Json(name = "files") val files: Map<String, GistFileRequest?>
)

@JsonClass(generateAdapter = true)
data class GistOwnerResponse(
  @param:Json(name = "login") val login: String?,
  @param:Json(name = "id") val id: Int?,
  @param:Json(name = "avatar_url") val avatarUrl: String?
)

@JsonClass(generateAdapter = true)
data class GistFileResponse(
  @param:Json(name = "filename") val filename: String?,
  @param:Json(name = "type") val type: String?,
  @param:Json(name = "language") val language: String?,
  @param:Json(name = "raw_url") val rawUrl: String?,
  @param:Json(name = "size") val size: Long?,
  @param:Json(name = "content") val content: String?
)

@JsonClass(generateAdapter = true)
data class GistResponse(
  @param:Json(name = "id") val id: String?,
  @param:Json(name = "description") val description: String?,
  @param:Json(name = "html_url") val htmlUrl: String?,
  @param:Json(name = "url") val url: String?,
  @param:Json(name = "created_at") val createdAt: String?,
  @param:Json(name = "updated_at") val updatedAt: String?,
  @param:Json(name = "node_id") val nodeId: String?,
  @param:Json(name = "public") val isPublic: Boolean?,
  @param:Json(name = "owner") val owner: GistOwnerResponse?,
  @param:Json(name = "files") val files: Map<String, GistFileResponse>?,
  @param:Json(name = "history") val history: List<GistHistoryResponse>? = null
)

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
