package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.example.data.local.entity.GistBackupFile
import com.example.data.local.entity.GistBackupItem
import com.example.data.local.entity.GistBackupPayload
import com.example.data.local.entity.GistWithFiles
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object BackupExporter {
  fun exportBackup(
    scope: CoroutineScope,
    localGists: List<GistWithFiles>,
    context: Context,
    uri: Uri,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    onResult: (Boolean, String) -> Unit
  ): Job {
    return scope.launch(ioDispatcher) {
      try {
        val backupItems =
          localGists.map { item ->
            GistBackupItem(
              id = item.gist.id,
              description = item.gist.description,
              htmlUrl = item.gist.htmlUrl,
              url = item.gist.url,
              createdAt = item.gist.createdAt,
              updatedAt = item.gist.updatedAt,
              isPublic = item.gist.isPublic,
              isPinned = item.gist.isPinned,
              isLocalOnly = item.gist.isLocalOnly,
              isDirty = item.gist.isDirty,
              isDeleted = item.gist.isDeleted,
              isStarred = item.gist.isStarred,
              isStarredDirty = item.gist.isStarredDirty,
              ownerLogin = item.gist.ownerLogin,
              ownerId = item.gist.ownerId,
              ownerAvatarUrl = item.gist.ownerAvatarUrl,
              files =
                item.files.map { file ->
                  GistBackupFile(
                    filename = file.filename,
                    content = file.content,
                    type = file.type,
                    language = file.language,
                    size = file.size
                  )
                }
            )
          }

        val sdf =
          SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
          }
        val exportedAt = sdf.format(Date())

        val payload =
          GistBackupPayload(backupVersion = 1, exportedAt = exportedAt, gists = backupItems)

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(GistBackupPayload::class.java).indent("  ")
        val jsonString = adapter.toJson(payload)

        if (uri.scheme == "file") {
          val file = java.io.File(uri.path ?: throw IllegalArgumentException("Invalid file path"))
          file.outputStream().use { outputStream ->
            outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
          }
        } else {
          context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
          } ?: throw IllegalArgumentException("Failed to open output stream")
        }

        withContext(mainDispatcher) { onResult(true, "Backup completed successfully") }
      } catch (e: Exception) {
        withContext(mainDispatcher) {
          onResult(false, e.localizedMessage ?: e.message ?: "Failed to save backup")
        }
      }
    }
  }
}
