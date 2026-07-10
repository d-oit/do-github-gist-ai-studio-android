package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GistFileRequest(
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class GistRequest(
    @Json(name = "description") val description: String?,
    @Json(name = "public") val isPublic: Boolean,
    @Json(name = "files") val files: Map<String, GistFileRequest?>
)

@JsonClass(generateAdapter = true)
data class GistOwnerResponse(
    @Json(name = "login") val login: String?,
    @Json(name = "id") val id: Int?,
    @Json(name = "avatar_url") val avatarUrl: String?
)

@JsonClass(generateAdapter = true)
data class GistFileResponse(
    @Json(name = "filename") val filename: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "language") val language: String?,
    @Json(name = "raw_url") val rawUrl: String?,
    @Json(name = "size") val size: Long?,
    @Json(name = "content") val content: String?
)

@JsonClass(generateAdapter = true)
data class GistResponse(
    @Json(name = "id") val id: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "html_url") val htmlUrl: String?,
    @Json(name = "url") val url: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "node_id") val nodeId: String?,
    @Json(name = "public") val isPublic: Boolean?,
    @Json(name = "owner") val owner: GistOwnerResponse?,
    @Json(name = "files") val files: Map<String, GistFileResponse>?
)
