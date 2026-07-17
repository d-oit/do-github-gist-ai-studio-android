package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.core.config.AppConfiguration
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository

class GistViewModelFactory(
  private val repository: GistRepository,
  private val configPrefs: ConfigPrefs,
  private val appConfiguration: AppConfiguration
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(GistViewModel::class.java)) {
      return GistViewModel(repository, configPrefs, appConfiguration) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
