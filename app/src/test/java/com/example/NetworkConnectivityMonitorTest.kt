package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.network.NetworkConnectivityMonitor
import com.example.data.local.AppDatabase
import com.example.data.local.entity.GistEntity
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
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
class NetworkConnectivityMonitorTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private lateinit var database: AppDatabase
  private lateinit var configPrefs: ConfigPrefs
  private lateinit var repository: GistRepository

  private class FakeGitHubApiService : com.example.data.remote.api.GitHubApiService {
    override suspend fun getGists(page: Int?, perPage: Int?) =
      emptyList<com.example.data.remote.model.GistResponse>()

    override suspend fun getGist(id: String) = throw Exception()

    override suspend fun getGistRevision(id: String, sha: String) = throw Exception()

    override suspend fun getAuthenticatedUser() = throw Exception()

    override suspend fun createGist(request: com.example.data.remote.model.GistRequest) =
      throw Exception()

    override suspend fun updateGist(
      id: String,
      request: com.example.data.remote.model.GistRequest
    ) = throw Exception()

    override suspend fun deleteGist(id: String) = retrofit2.Response.success(Unit)

    override suspend fun checkIsStarred(id: String) = retrofit2.Response.success(Unit)

    override suspend fun starGist(id: String) = retrofit2.Response.success(Unit)

    override suspend fun unstarGist(id: String) = retrofit2.Response.success(Unit)

    override suspend fun forkGist(id: String): com.example.data.remote.model.GistResponse =
      throw Exception()
  }

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()

    database =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor { it.run() }
        .setTransactionExecutor { it.run() }
        .build()

    configPrefs = ConfigPrefs(context)
    configPrefs.clear()

    repository =
      GistRepository(
        gistDao = database.gistDao(),
        apiService = FakeGitHubApiService(),
        configPrefs = configPrefs
      )
  }

  @After
  fun tearDown() {
    database.close()
    configPrefs.clear()
  }

  @Test
  fun test_networkConnectivityMonitor_initialOnlineState() {
    val monitor = NetworkConnectivityMonitor(context, repository)
    // Under Robolectric environment, the default is typically online or offline based on shadow
    // config
    val initialOnline = monitor.isOnline.value
    assertNotNull(initialOnline)
  }

  @Test
  fun test_hasUnsynchronizedGists_detectsCorrectly() = runTest {
    // Initially, no unsynchronized gists
    val unsyncedBefore = repository.getUnsynchronizedGists()
    assertTrue(unsyncedBefore.isEmpty())

    // Insert an unsynchronized GistEntity
    val unsyncedGist =
      GistEntity(
        id = "local_only_id",
        description = "Unsynced local gist",
        htmlUrl = "",
        url = "",
        createdAt = "2026-07-15T00:00:00Z",
        updatedAt = "2026-07-15T00:00:00Z",
        nodeId = "",
        isPublic = true,
        isPinned = false,
        isLocalOnly = true, // local only
        isDirty = false,
        isDeleted = false,
        isStarred = false,
        isStarredDirty = false,
        tags = emptyList(),
        ownerLogin = "test_user",
        ownerId = 12345,
        ownerAvatarUrl = ""
      )
    database.gistDao().insertGist(unsyncedGist)

    val unsyncedAfter = repository.getUnsynchronizedGists()
    assertEquals(1, unsyncedAfter.size)
    assertEquals("local_only_id", unsyncedAfter[0].gist.id)
  }

  @Test
  fun test_networkConnectivityMonitor_registersCallbackAndTriggersSync() = runTest {
    // Initialize WorkManager for testing enqueued work
    try {
      val config = androidx.work.Configuration.Builder().build()
      androidx.work.WorkManager.initialize(context, config)
    } catch (e: Exception) {
      // Ignored if already initialized
    }

    // Insert an unsynchronized GistEntity to trigger sync
    val unsyncedGist =
      GistEntity(
        id = "local_only_id",
        description = "Unsynced local gist",
        htmlUrl = "",
        url = "",
        createdAt = "2026-07-15T00:00:00Z",
        updatedAt = "2026-07-15T00:00:00Z",
        nodeId = "",
        isPublic = true,
        isPinned = false,
        isLocalOnly = true,
        isDirty = false,
        isDeleted = false,
        isStarred = false,
        isStarredDirty = false,
        tags = emptyList(),
        ownerLogin = "test_user",
        ownerId = 12345,
        ownerAvatarUrl = ""
      )
    database.gistDao().insertGist(unsyncedGist)

    val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val shadowConnectivityManager = org.robolectric.Shadows.shadowOf(connectivityManager)

    // Configure internet capabilities and NetworkInfo for default/active network under Robolectric
    val mockNetwork = org.robolectric.shadows.ShadowNetwork.newInstance(1)
    val capabilities = android.net.NetworkCapabilities()
    val shadowCapabilities = org.robolectric.Shadows.shadowOf(capabilities)
    shadowCapabilities.addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    shadowCapabilities.addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    shadowConnectivityManager.setNetworkCapabilities(mockNetwork, capabilities)

    val networkInfo =
      org.robolectric.shadows.ShadowNetworkInfo.newInstance(
        android.net.NetworkInfo.DetailedState.CONNECTED,
        android.net.ConnectivityManager.TYPE_WIFI,
        0,
        true,
        true
      )
    shadowConnectivityManager.addNetwork(mockNetwork, networkInfo)

    val monitor = NetworkConnectivityMonitor(context, repository)
    val testScope = TestScope(testDispatcher)
    monitor.startMonitoring(testScope)

    // Trigger NetworkCallback onAvailable
    val callbacks = shadowConnectivityManager.networkCallbacks
    assertFalse("Should have registered network callbacks", callbacks.isEmpty())

    callbacks.forEach { callback -> callback.onAvailable(mockNetwork) }

    testScope.testScheduler.advanceUntilIdle()

    // Assert that monitor.isOnline becomes true
    assertTrue("Monitor should show online state", monitor.isOnline.value)

    // Assert that a WorkManager task has been enqueued (polling up to 2 seconds due to
    // Dispatchers.IO)
    val workManager = androidx.work.WorkManager.getInstance(context)
    var workInfos = emptyList<androidx.work.WorkInfo>()
    for (i in 1..40) {
      workInfos = workManager.getWorkInfosForUniqueWork("gist_sync_work").get()
      if (workInfos.isNotEmpty()) break
      Thread.sleep(50)
    }
    assertFalse("Should have enqueued a sync task", workInfos.isEmpty())
  }

  @Test
  fun test_networkConnectivityMonitor_networkLostUpdatesState() = runTest {
    val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val shadowConnectivityManager = org.robolectric.Shadows.shadowOf(connectivityManager)

    // Initially configure a connected mock network
    val mockNetwork = org.robolectric.shadows.ShadowNetwork.newInstance(1)
    val capabilities = android.net.NetworkCapabilities()
    val shadowCapabilities = org.robolectric.Shadows.shadowOf(capabilities)
    shadowCapabilities.addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    shadowCapabilities.addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    shadowConnectivityManager.setNetworkCapabilities(mockNetwork, capabilities)

    val networkInfo =
      org.robolectric.shadows.ShadowNetworkInfo.newInstance(
        android.net.NetworkInfo.DetailedState.CONNECTED,
        android.net.ConnectivityManager.TYPE_WIFI,
        0,
        true,
        true
      )
    shadowConnectivityManager.addNetwork(mockNetwork, networkInfo)

    val monitor = NetworkConnectivityMonitor(context, repository)
    val testScope = TestScope(testDispatcher)
    monitor.startMonitoring(testScope)

    val callbacks = shadowConnectivityManager.networkCallbacks
    assertFalse(callbacks.isEmpty())

    // Trigger onAvailable to set it to true
    callbacks.forEach { callback -> callback.onAvailable(mockNetwork) }
    testScope.testScheduler.advanceUntilIdle()
    assertTrue(monitor.isOnline.value)

    // Configure shadow capabilities as empty (no internet) and remove network for checking network
    // loss
    val emptyCapabilities = android.net.NetworkCapabilities()
    shadowConnectivityManager.setNetworkCapabilities(mockNetwork, emptyCapabilities)
    shadowConnectivityManager.removeNetwork(mockNetwork)

    // Trigger onLost
    callbacks.forEach { callback -> callback.onLost(mockNetwork) }
    testScope.testScheduler.advanceUntilIdle()

    // Assert that it correctly transitions isOnline state to false when offline
    assertFalse(monitor.isOnline.value)
  }
}
