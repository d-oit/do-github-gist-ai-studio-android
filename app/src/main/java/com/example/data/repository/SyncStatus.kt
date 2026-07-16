package com.example.data.repository

sealed interface SyncStatus {
  object Idle : SyncStatus

  object Syncing : SyncStatus

  data class Success(val message: String, val timestamp: Long) : SyncStatus

  data class Error(val errorMessage: String, val timestamp: Long) : SyncStatus
}
