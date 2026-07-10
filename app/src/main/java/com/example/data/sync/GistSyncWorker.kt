package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.DoGistHubApp

/**
 * A WorkManager CoroutineWorker that synchronizes unsaved/unsynced local Gists
 * to GitHub using the [com.example.data.repository.GistRepository].
 * It requires a valid network connection.
 */
class GistSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting background Gist synchronization...")
        val repository = DoGistHubApp.instance.repository

        return try {
            val syncResult = repository.syncWithGitHub()
            if (syncResult.isSuccess) {
                Log.d(TAG, "Gist synchronization succeeded: ${syncResult.getOrNull()}")
                Result.success()
            } else {
                val errorMsg = syncResult.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "Gist synchronization failed: $errorMsg")
                
                // If it is due to an unconfigured token, don't keep retrying
                if (errorMsg.contains("token", ignoreCase = true)) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gist synchronization failed with exception", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "GistSyncWorker"
        private const val WORK_NAME = "gist_sync_work"

        /**
         * Enqueues a one-time, unique synchronization work request.
         * Runs as soon as the internet connection is active.
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<GistSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
            Log.d(TAG, "GistSyncWorker enqueued unique work")
        }
    }
}
