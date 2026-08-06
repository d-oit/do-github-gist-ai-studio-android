package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import com.example.data.repository.SyncStatus
import com.example.ui.components.SyncStatusDashboardView
import com.example.ui.viewmodel.GistViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OfflineOnlyModeTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var configPrefs: ConfigPrefs

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    configPrefs = ConfigPrefs(context)
    configPrefs.clear()
  }

  @Test
  fun configPrefs_persistsOfflineOnlyState() {
    assertFalse(configPrefs.isOfflineOnly())

    configPrefs.setOfflineOnly(true)
    assertTrue(configPrefs.isOfflineOnly())

    configPrefs.setOfflineOnly(false)
    assertFalse(configPrefs.isOfflineOnly())
  }

  @Test
  fun gistViewModel_togglesOfflineOnlyModeAndUpdatesStatus() {
    val fakeApi = FakeGitHubApiService()
    val repo =
      GistRepository(gistDao = FakeGistDao(), apiService = fakeApi, configPrefs = configPrefs)
    val viewModel = GistViewModel(repo, configPrefs, com.example.core.config.AppConfigurationImpl())

    assertFalse(viewModel.isOfflineOnly.value)

    viewModel.toggleOfflineOnly()
    assertTrue(viewModel.isOfflineOnly.value)
    assertTrue(configPrefs.isOfflineOnly())
    assertEquals("Offline-Only mode enabled. Network sync paused.", viewModel.statusMessage.value)

    viewModel.toggleOfflineOnly()
    assertFalse(viewModel.isOfflineOnly.value)
    assertFalse(configPrefs.isOfflineOnly())
    assertEquals("Offline-Only mode disabled. Network sync resumed.", viewModel.statusMessage.value)
  }

  @Test
  fun syncStatusDashboardView_rendersOfflineOnlyToggleAndBannerWhenEnabled() {
    composeTestRule.setContent {
      SyncStatusDashboardView(
        gists = emptyList(),
        isSyncing = false,
        isOnline = true,
        onSyncClick = {},
        lastSyncTime = System.currentTimeMillis(),
        syncStatus = SyncStatus.Idle,
        isOfflineOnly = true,
        onToggleOfflineOnly = {}
      )
    }

    composeTestRule.onNodeWithTag("offline_only_toggle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("offline_mode_switch").assertIsDisplayed()
    composeTestRule.onNodeWithTag("offline_only_mode_banner").assertIsDisplayed()
    composeTestRule
      .onNodeWithText(
        "Offline-Only Mode Active — Network synchronization is paused.",
        substring = true
      )
      .assertIsDisplayed()
  }
}

@Suppress("EmptyFunctionBlock")
private class FakeGistDao : com.example.data.local.dao.GistDao {
  override fun observeAllGists():
    kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.GistWithFiles>> =
    kotlinx.coroutines.flow.flowOf(emptyList())

  override fun observeGistById(
    id: String
  ): kotlinx.coroutines.flow.Flow<com.example.data.local.entity.GistWithFiles?> =
    kotlinx.coroutines.flow.flowOf(null)

  override fun observeUnsynchronizedGists():
    kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.GistWithFiles>> =
    kotlinx.coroutines.flow.flowOf(emptyList())

  override fun searchLocalGists(
    query: String
  ): kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.GistWithFiles>> =
    kotlinx.coroutines.flow.flowOf(emptyList())

  override suspend fun getGistById(id: String): com.example.data.local.entity.GistWithFiles? = null

  override suspend fun insertGist(gist: com.example.data.local.entity.GistEntity) {}

  override suspend fun insertFiles(files: List<com.example.data.local.entity.GistFileEntity>) {}

  override suspend fun deleteFilesByGistId(gistId: String) {}

  override suspend fun deleteGistById(id: String) {}

  override suspend fun updateTags(id: String, tags: List<String>) {}

  override suspend fun upsertGistWithFiles(
    gist: com.example.data.local.entity.GistEntity,
    files: List<com.example.data.local.entity.GistFileEntity>
  ) {}

  override suspend fun getUnsynchronizedGists(): List<com.example.data.local.entity.GistWithFiles> =
    emptyList()

  override suspend fun getSyncedGists(): List<com.example.data.local.entity.GistWithFiles> =
    emptyList()

  override suspend fun deleteAllGists() {}

  override suspend fun deleteAllFiles() {}

  override suspend fun clearAllData() {}
}
