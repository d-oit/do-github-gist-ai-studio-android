package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.data.local.pref.AutoSavedDraft
import com.example.data.local.pref.DraftFile

fun GistViewModel.getAutoSavedDraft(editingGistId: String?): AutoSavedDraft? {
  return configPrefs.getAutoSavedDraft(editingGistId)
}

fun GistViewModel.saveAutoSavedDraft(
  editingGistId: String?,
  description: String,
  files: List<Pair<String, String>>,
  isPublic: Boolean,
  isPinned: Boolean,
  tags: List<String>
) {
  val draftFiles = files.map { DraftFile(it.first, it.second) }
  val draft =
    AutoSavedDraft(
      editingGistId = editingGistId,
      description = description,
      files = draftFiles,
      isPublic = isPublic,
      isPinned = isPinned,
      tags = tags,
      timestamp = System.currentTimeMillis()
    )
  configPrefs.saveAutoSavedDraft(draft)
}

fun GistViewModel.clearAutoSavedDraft(editingGistId: String?) {
  configPrefs.clearAutoSavedDraft(editingGistId)
}

fun GistViewModel.detectLanguage(filename: String): String {
  return repository.detectLanguage(filename)
}

fun GistViewModel.exportBackup(
  context: Context,
  uri: Uri,
  ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO,
  onResult: (Boolean, String) -> Unit
) {
  BackupExporter.exportBackup(
    scope = viewModelScope,
    localGists = gists.value,
    context = context,
    uri = uri,
    ioDispatcher = ioDispatcher,
    onResult = onResult
  )
}
