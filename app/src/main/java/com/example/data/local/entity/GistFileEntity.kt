package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gist_files",
    foreignKeys = [
        ForeignKey(
            entity = GistEntity::class,
            parentColumns = ["id"],
            childColumns = ["gistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["gistId"])]
)
data class GistFileEntity(
    @PrimaryKey val fileId: String,
    val gistId: String,
    val filename: String,
    val type: String,
    val language: String?,
    val rawUrl: String,
    val size: Long,
    val content: String
)
