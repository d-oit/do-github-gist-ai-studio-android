package com.example.data.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.data.repository.GistRepository

class GistSyncWorkerFactory(private val repository: GistRepository) : WorkerFactory() {
  override fun createWorker(
    appContext: Context,
    workerClassName: String,
    workerParameters: WorkerParameters
  ): ListenableWorker? {
    return when (workerClassName) {
      GistSyncWorker::class.java.name -> {
        GistSyncWorker(appContext, workerParameters, repository)
      }
      else -> null
    }
  }
}
