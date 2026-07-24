package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.data.local.AppDatabase
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import com.example.data.repository.SyncStatus
import com.example.data.sync.GistSyncWorker
import com.example.data.sync.GistSyncWorkerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GistSyncWorkerTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private lateinit var database: AppDatabase
  private lateinit var configPrefs: ConfigPrefs
  private lateinit var fakeApiService: FakeGitHubApiService
  private lateinit var repository: GistRepository

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()

    database =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor { it.run() }
        .setTransactionExecutor { it.run() }
        .build()

    configPrefs = ConfigPrefs(context)
    configPrefs.clear()
    configPrefs.setGithubToken("ghp_test_sync_worker_999")
    configPrefs.setOwnerLogin("testUser")
    configPrefs.setOwnerAvatarUrl("https://github.com/testUser.png")
    configPrefs.setOwnerId(12345)

    fakeApiService = FakeGitHubApiService()

    repository =
      GistRepository(
        gistDao = database.gistDao(),
        apiService = fakeApiService,
        configPrefs = configPrefs
      )
  }

  @After
  fun tearDown() {
    database.close()
    configPrefs.clear()
    Dispatchers.resetMain()
  }

  @Test
  fun test_gistSyncWorkerFactory_createsCorrectWorker() {
    val factory = GistSyncWorkerFactory(repository)
    val workerClassName = GistSyncWorker::class.java.name

    val worker =
      TestListenableWorkerBuilder<GistSyncWorker>(context).setWorkerFactory(factory).build()

    assertNotNull(worker)
    assertTrue(worker is GistSyncWorker)
  }

  @Test
  fun test_worker_doWork_success_whenNoUnsynced() =
    runTest(testDispatcher) {
      val factory = GistSyncWorkerFactory(repository)
      val worker =
        TestListenableWorkerBuilder<GistSyncWorker>(context).setWorkerFactory(factory).build()

      val result = worker.doWork()

      assertEquals(ListenableWorker.Result.success(), result)
      assertTrue(repository.syncStatus.value is SyncStatus.Success)
    }

  @Test
  fun test_worker_doWork_failure_whenNoToken() =
    runTest(testDispatcher) {
      configPrefs.setGithubToken("") // Clear the token to trigger unconfigured failure

      val factory = GistSyncWorkerFactory(repository)
      val worker =
        TestListenableWorkerBuilder<GistSyncWorker>(context).setWorkerFactory(factory).build()

      val result = worker.doWork()

      assertEquals(ListenableWorker.Result.failure(), result)
      assertTrue(repository.syncStatus.value is SyncStatus.Error)
      val errorState = repository.syncStatus.value as SyncStatus.Error
      assertTrue(errorState.errorMessage.contains("configured", ignoreCase = true))
    }

  @Test
  fun test_enqueueAndEnqueuePeriodic_dontCrash() {
    // Basic test ensuring no crashes occur during work request construction and enqueueing
    GistSyncWorker.enqueue(context)
    GistSyncWorker.enqueuePeriodic(context)
  }
}
