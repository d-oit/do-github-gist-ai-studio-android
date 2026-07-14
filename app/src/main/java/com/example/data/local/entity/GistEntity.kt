package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gists")
data class GistEntity(
  @PrimaryKey val id: String,
  val description: String?,
  val htmlUrl: String,
  val url: String,
  val createdAt: String,
  val updatedAt: String,
  val nodeId: String,
  val isPublic: Boolean,
  val isPinned: Boolean,
  val isLocalOnly: Boolean,
  val isDirty: Boolean,
  val isDeleted: Boolean = false,
  val isStarred: Boolean = false,
  val isStarredDirty: Boolean = false,
  val tags: List<String> = emptyList(),
  val ownerLogin: String,
  val ownerId: Int,
  val ownerAvatarUrl: String
)
