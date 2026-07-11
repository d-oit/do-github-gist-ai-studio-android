package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import com.example.data.remote.api.GitHubApiService
import com.example.data.remote.model.GistRequest
import com.example.data.remote.model.GistResponse
import com.example.data.remote.model.GistOwnerResponse
import com.example.ui.viewmodel.GistViewModel
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
import java.util.UUID

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
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
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
        repository = GistRepository(
            gistDao = database.gistDao(),
            apiService = fakeApiService,
            configPrefs = configPrefs
        )

        viewModel = GistViewModel(repository, configPrefs)
    }

    @After
    fun tearDown() {
        database.close()
        configPrefs.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun test_verifyToken_savesPermanently_and_restoresOnRestart() = runTest(testDispatcher) {
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
        val newViewModel = GistViewModel(repository, newPrefs)

        // Verify loaded states match exactly
        assertEquals(targetToken, newViewModel.token.value)
        assertEquals("testUser", newViewModel.ownerLogin.value)
        assertEquals("https://github.com/testUser.png", newViewModel.ownerAvatar.value)
    }

    @Test
    fun test_clearConfig_wipesEverything() = runTest(testDispatcher) {
        // Pre-populate configs
        configPrefs.setGithubToken("test_token")
        configPrefs.setOwnerLogin("someUser")
        configPrefs.setOwnerAvatarUrl("https://avatar.png")

        // Re-initialize VM to load populated configs
        viewModel = GistViewModel(repository, configPrefs)
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
    fun test_saveConfig_savesManually() = runTest(testDispatcher) {
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
    fun test_createLocalDraft_addsToList() = runTest(testDispatcher) {
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
    fun test_syncAll_uploadsLocalDraftsToGitHub() = runTest(testDispatcher) {
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
    fun test_togglePin_works() = runTest(testDispatcher) {
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

    // --- Fake GitHub API Service Implementation ---
    private class FakeGitHubApiService : GitHubApiService {
        var userResponse = GistOwnerResponse(
            login = "testUser",
            id = 12345,
            avatarUrl = "https://github.com/testUser.png"
        )
        val gistsList = mutableListOf<GistResponse>()
        var shouldFailUser = false

        override suspend fun getGists(): List<GistResponse> = gistsList

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
            val newGist = GistResponse(
                id = UUID.randomUUID().toString(),
                description = request.description,
                htmlUrl = "https://gist.github.com/testUser/some_id",
                url = "https://api.github.com/gists/some_id",
                createdAt = "2026-07-09T09:00:00Z",
                updatedAt = "2026-07-09T09:00:00Z",
                nodeId = "node_id",
                isPublic = request.isPublic,
                owner = userResponse,
                files = request.files.mapValues { (name, fileReq) ->
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
            val updatedGist = GistResponse(
                id = id,
                description = request.description,
                htmlUrl = "https://gist.github.com/testUser/$id",
                url = "https://api.github.com/gists/$id",
                createdAt = "2026-07-09T09:00:00Z",
                updatedAt = "2026-07-09T09:00:00Z",
                nodeId = "node_id",
                isPublic = request.isPublic,
                owner = userResponse,
                files = request.files.mapValues { (name, fileReq) ->
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
    }
}
