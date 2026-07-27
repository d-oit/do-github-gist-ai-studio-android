package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.dao.GistDao
import com.example.data.local.dao.SearchHistoryDao
import com.example.data.local.entity.SearchHistoryEntity
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import com.example.ui.screens.GistHubAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GistViewModel
import com.example.ui.viewmodel.clearSearchHistory
import com.example.ui.viewmodel.deleteSearchQuery
import com.example.ui.viewmodel.saveSearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SearchHistoryTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var db: AppDatabase
  private lateinit var gistDao: GistDao
  private lateinit var searchHistoryDao: SearchHistoryDao
  private lateinit var repository: GistRepository
  private lateinit var configPrefs: ConfigPrefs
  private lateinit var viewModel: GistViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    db =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor { it.run() }
        .setTransactionExecutor { it.run() }
        .build()
    gistDao = db.gistDao()
    searchHistoryDao = db.searchHistoryDao()
    configPrefs = ConfigPrefs(context)
    repository =
      GistRepository(
        gistDao = gistDao,
        apiService = FakeGitHubApiService(),
        configPrefs = configPrefs,
        searchHistoryDao = searchHistoryDao
      )
    viewModel =
      GistViewModel(
        repository = repository,
        configPrefs = configPrefs,
        appConfiguration = com.example.core.config.AppConfigurationImpl()
      )
  }

  @After
  fun tearDown() {
    db.close()
    Dispatchers.resetMain()
  }

  @Test
  fun testRoomDaoSearchHistoryInsertAndObserve() = runTest {
    searchHistoryDao.insertSearchQuery(SearchHistoryEntity(query = "kotlin", timestamp = 100L))
    searchHistoryDao.insertSearchQuery(SearchHistoryEntity(query = "compose", timestamp = 200L))

    val history = searchHistoryDao.observeRecentSearchHistory().first()
    assertEquals(2, history.size)
    assertEquals("compose", history[0].query)
    assertEquals("kotlin", history[1].query)
  }

  @Test
  fun testRepositorySaveAndDeleteSearchQuery() = runTest {
    repository.saveSearchQuery("algorithm")
    repository.saveSearchQuery("android")

    var history = repository.searchHistory.first()
    assertEquals(2, history.size)

    repository.deleteSearchQuery("algorithm")
    history = repository.searchHistory.first()
    assertEquals(1, history.size)
    assertEquals("android", history[0].query)

    repository.clearSearchHistory()
    history = repository.searchHistory.first()
    assertTrue(history.isEmpty())
  }

  @Test
  fun testSearchHistoryUiInteraction() =
    runTest(testDispatcher) {
      viewModel.saveSearchQuery("sample_gist")
      viewModel.saveSearchQuery("reactive")
      testDispatcher.scheduler.advanceUntilIdle()
      composeTestRule.waitForIdle()

      // Check initially saved history
      val initialHistory = repository.searchHistory.first()
      assertEquals("Expected 2 items initially", 2, initialHistory.size)

      composeTestRule.setContent { MyApplicationTheme { GistHubAppScreen(viewModel = viewModel) } }
      testDispatcher.scheduler.advanceUntilIdle()
      composeTestRule.waitForIdle()

      // Verify search history chips are rendered
      composeTestRule.onNodeWithTag("search_history_container").assertExists()
      composeTestRule.onNodeWithTag("search_history_header").assertIsDisplayed()
      composeTestRule.onNodeWithTag("search_history_chip_sample_gist").assertIsDisplayed()
      composeTestRule.onNodeWithTag("search_history_chip_reactive").assertIsDisplayed()

      // Click on search history chip text to set search query without clicking close icon
      composeTestRule.onNodeWithTag("search_history_chip_reactive").performTouchInput {
        click(androidx.compose.ui.geometry.Offset(width * 0.25f, height / 2f))
      }
      testDispatcher.scheduler.advanceUntilIdle()
      composeTestRule.waitForIdle()

      // Verify search query was updated in ViewModel
      assertEquals("reactive", viewModel.searchQuery.value)

      // Delete individual item
      composeTestRule
        .onNodeWithTag("delete_search_history_item_sample_gist", useUnmergedTree = true)
        .performClick()
      testDispatcher.scheduler.advanceUntilIdle()
      composeTestRule.waitForIdle()

      // Verify repository/database content directly
      val dbHistory = repository.searchHistory.first()
      assertEquals(
        "Expected 1 item in database but found ${dbHistory.size}: ${dbHistory.map { it.query }}",
        1,
        dbHistory.size
      )
      assertEquals("reactive", dbHistory[0].query)

      // Clear all history
      composeTestRule.onNodeWithTag("clear_all_search_history_btn").performClick()
      testDispatcher.scheduler.advanceUntilIdle()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithTag("search_history_container").assertDoesNotExist()
    }
}
