package com.example

import android.app.Application
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

    override fun onCreate() {
        super.onCreate()
        instance = this

        configPrefs = ConfigPrefs(this)
        database = StorageModule.provideAppDatabase(this)
        
        val moshi = NetworkModule.provideMoshi()
        val authInterceptor = com.example.data.remote.interceptor.GitHubAuthInterceptor(configPrefs)
        val okHttp = NetworkModule.provideOkHttpClient(authInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttp, moshi)
        val apiService = NetworkModule.provideGitHubApiService(retrofit)

        repository = GistRepository(
            gistDao = database.gistDao(),
            apiService = apiService,
            configPrefs = configPrefs
        )

        // Observe the database for 'isDirty' or 'isLocalOnly' flags 
        // and automatically attempt to push changes to GitHub Gist API
        val applicationScope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()
        )
        applicationScope.launch {
            repository.unsynchronizedGists.collect { unsynced ->
                if (unsynced.isNotEmpty()) {
                    com.example.data.sync.GistSyncWorker.enqueue(this@DoGistHubApp)
                }
            }
        }
    }

    companion object {
        lateinit var instance: DoGistHubApp
            private set
    }
}
