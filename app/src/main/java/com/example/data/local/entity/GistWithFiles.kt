package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class GistWithFiles(
  @Embedded val gist: GistEntity,
  @Relation(parentColumn = "id", entityColumn = "gistId") val files: List<GistFileEntity>
)
