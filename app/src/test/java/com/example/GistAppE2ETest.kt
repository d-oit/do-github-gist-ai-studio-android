package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.GistEntity
import com.example.data.local.pref.ConfigPrefs
import com.example.data.remote.api.GitHubApiService
import com.example.data.remote.model.GistOwnerResponse
import com.example.data.remote.model.GistRequest
import com.example.data.remote.model.GistResponse
import com.example.data.repository.GistRepository
import com.example.ui.viewmodel.GistViewModel
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class GistAppE2ETest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private lateinit var database: AppDatabase
  private lateinit var configPrefs: ConfigPrefs
  private lateinit var fakeApiService: FakeGitHubApiService
  private lateinit var repository: GistRepository
  private lateinit var viewModel: GistViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()

    // 1. In-memory Database with direct executors to ensure synchronous flow emissions in tests
    database =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor { it.run() }
        .setTransactionExecutor { it.run() }
        .build()

    // 2. Clear Config SharedPreferences
    configPrefs = ConfigPrefs(context)
    configPrefs.clear()

    // 3. Setup Fake Api Service
    fakeApiService = FakeGitHubApiService()

    // 4. Initialize Repository and ViewModel
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
    configPrefs.overrideToken = null
    database.close()
    configPrefs.clear()
    Dispatchers.resetMain()
  }

  @Test
  fun test_verifyToken_savesPermanently_and_restoresOnRestart() =
    runTest(testDispatcher) {
      val targetToken = "ghp_secure_personal_access_token_12345"

      // Step 1: Update token in ViewModel
      viewModel.updateToken(targetToken)
      assertEquals(targetToken, viewModel.token.value)

      // Step 2: Trigger verify token and autofill profile
      viewModel.validateAndFetchProfile()

      // Let coroutines execute
      testDispatcher.scheduler.advanceUntilIdle()

      // Step 3: Assert UI flows and SharedPreferences updated
      assertEquals("testUser", viewModel.ownerLogin.value)
      assertEquals("https://github.com/testUser.png", viewModel.ownerAvatar.value)

      // Check SharedPreferences / ConfigPrefs directly to ensure permanent storage
      assertEquals(targetToken, configPrefs.getGithubToken())
      assertEquals("testUser", configPrefs.getOwnerLogin())
      assertEquals("https://github.com/testUser.png", configPrefs.getOwnerAvatarUrl())

      // Step 4: Simulate App Restart / New ViewModel instantiation
      val newPrefs = ConfigPrefs(context)
      val newViewModel =
        GistViewModel(
          repository = repository,
          configPrefs = newPrefs,
          appConfiguration =
            object : com.example.core.config.AppConfiguration {
              override fun geminiApiKeyOrNull(): String? = "fake_key"
            }
        )

      // Verify loaded states match exactly
      assertEquals(targetToken, newViewModel.token.value)
      assertEquals("testUser", newViewModel.ownerLogin.value)
      assertEquals("https://github.com/testUser.png", newViewModel.ownerAvatar.value)
    }

  @Test
  fun test_clearConfig_wipesEverything() =
    runTest(testDispatcher) {
      // Pre-populate configs
      configPrefs.setGithubToken("test_token")
      configPrefs.setOwnerLogin("someUser")
      configPrefs.setOwnerAvatarUrl("https://avatar.png")

      // Re-initialize VM to load populated configs
      viewModel =
        GistViewModel(
          repository = repository,
          configPrefs = configPrefs,
          appConfiguration =
            object : com.example.core.config.AppConfiguration {
              override fun geminiApiKeyOrNull(): String? = "fake_key"
            }
        )
      testDispatcher.scheduler.advanceUntilIdle()
      assertEquals("test_token", viewModel.token.value)
      assertEquals("someUser", viewModel.ownerLogin.value)

      // Trigger Clear Config
      viewModel.clearConfig()
      testDispatcher.scheduler.advanceUntilIdle()

      // Assert VM flows are wiped
      assertEquals("", viewModel.token.value)
      assertEquals("anonymous", viewModel.ownerLogin.value)
      assertEquals("", viewModel.ownerAvatar.value)

      // Assert ConfigPrefs are permanently cleared
      assertEquals("", configPrefs.getGithubToken())
      assertEquals("anonymous", configPrefs.getOwnerLogin())
      assertEquals("", configPrefs.getOwnerAvatarUrl())
    }

  @Test
  fun test_saveConfig_savesManually() =
    runTest(testDispatcher) {
      viewModel.updateToken("custom_token")
      viewModel.updateOwnerLogin("customUser")
      viewModel.updateOwnerAvatar("https://custom_avatar.png")

      viewModel.saveConfig()
      testDispatcher.scheduler.advanceUntilIdle()

      assertEquals("custom_token", configPrefs.getGithubToken())
      assertEquals("customUser", configPrefs.getOwnerLogin())
      assertEquals("https://custom_avatar.png", configPrefs.getOwnerAvatarUrl())
    }

  @Test
  fun test_createLocalDraft_addsToList() =
    runTest(testDispatcher) {
      val desc = "My beautiful test draft"
      val filename = "test_script.py"
      val code = "print('Hello E2E')"

      // Create draft
      viewModel.createGist(
        description = desc,
        filename = filename,
        content = code,
        isPublic = false,
        isPinned = false
      )
      testDispatcher.scheduler.advanceUntilIdle()

      // Get the list of gists
      val currentGists = viewModel.gists.value
      assertEquals(1, currentGists.size)

      val draft = currentGists.first()
      assertEquals(desc, draft.gist.description)
      assertTrue(draft.gist.isLocalOnly)
      assertFalse(draft.gist.isDirty) // freshly created local drafts are just localOnly
      assertEquals(1, draft.files.size)
      assertEquals(filename, draft.files.first().filename)
      assertEquals(code, draft.files.first().content)
    }

  @Test
  fun test_syncAll_uploadsLocalDraftsToGitHub() =
    runTest(testDispatcher) {
      // Configure credentials
      configPrefs.setGithubToken("ghp_valid_token")
      configPrefs.setOwnerLogin("testUser")

      // Create a local draft first
      viewModel.createGist(
        description = "E2E Sync Draft",
        filename = "sync.kt",
        content = "fun main() {}",
        isPublic = true,
        isPinned = false
      )
      testDispatcher.scheduler.advanceUntilIdle()

      // Check it starts as localOnly
      assertTrue(viewModel.gists.value.first().gist.isLocalOnly)

      // Trigger Sync
      viewModel.syncAll()
      testDispatcher.scheduler.advanceUntilIdle()

      // Check it has successfully synced and is no longer localOnly
      val syncedList = viewModel.gists.value
      assertEquals(1, syncedList.size)
      val syncedGist = syncedList.first()

      assertFalse(syncedGist.gist.isLocalOnly)
      assertFalse(syncedGist.gist.isDirty)
      assertNotNull(syncedGist.gist.id)
      assertFalse(syncedGist.gist.id.startsWith("draft_"))

      // Verify fake api service registered the upload
      assertEquals(1, fakeApiService.gistsList.size)
      assertEquals("E2E Sync Draft", fakeApiService.gistsList.first().description)
    }

  @Test
  fun test_togglePin_works() =
    runTest(testDispatcher) {
      viewModel.createGist(
        description = "Pinnable Gist",
        filename = "pin.txt",
        content = "pinned",
        isPublic = true,
        isPinned = false
      )
      testDispatcher.scheduler.advanceUntilIdle()

      val gistId = viewModel.gists.value.first().gist.id
      assertFalse(viewModel.gists.value.first().gist.isPinned)

      // Toggle PIN to true
      viewModel.togglePin(gistId)
      testDispatcher.scheduler.advanceUntilIdle()
      assertTrue(viewModel.gists.value.first().gist.isPinned)

      // Toggle PIN back to false
      viewModel.togglePin(gistId)
      testDispatcher.scheduler.advanceUntilIdle()
      assertFalse(viewModel.gists.value.first().gist.isPinned)
    }

  @Test
  fun test_toggleStar_online_immediate_sync() =
    runTest(testDispatcher) {
      configPrefs.setGithubToken("ghp_valid_token")
      configPrefs.setOwnerLogin("testUser")

      // 1. Create draft and sync to make it remote
      viewModel.createGist(
        description = "Online Starrable Gist",
        filename = "star_online.txt",
        content = "online content",
        isPublic = true,
        isPinned = false
      )
      testDispatcher.scheduler.advanceUntilIdle()

      viewModel.syncAll()
      testDispatcher.scheduler.advanceUntilIdle()

      val syncedItem = viewModel.gists.value.first { !it.gist.id.startsWith("draft_") }
      val gistId = syncedItem.gist.id

      // 2. Toggle star while online
      viewModel.toggleStar(gistId)
      testDispatcher.scheduler.advanceUntilIdle()

      // 3. Since we are online, it should sync immediately!
      val starredItem = viewModel.gists.value.first { it.gist.id == gistId }
      assertTrue(starredItem.gist.isStarred)
      assertFalse(starredItem.gist.isStarredDirty) // Immediately synced, so not dirty!
      assertTrue(fakeApiService.starredGistIds.contains(gistId))
    }

  @Test
  fun test_toggleStar_offline_delayed_sync() =
    runTest(testDispatcher) {
      // Clear token to simulate offline/unauthorized state
      configPrefs.overrideToken = ""
      configPrefs.setGithubToken("")
      configPrefs.setOwnerLogin("testUser")

      // 1. Create a remote-like gist manually in DB
      val mockGistId = "remote_123"
      val now = "2026-07-09T09:00:00Z"
      val entity =
        GistEntity(
          id = mockGistId,
          description = "Offline Starrable Gist",
          htmlUrl = "https://gist.github.com/mockGistId",
          url = "https://api.github.com/gists/mockGistId",
          createdAt = now,
          updatedAt = now,
          nodeId = "mock_node",
          isPublic = true,
          isPinned = false,
          isLocalOnly = false,
          isDirty = false,
          isStarred = false,
          isStarredDirty = false,
          ownerLogin = "testUser",
          ownerId = 12345,
          ownerAvatarUrl = ""
        )
      database.gistDao().insertGist(entity)
      testDispatcher.scheduler.advanceUntilIdle()

      // Also add it to our fake api service so it exists when sync fetches
      fakeApiService.gistsList.add(
        GistResponse(
          id = mockGistId,
          description = "Offline Starrable Gist",
          htmlUrl = "https://gist.github.com/mockGistId",
          url = "https://api.github.com/gists/mockGistId",
          createdAt = now,
          updatedAt = now,
          nodeId = "mock_node",
          isPublic = true,
          owner = fakeApiService.userResponse,
          files = emptyMap()
        )
      )

      // 2. Toggle star while offline (no token)
      viewModel.toggleStar(mockGistId)
      testDispatcher.scheduler.advanceUntilIdle()

      val starredItem = viewModel.gists.value.first { it.gist.id == mockGistId }
      assertTrue(starredItem.gist.isStarred)
      assertTrue(starredItem.gist.isStarredDirty) // Must be dirty because it couldn't push offline!
      assertFalse(fakeApiService.starredGistIds.contains(mockGistId)) // Not pushed yet

      // 3. Restore token (go online) and sync
      configPrefs.overrideToken = null
      configPrefs.setGithubToken("ghp_valid_token")
      viewModel.syncAll()
      testDispatcher.scheduler.advanceUntilIdle()

      val syncedItem = viewModel.gists.value.first { it.gist.id == mockGistId }
      assertTrue(syncedItem.gist.isStarred)
      assertFalse(syncedItem.gist.isStarredDirty) // Now cleared post successful sync!
      assertTrue(fakeApiService.starredGistIds.contains(mockGistId)) // Successfully pushed!
    }

  @Test
  fun test_exportBackup_saves_successfully() =
    runTest(testDispatcher) {
      configPrefs.setGithubToken("ghp_valid_token")
      configPrefs.setOwnerLogin("testUser")

      // 1. Create a local draft Gist
      viewModel.createGist(
        description = "Gist for Backup Test",
        filename = "backup_test.txt",
        content = "This content should be backed up.",
        isPublic = true,
        isPinned = true
      )
      testDispatcher.scheduler.advanceUntilIdle()

      // Verify it was added
      val currentGists = viewModel.gists.value
      assertEquals(1, currentGists.size)

      // 2. Prepare a temporary file and its Uri
      val tempFile = java.io.File.createTempFile("gist_backup_test", ".json")
      val uri = android.net.Uri.fromFile(tempFile)

      // 3. Trigger exportBackup
      var isSuccess = false
      var resultMsg = ""
      viewModel.exportBackup(context, uri, testDispatcher) { success, msg ->
        isSuccess = success
        resultMsg = msg
      }
      testDispatcher.scheduler.advanceUntilIdle()

      // 4. Assertions
      assertTrue("Backup should have succeeded: $resultMsg", isSuccess)
      assertTrue("Backup file must exist", tempFile.exists())
      assertTrue("Backup file size must be greater than 0", tempFile.length() > 0)

      // Read the written JSON file content
      val fileContent = tempFile.readText()
      assertTrue("JSON should contain backupVersion", fileContent.contains("\"backupVersion\""))
      assertTrue(
        "JSON should contain the correct description",
        fileContent.contains("Gist for Backup Test")
      )
      assertTrue(
        "JSON should contain the correct filename",
        fileContent.contains("backup_test.txt")
      )
      assertTrue(
        "JSON should contain the correct content",
        fileContent.contains("This content should be backed up.")
      )

      // Clean up
      tempFile.delete()
    }

  @Test
  fun test_forkGist_createsFork_andSavesLocally() =
    runTest(testDispatcher) {
      // Step 1: Add a public Gist to fakeApiService that belongs to someone else
      val otherUserGist =
        GistResponse(
          id = "original_gist_123",
          description = "This is a public gist to fork",
          htmlUrl = "https://gist.github.com/otherUser/original_gist_123",
          url = "https://api.github.com/gists/original_gist_123",
          createdAt = "2026-07-09T09:00:00Z",
          updatedAt = "2026-07-09T09:00:00Z",
          nodeId = "node_id",
          isPublic = true,
          owner =
            GistOwnerResponse(
              login = "otherUser",
              id = 67890,
              avatarUrl = "https://github.com/otherUser.png"
            ),
          files =
            mapOf(
              "code.py" to
                com.example.data.remote.model.GistFileResponse(
                  filename = "code.py",
                  type = "text/plain",
                  language = "Python",
                  rawUrl = "https://gist.github.com/raw/original_gist_123/code.py",
                  size = 100L,
                  content = "print('Hello Fork')"
                )
            )
        )
      fakeApiService.gistsList.add(otherUserGist)
      configPrefs.setGithubToken("ghp_test_token_123")

      // Step 2: Trigger forkGist from ViewModel
      viewModel.forkGist("original_gist_123")
      testDispatcher.scheduler.advanceUntilIdle()

      // Step 3: Verify success state is updated in Repository
      val currentSyncStatus = repository.syncStatus.value
      assertTrue(
        "SyncStatus should be Success",
        currentSyncStatus is com.example.data.repository.SyncStatus.Success
      )
      val successMsg = (currentSyncStatus as com.example.data.repository.SyncStatus.Success).message
      assertTrue("Message should contain success word", successMsg.contains("forked"))

      // Step 4: Verify the newly created fork exists in the local room database
      val localGists = repository.allGists.first()
      val forkedLocal = localGists.find { it.gist.description == "This is a public gist to fork" }
      assertNotNull("Forked Gist should be saved locally", forkedLocal)
      assertEquals(1, forkedLocal?.files?.size)
      assertEquals("code.py", forkedLocal?.files?.first()?.filename)
      assertEquals("print('Hello Fork')", forkedLocal?.files?.first()?.content)
    }

  // --- Fake GitHub API Service Implementation ---
  private class FakeGitHubApiService : GitHubApiService {
    var userResponse =
      GistOwnerResponse(
        login = "testUser",
        id = 12345,
        avatarUrl = "https://github.com/testUser.png"
      )
    val gistsList = mutableListOf<GistResponse>()
    var shouldFailUser = false

    override suspend fun getGists(page: Int?, perPage: Int?): List<GistResponse> = gistsList

    override suspend fun getGist(id: String): GistResponse {
      return gistsList.find { it.id == id } ?: throw Exception("Gist Not Found")
    }

    override suspend fun getGistRevision(id: String, sha: String): GistResponse {
      return getGist(id)
    }

    override suspend fun getAuthenticatedUser(): GistOwnerResponse {
      if (shouldFailUser) throw Exception("Unauthorized")
      return userResponse
    }

    override suspend fun createGist(request: GistRequest): GistResponse {
      val newGist =
        GistResponse(
          id = UUID.randomUUID().toString(),
          description = request.description,
          htmlUrl = "https://gist.github.com/testUser/some_id",
          url = "https://api.github.com/gists/some_id",
          createdAt = "2026-07-09T09:00:00Z",
          updatedAt = "2026-07-09T09:00:00Z",
          nodeId = "node_id",
          isPublic = request.isPublic,
          owner = userResponse,
          files =
            request.files.mapValues { (name, fileReq) ->
              com.example.data.remote.model.GistFileResponse(
                filename = name,
                type = "text/plain",
                language = "Kotlin",
                rawUrl = "https://gist.github.com/raw/some_id",
                size = fileReq?.content?.length?.toLong() ?: 0L,
                content = fileReq?.content
              )
            }
        )
      gistsList.add(newGist)
      return newGist
    }

    override suspend fun updateGist(id: String, request: GistRequest): GistResponse {
      val updatedGist =
        GistResponse(
          id = id,
          description = request.description,
          htmlUrl = "https://gist.github.com/testUser/$id",
          url = "https://api.github.com/gists/$id",
          createdAt = "2026-07-09T09:00:00Z",
          updatedAt = "2026-07-09T09:00:00Z",
          nodeId = "node_id",
          isPublic = request.isPublic,
          owner = userResponse,
          files =
            request.files.mapValues { (name, fileReq) ->
              com.example.data.remote.model.GistFileResponse(
                filename = name,
                type = "text/plain",
                language = "Kotlin",
                rawUrl = "https://gist.github.com/raw/$id",
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
      gistsList.removeAll { it.id == id }
      return Response.success(Unit)
    }

    val starredGistIds = mutableSetOf<String>()

    override suspend fun checkIsStarred(id: String): Response<Unit> {
      return if (starredGistIds.contains(id)) {
        Response.success(204, Unit)
      } else {
        Response.error(404, okhttp3.ResponseBody.create(null, ""))
      }
    }

    override suspend fun starGist(id: String): Response<Unit> {
      starredGistIds.add(id)
      return Response.success(204, Unit)
    }

    override suspend fun unstarGist(id: String): Response<Unit> {
      starredGistIds.remove(id)
      return Response.success(204, Unit)
    }

    override suspend fun forkGist(id: String): GistResponse {
      val original = getGist(id)
      val newId = "fork_" + UUID.randomUUID().toString()
      val forked =
        original.copy(
          id = newId,
          htmlUrl = "https://gist.github.com/testUser/$newId",
          url = "https://api.github.com/gists/$newId"
        )
      gistsList.add(forked)
      return forked
    }
  }
}
