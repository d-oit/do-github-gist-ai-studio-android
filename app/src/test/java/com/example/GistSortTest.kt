package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.dao.GistDao
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import com.example.ui.screens.GistHubAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GistSortOption
import com.example.ui.viewmodel.GistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GistSortTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var db: AppDatabase
  private lateinit var gistDao: GistDao
  private lateinit var repository: GistRepository
  private lateinit var configPrefs: ConfigPrefs
  private lateinit var viewModel: GistViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    org.robolectric.shadows.ShadowLog.stream = System.out
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    db =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor { it.run() }
        .setTransactionExecutor { it.run() }
        .build()
    gistDao = db.gistDao()
    configPrefs = ConfigPrefs(context)
    repository = GistRepository(gistDao, FakeGitHubApiService(), configPrefs)
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
    db.close()
    Dispatchers.resetMain()
  }

  @Test
  fun test_defaultSortOption_isRecentlyUpdated() {
    assertEquals(GistSortOption.RECENTLY_UPDATED, viewModel.sortOption.value)
  }

  @Test
  fun test_updateSortOption_changesState() {
    viewModel.updateSortOption(GistSortOption.TITLE)
    assertEquals(GistSortOption.TITLE, viewModel.sortOption.value)

    viewModel.updateSortOption(GistSortOption.CREATED_DATE)
    assertEquals(GistSortOption.CREATED_DATE, viewModel.sortOption.value)
  }

  @Test
  fun test_sortComparators() {
    val item1 = createSampleGist("1", "Alpha", "2026-01-01T10:00:00Z", "2026-01-05T10:00:00Z")
    val item2 = createSampleGist("2", "Zulu", "2026-01-03T10:00:00Z", "2026-01-03T10:00:00Z")
    val item3 = createSampleGist("3", "Bravo", "2026-01-02T10:00:00Z", "2026-01-08T10:00:00Z")

    val list = listOf(item1, item2, item3)

    // Sort by Recently Updated (item3 -> item1 -> item2)
    val byUpdated =
      list.sortedWith(
        compareByDescending<GistWithFiles> { it.gist.isPinned }
          .thenByDescending { it.gist.updatedAt.ifBlank { it.gist.createdAt } }
      )
    assertEquals("3", byUpdated[0].gist.id)
    assertEquals("1", byUpdated[1].gist.id)
    assertEquals("2", byUpdated[2].gist.id)

    // Sort by Created Date (item2 -> item3 -> item1)
    val byCreated =
      list.sortedWith(
        compareByDescending<GistWithFiles> { it.gist.isPinned }
          .thenByDescending { it.gist.createdAt }
      )
    assertEquals("2", byCreated[0].gist.id)
    assertEquals("3", byCreated[1].gist.id)
    assertEquals("1", byCreated[2].gist.id)

    // Sort by Title (Alpha -> Bravo -> Zulu)
    val byTitle =
      list.sortedWith(
        compareByDescending<GistWithFiles> { it.gist.isPinned }
          .thenBy {
            (it.gist.description?.takeIf { d -> d.isNotBlank() }
                ?: it.files.firstOrNull()?.filename
                ?: "Untitled Gist")
              .lowercase()
          }
      )
    assertEquals("1", byTitle[0].gist.id)
    assertEquals("3", byTitle[1].gist.id)
    assertEquals("2", byTitle[2].gist.id)
  }

  @Test
  fun test_sortMenuUI_rendersAndOptionSelection() {
    composeTestRule.setContent { MyApplicationTheme { GistHubAppScreen(viewModel = viewModel) } }

    // Verify Sort menu button exists
    composeTestRule
      .onNodeWithTag("sort_menu_button", useUnmergedTree = true)
      .assertExists()
      .performClick()

    // Idle the Robolectric main looper to process the window addition for Popups
    org.robolectric.shadows.ShadowLooper.idleMainLooper()

    // Advance clock to allow dropdown animation to complete under Robolectric
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Verify options exist in dropdown
    composeTestRule
      .onNodeWithTag("sort_option_recently_updated", useUnmergedTree = true)
      .assertExists()
    composeTestRule.onNodeWithTag("sort_option_created_date", useUnmergedTree = true).assertExists()
    composeTestRule
      .onNodeWithTag("sort_option_title", useUnmergedTree = true)
      .assertExists()
      .performClick()
    composeTestRule.waitForIdle()

    // Verify ViewModel state updated
    assertEquals(GistSortOption.TITLE, viewModel.sortOption.value)
  }

  private fun createSampleGist(
    id: String,
    description: String,
    createdAt: String,
    updatedAt: String
  ): GistWithFiles {
    val gist =
      GistEntity(
        id = id,
        description = description,
        htmlUrl = "https://gist.github.com/$id",
        url = "https://api.github.com/gists/$id",
        createdAt = createdAt,
        updatedAt = updatedAt,
        nodeId = "node_$id",
        isPublic = true,
        isPinned = false,
        isLocalOnly = false,
        isDirty = false,
        isDeleted = false,
        isStarred = false,
        isStarredDirty = false,
        tags = emptyList(),
        ownerLogin = "testuser",
        ownerId = 123,
        ownerAvatarUrl = "https://avatar.com/user"
      )
    val file =
      GistFileEntity(
        fileId = "file_$id",
        gistId = id,
        filename = "$description.txt",
        type = "text/plain",
        language = "Text",
        rawUrl = "",
        size = 10L,
        content = "content"
      )
    return GistWithFiles(gist, listOf(file))
  }

  private class FakeGitHubApiService : com.example.data.remote.api.GitHubApiService {
    override suspend fun getGists(
      page: Int?,
      perPage: Int?
    ): List<com.example.data.remote.model.GistResponse> = emptyList()

    override suspend fun getGist(id: String) = throw Exception()

    override suspend fun getGistRevision(id: String, sha: String) = throw Exception()

    override suspend fun getAuthenticatedUser() = throw Exception()

    override suspend fun createGist(request: com.example.data.remote.model.GistRequest) =
      throw Exception()

    override suspend fun updateGist(
      id: String,
      request: com.example.data.remote.model.GistRequest
    ) = throw Exception()

    override suspend fun deleteGist(id: String): retrofit2.Response<Unit> =
      retrofit2.Response.success(Unit)

    override suspend fun checkIsStarred(id: String) = throw Exception()

    override suspend fun starGist(id: String) = throw Exception()

    override suspend fun unstarGist(id: String) = throw Exception()

    override suspend fun forkGist(id: String): com.example.data.remote.model.GistResponse =
      throw Exception()
  }
}
