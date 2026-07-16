package com.example.data.repository

import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.remote.model.GistResponse
import java.util.UUID

object GistMapper {
  fun mapToEntity(
    response: GistResponse,
    id: String,
    isPinned: Boolean,
    isLocalOnly: Boolean,
    isDirty: Boolean,
    isStarred: Boolean,
    isStarredDirty: Boolean,
    tags: List<String>,
    ownerLogin: String,
    ownerId: Int,
    ownerAvatarUrl: String
  ): GistEntity {
    return GistEntity(
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
      isStarred = isStarred,
      isStarredDirty = isStarredDirty,
      tags = tags,
      ownerLogin = response.owner?.login ?: ownerLogin,
      ownerId = response.owner?.id ?: ownerId,
      ownerAvatarUrl = response.owner?.avatarUrl ?: ownerAvatarUrl
    )
  }

  fun mapToFiles(
    response: GistResponse,
    id: String,
    detectLanguage: (String) -> String
  ): List<GistFileEntity> {
    return response.files?.map { (name, file) ->
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
  }
}
