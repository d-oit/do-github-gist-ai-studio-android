package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * A WorkManager CoroutineWorker that synchronizes unsaved/unsynced local Gists to GitHub using the
 * [com.example.data.repository.GistRepository]. It requires a valid network connection.
 */
class GistSyncWorker(
  context: Context,
  workerParams: WorkerParameters,
  private val repository: com.example.data.repository.GistRepository
) : CoroutineWorker(context, workerParams) {

  override suspend fun doWork(): Result {
    Log.d(TAG, "Starting background Gist synchronization...")

    return try {
      repository.updateSyncStatus(com.example.data.repository.SyncStatus.Syncing)
      val syncResult = repository.syncWithGitHub()
      if (syncResult.isSuccess) {
        val successMsg = syncResult.getOrNull() ?: "Successfully synchronized"
        Log.d(TAG, "Gist synchronization succeeded: $successMsg")
        repository.updateSyncStatus(
          com.example.data.repository.SyncStatus.Success(successMsg, System.currentTimeMillis())
        )
        Result.success()
      } else {
        val throwable = syncResult.exceptionOrNull()
        val errorMsg = com.example.core.error.SyncErrorHandler.classifyError(throwable)
        Log.e(TAG, "Gist synchronization failed: $errorMsg")
        repository.updateSyncStatus(
          com.example.data.repository.SyncStatus.Error(errorMsg, System.currentTimeMillis())
        )

        // If it is due to an unconfigured token, don't keep retrying
        if (
          errorMsg.contains("token", ignoreCase = true) ||
            errorMsg.contains("auth", ignoreCase = true) ||
            errorMsg.contains("credential", ignoreCase = true) ||
            errorMsg.contains("config", ignoreCase = true)
        ) {
          Result.failure()
        } else {
          Result.retry()
        }
      }
    } catch (e: Exception) {
      val classified = com.example.core.error.SyncErrorHandler.classifyError(e)
      Log.e(TAG, "Gist synchronization failed with exception", e)
      repository.updateSyncStatus(
        com.example.data.repository.SyncStatus.Error(classified, System.currentTimeMillis())
      )
      Result.retry()
    }
  }

  companion object {
    private const val TAG = "GistSyncWorker"
    private const val WORK_NAME = "gist_sync_work"
    private const val PERIODIC_WORK_NAME = "gist_periodic_sync_work"

    /**
     * Enqueues a one-time, unique synchronization work request. Runs as soon as the internet
     * connection is active.
     */
    fun enqueue(context: Context) {
      try {
        val constraints =
          Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val syncRequest =
          OneTimeWorkRequestBuilder<GistSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
          .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, syncRequest)
        Log.d(TAG, "GistSyncWorker enqueued unique work")
      } catch (e: Exception) {
        Log.e(TAG, "WorkManager not available, skipping one-time sync enqueuing: ${e.message}")
      }
    }

    /**
     * Enqueues a periodic background synchronization work request. Runs every 15 minutes to keep
     * the local database synchronized with the GitHub API.
     */
    fun enqueuePeriodic(context: Context) {
      try {
        val constraints =
          Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val periodicRequest =
          PeriodicWorkRequestBuilder<GistSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
          .enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
          )
        Log.d(TAG, "GistSyncWorker enqueued unique periodic work")
      } catch (e: Exception) {
        Log.e(TAG, "WorkManager not available, skipping periodic sync enqueuing: ${e.message}")
      }
    }
  }
}
