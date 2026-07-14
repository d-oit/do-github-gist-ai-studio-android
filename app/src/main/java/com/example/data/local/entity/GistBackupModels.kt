package com.example.data.local.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GistBackupFile(
  val filename: String,
  val content: String,
  val type: String,
  val language: String?,
  val size: Long
)

@JsonClass(generateAdapter = true)
data class GistBackupItem(
  val id: String,
  val description: String?,
  val htmlUrl: String,
  val url: String,
  val createdAt: String,
  val updatedAt: String,
  val isPublic: Boolean,
  val isPinned: Boolean,
  val isLocalOnly: Boolean,
  val isDirty: Boolean,
  val isDeleted: Boolean,
  val isStarred: Boolean,
  val isStarredDirty: Boolean,
  val ownerLogin: String,
  val ownerId: Int,
  val ownerAvatarUrl: String,
  val files: List<GistBackupFile>
)

@JsonClass(generateAdapter = true)
data class GistBackupPayload(
  val backupVersion: Int = 1,
  val exportedAt: String,
  val gists: List<GistBackupItem>
)
