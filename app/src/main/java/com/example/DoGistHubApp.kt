package com.example

import android.app.Application
import com.example.core.config.AppConfiguration
import com.example.core.config.AppConfigurationImpl
import com.example.data.local.AppDatabase
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import com.example.di.NetworkModule
import com.example.di.StorageModule
import kotlinx.coroutines.launch

class DoGistHubApp : Application() {

  lateinit var configPrefs: ConfigPrefs
    private set

  lateinit var database: AppDatabase
    private set

  lateinit var repository: GistRepository
    private set

  lateinit var appConfiguration: AppConfiguration
    private set

  lateinit var networkConnectivityMonitor: com.example.core.network.NetworkConnectivityMonitor
    private set

  override fun onCreate() {
    // Install global uncaught exception handler to safely log all crashes with privacy redaction
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      val rawMessage = throwable.localizedMessage ?: ""
      val stackTraceStr = android.util.Log.getStackTraceString(throwable)
      val sanitizedMsg = com.example.core.security.PrivacySanitizer.redact(rawMessage)
      val sanitizedStack = com.example.core.security.PrivacySanitizer.redact(stackTraceStr)
      android.util.Log.e("DoGistHubApp", "CRITICAL GLOBAL EXCEPTION on thread ${thread.name}: $sanitizedMsg\n$sanitizedStack")
    }

    if (com.example.BuildConfig.DEBUG) {
      android.os.StrictMode.setThreadPolicy(
        android.os.StrictMode.ThreadPolicy.Builder()
          .detectDiskReads()
          .detectDiskWrites()
          .detectNetwork()
          .penaltyLog()
          .build()
      )
      android.os.StrictMode.setVmPolicy(
        android.os.StrictMode.VmPolicy.Builder()
          .detectLeakedSqlLiteObjects()
          .detectLeakedRegistrationObjects()
          .penaltyLog()
          .build()
      )
    }
    super.onCreate()

    configPrefs = ConfigPrefs(this)
    database = StorageModule.provideAppDatabase(this)
    appConfiguration = AppConfigurationImpl()

    val moshi = NetworkModule.provideMoshi()
    val authInterceptor = com.example.data.remote.interceptor.GitHubAuthInterceptor(configPrefs)
    val okHttp = NetworkModule.provideOkHttpClient(authInterceptor, this)
    val retrofit = NetworkModule.provideRetrofit(okHttp, moshi)
    val apiService = NetworkModule.provideGitHubApiService(retrofit)

    repository =
      GistRepository(
        gistDao = database.gistDao(),
        apiService = apiService,
        configPrefs = configPrefs
      )

    // Initialize custom WorkManager configuration with GistSyncWorkerFactory
    try {
      val syncWorkerFactory = com.example.data.sync.GistSyncWorkerFactory(repository)
      val workConfig =
        androidx.work.Configuration.Builder().setWorkerFactory(syncWorkerFactory).build()
      androidx.work.WorkManager.initialize(this, workConfig)
    } catch (e: IllegalStateException) {
      // WorkManager is already initialized (e.g. in Robolectric unit tests)
    }

    // Observe the database for 'isDirty' or 'isLocalOnly' flags
    // and automatically attempt to push changes to GitHub Gist API
    val applicationScope =
      kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()
      )
    applicationScope.launch {
      try {
        repository.unsynchronizedGists.collect { unsynced ->
          if (unsynced.isNotEmpty()) {
            com.example.data.sync.GistSyncWorker.enqueue(this@DoGistHubApp)
          }
        }
      } catch (e: Exception) {
        // Handle/ignore exceptions in test environments gracefully
      }
    }

    // Schedule periodic background sync (every 15 mins)
    try {
      com.example.data.sync.GistSyncWorker.enqueuePeriodic(this)
    } catch (e: Exception) {
      // Ignore background scheduling exceptions in test environments
    }

    // Initialize network connectivity monitor to hook internet restored states and periodically
    // verify connectivity
    networkConnectivityMonitor =
      com.example.core.network.NetworkConnectivityMonitor(this, repository)
    networkConnectivityMonitor.startMonitoring(applicationScope)
  }
}
