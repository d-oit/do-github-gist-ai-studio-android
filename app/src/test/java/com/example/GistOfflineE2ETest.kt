package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.pref.ConfigPrefs
import com.example.data.remote.api.GitHubApiService
import com.example.data.remote.model.GistOwnerResponse
import com.example.data.remote.model.GistRequest
import com.example.data.remote.model.GistResponse
import com.example.data.repository.GistRepository
import com.example.ui.viewmodel.GistViewModel
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GistOfflineE2ETest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private lateinit var database: AppDatabase
  private lateinit var configPrefs: ConfigPrefs
  private lateinit var fakeApiService: OfflineSimulatingGitHubApiService
  private lateinit var repository: GistRepository
  private lateinit var viewModel: GistViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()

    // Initialize in-memory database with synchronous direct executors for deterministic test
    // assertions
    database =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor { it.run() }
        .setTransactionExecutor { it.run() }
        .build()

    // Set up Config preferences with verified credentials
    configPrefs = ConfigPrefs(context)
    configPrefs.clear()
    configPrefs.setGithubToken("ghp_test_e2e_token_offline_sync_999")
    configPrefs.setOwnerLogin("testUser")
    configPrefs.setOwnerAvatarUrl("https://github.com/testUser.png")
    configPrefs.setOwnerId(12345)

    // Initialize our network-outage simulator API service
    fakeApiService = OfflineSimulatingGitHubApiService()

    // Construct Repository and ViewModel under test
    repository =
      GistRepository(
        gistDao = database.gistDao(),
        apiService = fakeApiService,
        configPrefs = configPrefs
      )

    viewModel =
      GistViewModel(
        repository = repository,
        configPrefs = configPrefs,
        appConfiguration =
          object : com.example.core.config.AppConfiguration {
            override fun geminiApiKeyOrNull(): String? = "fake_key"
          }
      )
  }

  @After
  fun tearDown() {
    database.close()
    configPrefs.clear()
    Dispatchers.resetMain()
  }

  @Test
  fun test_offline_create_read_update_delete_and_sync_transitions() =
    runTest(testDispatcher) {
      // ==========================================
      // 1. CREATE GIST WHILE OFFLINE (LOCAL DRAFT)
      // ==========================================
      fakeApiService.isOffline = true // Simulate absolute network outage

      val draftDesc = "Innovative Offline Gist Draft"
      val draftFilename = "offline_script.py"
      val draftContent = "print('Created completely offline')"

      viewModel.createGist(
        description = draftDesc,
        filename = draftFilename,
        content = draftContent,
        isPublic = true,
        isPinned = false
      )
      testDispatcher.scheduler.advanceUntilIdle()

      // Verify local draft is saved correctly in Room and visible to the UI State
      val offlineGists = viewModel.gists.value
      assertEquals(
        "Should contain exactly 1 local draft when created offline",
        1,
        offlineGists.size
      )

      val createdDraft = offlineGists.first()
      val draftId = createdDraft.gist.id
      assertTrue("Offline-created Gist must have a temp draft ID", draftId.startsWith("draft_"))
      assertEquals("Description should match expected", draftDesc, createdDraft.gist.description)
      assertTrue("isLocalOnly must be true for offline drafts", createdDraft.gist.isLocalOnly)
      assertFalse(
        "isDirty must be false for freshly created local drafts",
        createdDraft.gist.isDirty
      )
      assertEquals("Filename should match", draftFilename, createdDraft.files.first().filename)
      assertEquals("Content should match", draftContent, createdDraft.files.first().content)

      // ==========================================
      // 2. READ GIST WHILE OFFLINE
      // ==========================================
      // Verify that observing the Gist details offline works perfectly
      val observedGist = repository.getGist(draftId)
      assertNotNull("Observed Gist must not be null when read offline", observedGist)
      assertEquals(draftDesc, observedGist?.gist?.description)
      assertEquals(draftContent, observedGist?.files?.firstOrNull()?.content)

      // ==========================================
      // 3. UPDATE GIST WHILE OFFLINE
      // ==========================================
      // Scenario A: Update the local-only draft offline
      val updatedDraftDesc = "Innovative Offline Gist Draft (Modified v2)"
      val updatedDraftFilename = "offline_script_v2.py"
      val updatedDraftContent = "print('Created and updated offline')"

      viewModel.updateGist(
        id = draftId,
        description = updatedDraftDesc,
        filename = updatedDraftFilename,
        content = updatedDraftContent,
        isPublic = true,
        isPinned = true
      )
      testDispatcher.scheduler.advanceUntilIdle()

      // Assert that the draft itself was modified, but sync flags are unchanged (isLocalOnly=true,
      // isDirty=false)
      val updatedGists = viewModel.gists.value
      assertEquals(1, updatedGists.size)
      val draftAfterUpdate = updatedGists.first()
      assertEquals(draftId, draftAfterUpdate.gist.id)
      assertEquals(
        "Description should reflect the update",
        updatedDraftDesc,
        draftAfterUpdate.gist.description
      )
      assertTrue("Draft must remain local-only", draftAfterUpdate.gist.isLocalOnly)
      assertFalse("Draft must remain not dirty", draftAfterUpdate.gist.isDirty)
      assertEquals(updatedDraftFilename, draftAfterUpdate.files.first().filename)
      assertEquals(updatedDraftContent, draftAfterUpdate.files.first().content)
      assertTrue("Draft should now be pinned", draftAfterUpdate.gist.isPinned)

      // Scenario B: Update a previously synchronized Gist while offline
      // Pre-populate a synced Gist directly in Room first
      val syncedGistId = "remote_gist_id_abc_123"
      val syncedGistEntity =
        GistEntity(
          id = syncedGistId,
          description = "Synced remote Gist",
          htmlUrl = "https://gist.github.com/testUser/$syncedGistId",
          url = "https://api.github.com/gists/$syncedGistId",
          createdAt = "2026-07-09T09:00:00Z",
          updatedAt = "2026-07-09T09:00:00Z",
          nodeId = "node_abc",
          isPublic = true,
          isPinned = false,
          isLocalOnly = false,
          isDirty = false,
          ownerLogin = "testUser",
          ownerId = 12345,
          ownerAvatarUrl = "https://github.com/testUser.png"
        )
      val syncedFileEntity =
        GistFileEntity(
          fileId = "file_id_999",
          gistId = syncedGistId,
          filename = "app.kt",
          type = "text/plain",
          language = "Kotlin",
          rawUrl = "https://gist.github.com/raw/$syncedGistId/app.kt",
          size = 10L,
          content = "val x = 1"
        )
      database.gistDao().upsertGistWithFiles(syncedGistEntity, listOf(syncedFileEntity))
      testDispatcher.scheduler.advanceUntilIdle()

      // Perform local modification while offline
      val modifiedRemoteDesc = "Synced remote Gist with local offline modifications"
      val modifiedRemoteFilename = "app_v2.kt"
      val modifiedRemoteContent = "val x = 2 // offline change"

      viewModel.updateGist(
        id = syncedGistId,
        description = modifiedRemoteDesc,
        filename = modifiedRemoteFilename,
        content = modifiedRemoteContent,
        isPublic = true,
        isPinned = false
      )
      testDispatcher.scheduler.advanceUntilIdle()

      // Assert the previously synced Gist has now transitioned to 'isDirty = true'
      val listWithModifiedRemote = viewModel.gists.value
      assertEquals("Should have 2 gists in local storage", 2, listWithModifiedRemote.size)

      val modifiedRemoteGist = listWithModifiedRemote.find { it.gist.id == syncedGistId }
      assertNotNull("Modified remote Gist must exist in local cache", modifiedRemoteGist)
      assertEquals(
        "Description must show modifications",
        modifiedRemoteDesc,
        modifiedRemoteGist?.gist?.description
      )
      assertFalse(
        "Must NOT be localOnly as it originally exists on GitHub",
        modifiedRemoteGist?.gist?.isLocalOnly ?: true
      )
      assertTrue(
        "Must be flagged as isDirty because it has offline unsynced modifications",
        modifiedRemoteGist?.gist?.isDirty ?: false
      )
      assertEquals(modifiedRemoteFilename, modifiedRemoteGist?.files?.first()?.filename)
      assertEquals(modifiedRemoteContent, modifiedRemoteGist?.files?.first()?.content)

      // ==========================================
      // 4. DELETE GIST WHILE OFFLINE
      // ==========================================
      // Scenario A: Delete a local-only draft offline. It should be purged immediately.
      viewModel.deleteGist(draftId)
      testDispatcher.scheduler.advanceUntilIdle()

      val listAfterDraftDeletion = viewModel.gists.value
      assertNull(
        "Deleted draft must be completely purged from local storage",
        listAfterDraftDeletion.find { it.gist.id == draftId }
      )
      assertEquals("Only the modified remote gist should remain", 1, listAfterDraftDeletion.size)

      // Scenario B: Delete a previously synced Gist offline.
      // It should gracefully delete locally immediately, in preparation for eventual remote
      // reconciliation.
      viewModel.deleteGist(syncedGistId)
      testDispatcher.scheduler.advanceUntilIdle()

      val listAfterAllDeletions = viewModel.gists.value
      assertTrue("All Gists must be deleted from local storage", listAfterAllDeletions.isEmpty())

      // ==========================================
      // 5. RESTORE CONNECTION & SYNC STATE TRANSITIONS
      // ==========================================
      // Re-create a local draft and a dirty remote modification to verify sync transition
      val syncDraftDesc = "Draft waiting for sync"
      val syncDraftFile = "draft.kt"
      val syncDraftContent = "println('Sync me')"
      val syncDraftId =
        repository.createLocalDraft(
          syncDraftDesc,
          listOf(syncDraftFile to syncDraftContent),
          isPublic = true,
          isPinned = false
        )

      val syncRemoteId = "remote_sync_id_xyz"
      val syncRemoteEntity =
        GistEntity(
          id = syncRemoteId,
          description = "Synced remote Gist before edits",
          htmlUrl = "https://gist.github.com/testUser/$syncRemoteId",
          url = "https://api.github.com/gists/$syncRemoteId",
          createdAt = "2026-07-09T09:00:00Z",
          updatedAt = "2026-07-09T09:00:00Z",
          nodeId = "node_xyz",
          isPublic = true,
          isPinned = false,
          isLocalOnly = false,
          isDirty = false,
          ownerLogin = "testUser",
          ownerId = 12345,
          ownerAvatarUrl = "https://github.com/testUser.png"
        )
      val syncRemoteFile =
        GistFileEntity(
          fileId = "file_xyz",
          gistId = syncRemoteId,
          filename = "main.kt",
          type = "text/plain",
          language = "Kotlin",
          rawUrl = "https://gist.github.com/raw/$syncRemoteId/main.kt",
          size = 15L,
          content = "fun main() {}"
        )
      database.gistDao().upsertGistWithFiles(syncRemoteEntity, listOf(syncRemoteFile))
      testDispatcher.scheduler.advanceUntilIdle()

      // Edit the remote gist locally so it is marked as dirty
      val syncRemoteModifiedDesc = "Synced remote Gist with offline edits ready to sync"
      val syncRemoteModifiedContent = "fun main() { println('sync ready') }"
      repository.updateGistLocal(
        id = syncRemoteId,
        description = syncRemoteModifiedDesc,
        files = listOf("main.kt" to syncRemoteModifiedContent),
        isPublic = true,
        isPinned = false
      )
      testDispatcher.scheduler.advanceUntilIdle()

      // Ensure current state before sync
      val unsyncedBefore = database.gistDao().getUnsynchronizedGists()
      assertEquals(
        "Must have exactly 3 unsynced items before sync (1 deletion, 1 draft, 1 dirty)",
        3,
        unsyncedBefore.size
      )

      // RESTORE INTERNET CONNECTION
      fakeApiService.isOffline = false

      // Trigger SyncAll
      viewModel.syncAll()
      testDispatcher.scheduler.advanceUntilIdle()

      // Verify results after synchronization
      val finalGists = viewModel.gists.value
      assertEquals("Should have exactly 2 synchronized gists in local storage", 2, finalGists.size)

      // 1. Check local draft transition
      val syncedDraft = finalGists.find { it.gist.description == syncDraftDesc }
      assertNotNull("Draft should still exist in local DB (as a fully synced item)", syncedDraft)
      val newSyncedId = syncedDraft?.gist?.id ?: ""
      assertFalse(
        "Synced draft must NOT have a temp draft ID anymore",
        newSyncedId.startsWith("draft_")
      )
      assertFalse(
        "Synced draft must have isLocalOnly reset to false",
        syncedDraft?.gist?.isLocalOnly ?: true
      )
      assertFalse(
        "Synced draft must have isDirty reset to false",
        syncedDraft?.gist?.isDirty ?: true
      )
      assertEquals(
        "Filename should be preserved",
        syncDraftFile,
        syncedDraft?.files?.firstOrNull()?.filename
      )
      assertEquals(
        "Content should be preserved",
        syncDraftContent,
        syncedDraft?.files?.firstOrNull()?.content
      )

      // 2. Check dirty edits transition
      val syncedDirtyGist = finalGists.find { it.gist.id == syncRemoteId }
      assertNotNull("Dirty remote Gist should still exist", syncedDirtyGist)
      assertFalse(
        "Synced dirty Gist must have isLocalOnly = false",
        syncedDirtyGist?.gist?.isLocalOnly ?: true
      )
      assertFalse(
        "Synced dirty Gist must have isDirty reset to false",
        syncedDirtyGist?.gist?.isDirty ?: true
      )
      assertEquals(
        "Description should reflect updated content",
        syncRemoteModifiedDesc,
        syncedDirtyGist?.gist?.description
      )
      assertEquals(
        "Content should reflect updated content",
        syncRemoteModifiedContent,
        syncedDirtyGist?.files?.firstOrNull()?.content
      )

      // Verify remote API actually received the creations and updates
      val remoteGists = fakeApiService.getGists()
      val remoteDraftUpload = remoteGists.find { it.description == syncDraftDesc }
      assertNotNull("GitHub API must have received the local draft creation", remoteDraftUpload)
      assertEquals(syncDraftContent, remoteDraftUpload?.files?.get(syncDraftFile)?.content)

      val remoteDirtyUpdate = remoteGists.find { it.id == syncRemoteId }
      assertNotNull("GitHub API must have received the local update patch", remoteDirtyUpdate)
      assertEquals(syncRemoteModifiedDesc, remoteDirtyUpdate?.description)
      assertEquals(syncRemoteModifiedContent, remoteDirtyUpdate?.files?.get("main.kt")?.content)

      // Check unsynchronized lists is now completely empty
      val unsyncedAfter = database.gistDao().getUnsynchronizedGists()
      assertTrue(
        "All local unsynced states must be empty after full synchronization",
        unsyncedAfter.isEmpty()
      )
    }

  // --- Offline Simulating GitHub API Service Stub ---
  private class OfflineSimulatingGitHubApiService : GitHubApiService {
    var isOffline = false
    val gistsList = mutableListOf<GistResponse>()
    val defaultOwner =
      GistOwnerResponse(
        login = "testUser",
        id = 12345,
        avatarUrl = "https://github.com/testUser.png"
      )

    private fun checkOffline() {
      if (isOffline) {
        throw IOException("No internet connection - Failed to connect to api.github.com")
      }
    }

    override suspend fun getGists(page: Int?, perPage: Int?): List<GistResponse> {
      checkOffline()
      return gistsList
    }

    override suspend fun getGist(id: String): GistResponse {
      checkOffline()
      return gistsList.find { it.id == id } ?: throw Exception("Gist Not Found")
    }

    override suspend fun getGistRevision(id: String, sha: String): GistResponse {
      checkOffline()
      return getGist(id)
    }

    override suspend fun getAuthenticatedUser(): GistOwnerResponse {
      checkOffline()
      return defaultOwner
    }

    override suspend fun createGist(request: GistRequest): GistResponse {
      checkOffline()
      val newGist =
        GistResponse(
          id = UUID.randomUUID().toString(),
          description = request.description,
          htmlUrl = "https://gist.github.com/testUser/remote_" + UUID.randomUUID().toString(),
          url = "https://api.github.com/gists/remote_" + UUID.randomUUID().toString(),
          createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
          updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
          nodeId = "node_" + UUID.randomUUID().toString(),
          isPublic = request.isPublic,
          owner = defaultOwner,
          files =
            request.files.mapValues { (name, fileReq) ->
              com.example.data.remote.model.GistFileResponse(
                filename = name,
                type = "text/plain",
                language = "Kotlin",
                rawUrl = "https://gist.github.com/raw/remote_gist/" + name,
                size = fileReq?.content?.length?.toLong() ?: 0L,
                content = fileReq?.content
              )
            }
        )
      gistsList.add(newGist)
      return newGist
    }

    override suspend fun updateGist(id: String, request: GistRequest): GistResponse {
      checkOffline()
      val updatedGist =
        GistResponse(
          id = id,
          description = request.description,
          htmlUrl = "https://gist.github.com/testUser/$id",
          url = "https://api.github.com/gists/$id",
          createdAt = "2026-07-09T09:00:00Z",
          updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
          nodeId = "node_id",
          isPublic = request.isPublic,
          owner = defaultOwner,
          files =
            request.files.mapValues { (name, fileReq) ->
              com.example.data.remote.model.GistFileResponse(
                filename = name,
                type = "text/plain",
                language = "Kotlin",
                rawUrl = "https://gist.github.com/raw/$id/$name",
                size = fileReq?.content?.length?.toLong() ?: 0L,
                content = fileReq?.content
              )
            }
        )
      gistsList.removeAll { it.id == id }
      gistsList.add(updatedGist)
      return updatedGist
    }

    override suspend fun deleteGist(id: String): Response<Unit> {
      checkOffline()
      gistsList.removeAll { it.id == id }
      return Response.success(Unit)
    }

    val starredGistIds = mutableSetOf<String>()

    override suspend fun checkIsStarred(id: String): Response<Unit> {
      checkOffline()
      return if (starredGistIds.contains(id)) {
        Response.success(204, Unit)
      } else {
        Response.error(404, okhttp3.ResponseBody.create(null, ""))
      }
    }

    override suspend fun starGist(id: String): Response<Unit> {
      checkOffline()
      starredGistIds.add(id)
      return Response.success(204, Unit)
    }

    override suspend fun unstarGist(id: String): Response<Unit> {
      checkOffline()
      starredGistIds.remove(id)
      return Response.success(204, Unit)
    }

    override suspend fun forkGist(id: String): GistResponse {
      checkOffline()
      val original = getGist(id)
      val newId = "fork_" + UUID.randomUUID().toString()
      val forked = original.copy(
        id = newId,
        htmlUrl = "https://gist.github.com/testUser/$newId",
        url = "https://api.github.com/gists/$newId"
      )
      gistsList.add(forked)
      return forked
    }
  }
}
