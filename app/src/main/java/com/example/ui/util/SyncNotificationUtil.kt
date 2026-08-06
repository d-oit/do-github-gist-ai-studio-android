package com.example.ui.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.example.data.repository.SyncStatus

/**
 * Utility for displaying clear snackbar notifications during background or manual sync operations,
 * informing users of local data sync success or failure statuses.
 */
object SyncNotificationUtil {

  suspend fun showSyncStatusSnackbar(
    snackbarHostState: SnackbarHostState,
    syncStatus: SyncStatus,
    onDismiss: () -> Unit = {}
  ): SnackbarResult? {
    val result =
      when (syncStatus) {
        is SyncStatus.Success -> {
          val message =
            if (syncStatus.message.isNotBlank()) {
              "✓ ${syncStatus.message}"
            } else {
              "✓ Local data synchronized successfully"
            }
          snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Short,
            withDismissAction = true
          )
        }
        is SyncStatus.Error -> {
          val errorMessage =
            if (syncStatus.errorMessage.isNotBlank()) {
              "✕ Sync failed: ${syncStatus.errorMessage}"
            } else {
              "✕ Sync failed. Local data remains saved offline."
            }
          snackbarHostState.showSnackbar(
            message = errorMessage,
            duration = SnackbarDuration.Long,
            withDismissAction = true
          )
        }
        else -> null
      }
    if (result != null) {
      onDismiss()
    }
    return result
  }
}
