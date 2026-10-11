package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.example.data.local.entity.GistBackupPayload
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.data.repository.GistRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object BackupImporter {
  fun importBackup(
    scope: CoroutineScope,
    repository: GistRepository,
    context: Context,
    uri: Uri,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    onResult: (Boolean, String, Int) -> Unit
  ): Job {
    return scope.launch(ioDispatcher) {
      try {
        val inputStream: InputStream =
          if (uri.scheme == "file") {
            val path = uri.path ?: throw IllegalArgumentException("Invalid file path")
            java.io.File(path).inputStream()
          } else {
            context.contentResolver.openInputStream(uri)
              ?: throw IllegalArgumentException("Failed to open input stream for URI: $uri")
          }

        val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(GistBackupPayload::class.java)
        val payload =
          adapter.fromJson(jsonString)
            ?: throw IllegalArgumentException("Failed to deserialize backup payload")

        var importedCount = 0
        for (item in payload.gists) {
          val gistEntity =
            GistEntity(
              id = item.id,
              description = item.description,
              htmlUrl = item.htmlUrl,
              url = item.url,
              createdAt = item.createdAt,
              updatedAt = item.updatedAt,
              nodeId = "",
              isPublic = item.isPublic,
              isPinned = item.isPinned,
              isLocalOnly = item.isLocalOnly,
              isDirty = item.isDirty,
              isDeleted = false,
              isStarred = item.isStarred,
              isStarredDirty = item.isStarredDirty,
              ownerLogin = item.ownerLogin,
              ownerId = item.ownerId,
              ownerAvatarUrl = item.ownerAvatarUrl,
              tags = emptyList()
            )

          val fileEntities =
            item.files.map { file ->
              GistFileEntity(
                fileId = UUID.randomUUID().toString(),
                gistId = item.id,
                filename = file.filename,
                type = file.type,
                language = file.language ?: repository.detectLanguage(file.filename),
                rawUrl = "",
                size = file.size,
                content = file.content
              )
            }

          val gistWithFiles = GistWithFiles(gist = gistEntity, files = fileEntities)
          repository.restoreGist(gistWithFiles)
          importedCount++
        }

        withContext(mainDispatcher) {
          onResult(true, "Successfully restored $importedCount Gists", importedCount)
        }
      } catch (e: Exception) {
        withContext(mainDispatcher) {
          onResult(false, e.localizedMessage ?: e.message ?: "Failed to import backup", 0)
        }
      }
    }
  }
}
