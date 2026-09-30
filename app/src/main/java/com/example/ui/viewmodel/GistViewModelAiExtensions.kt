package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.core.security.PrivacySanitizer
import com.example.ui.components.LocalGistAiModel
import kotlinx.coroutines.launch

fun GistViewModel.analyzeGistContent(description: String, files: List<Pair<String, String>>) {
  viewModelScope.launch {
    _isAnalyzingGist.value = true
    _aiAnalysis.value = null
    Log.d("GistViewModel", "Triggered analyzeGistContent. File count: ${files.size}")
    try {
      val geminiKey = appConfiguration.geminiApiKeyOrNull() ?: ""
      val result =
        LocalGistAiModel.analyzeGist(
          description = description,
          files = files,
          apiKey = geminiKey.ifBlank { null }
        )
      _aiAnalysis.value = result
    } catch (e: Exception) {
      val sanitizedError = PrivacySanitizer.redact(e.message ?: "Unknown error")
      Log.e("GistViewModel", "Error in analyzeGistContent: $sanitizedError", e)
    } finally {
      _isAnalyzingGist.value = false
    }
  }
}

fun GistViewModel.clearAiAnalysis() {
  _aiAnalysis.value = null
}
