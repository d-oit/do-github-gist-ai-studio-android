package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.security.GistContentEncryptor
import com.example.data.local.AppDatabase
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import com.example.ui.viewmodel.BackupExporter
import com.example.ui.viewmodel.BackupImporter
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BackupImportExportTest {

  @get:Rule val tempFolder = TemporaryFolder()

  private lateinit var context: Context
  private lateinit var db: AppDatabase
  private lateinit var repository: GistRepository
  private lateinit var fakeApi: FakeGitHubApiService
  private lateinit var configPrefs: ConfigPrefs
  private val testDispatcher = UnconfinedTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    db =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    fakeApi = FakeGitHubApiService()
    configPrefs = ConfigPrefs(context)
    val encryptor = GistContentEncryptor(context)

    repository =
      GistRepository(
        gistDao = db.gistDao(),
        apiService = fakeApi,
        configPrefs = configPrefs,
        contentEncryptor = encryptor
      )
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun exportBackup_writesValidJsonPayloadToFile() = runTest {
    val gist =
      GistEntity(
        id = "export-gist-1",
        description = "Test export description",
        isPublic = true,
        createdAt = "2026-09-29T12:00:00Z",
        updatedAt = "2026-09-29T12:00:00Z",
        ownerLogin = "testdev",
        ownerAvatarUrl = "https://avatar.test/img.png",
        htmlUrl = "https://gist.github.com/export-gist-1",
        url = "https://api.github.com/gists/export-gist-1",
        nodeId = "node_1",
        isPinned = true,
        isLocalOnly = true,
        isDirty = false,
        isDeleted = false,
        isStarred = false,
        isStarredDirty = false,
        tags = listOf("android", "kotlin"),
        ownerId = 42
      )
    val file =
      GistFileEntity(
        fileId = "file-1",
        gistId = "export-gist-1",
        filename = "Main.kt",
        type = "text/plain",
        language = "Kotlin",
        rawUrl = "",
        size = 25L,
        content = "println(\"Hello World\")"
      )

    val gistWithFiles = listOf(GistWithFiles(gist = gist, files = listOf(file)))
    val backupFile = File(tempFolder.root, "test_backup.json")
    val uri = Uri.fromFile(backupFile)

    var exportResultSuccess = false
    var exportResultMessage = ""

    val job =
      BackupExporter.exportBackup(
        scope = testScope,
        localGists = gistWithFiles,
        context = context,
        uri = uri,
        ioDispatcher = testDispatcher,
        mainDispatcher = testDispatcher
      ) { success, msg ->
        exportResultSuccess = success
        exportResultMessage = msg
      }
    job.join()

    assertTrue(exportResultSuccess)
    assertTrue(exportResultMessage.contains("successfully", ignoreCase = true))
    assertTrue(backupFile.exists())
    val content = backupFile.readText()
    assertTrue(content.contains("export-gist-1"))
    assertTrue(content.contains("Test export description"))
    assertTrue(content.contains("Main.kt"))
  }

  @Test
  fun importBackup_restoresGistsIntoRoomDatabase() = runTest {
    val backupJson =
      """
      {
        "backupVersion": 1,
        "exportedAt": "2026-09-29T12:00:00Z",
        "gists": [
          {
            "id": "imported-gist-1",
            "description": "Restored snippet",
            "htmlUrl": "https://gist.github.com/imported-gist-1",
            "url": "https://api.github.com/gists/imported-gist-1",
            "createdAt": "2026-09-29T10:00:00Z",
            "updatedAt": "2026-09-29T10:00:00Z",
            "isPublic": true,
            "isPinned": false,
            "isLocalOnly": true,
            "isDirty": false,
            "isDeleted": false,
            "isStarred": false,
            "isStarredDirty": false,
            "ownerLogin": "importer",
            "ownerId": 99,
            "ownerAvatarUrl": "",
            "files": [
              {
                "filename": "Script.py",
                "content": "print('Imported')",
                "type": "text/plain",
                "language": "Python",
                "size": 17
              }
            ]
          }
        ]
      }
      """
        .trimIndent()

    val backupFile = File(tempFolder.root, "import_test.json")
    backupFile.writeText(backupJson)
    val uri = Uri.fromFile(backupFile)

    var importSuccess = false
    var importedCount = 0

    val job =
      BackupImporter.importBackup(
        scope = testScope,
        repository = repository,
        context = context,
        uri = uri,
        ioDispatcher = testDispatcher,
        mainDispatcher = testDispatcher
      ) { success, _, count ->
        importSuccess = success
        importedCount = count
      }
    job.join()

    assertTrue(importSuccess)
    assertEquals(1, importedCount)

    val storedGist = repository.getGist("imported-gist-1")
    assertNotNull(storedGist)
    assertEquals("Restored snippet", storedGist!!.gist.description)
    assertEquals(1, storedGist.files.size)
    assertEquals("Script.py", storedGist.files[0].filename)
    assertEquals("print('Imported')", storedGist.files[0].content)
  }

  @Test
  fun importBackup_failsGracefullyOnMalformedJson() = runTest {
    val malformedFile = File(tempFolder.root, "invalid.json")
    malformedFile.writeText("This is NOT JSON")
    val uri = Uri.fromFile(malformedFile)

    var importSuccess = true
    var errorMessage = ""

    val job =
      BackupImporter.importBackup(
        scope = testScope,
        repository = repository,
        context = context,
        uri = uri,
        ioDispatcher = testDispatcher,
        mainDispatcher = testDispatcher
      ) { success, msg, _ ->
        importSuccess = success
        errorMessage = msg
      }
    job.join()

    assertFalse(importSuccess)
    assertTrue(errorMessage.isNotEmpty())
  }

  @Test
  fun roundTrip_exportAndImport_preservesGistsAndContent() = runTest {
    repository.createLocalDraft(
      description = "Roundtrip description",
      files = listOf("Test.kt" to "fun main() = println(42)"),
      isPublic = false,
      isPinned = true
    )

    val initialGists = repository.allGists.first()
    assertEquals(1, initialGists.size)

    val backupFile = File(tempFolder.root, "roundtrip.json")
    val uri = Uri.fromFile(backupFile)

    val exportJob =
      BackupExporter.exportBackup(
        scope = testScope,
        localGists = initialGists,
        context = context,
        uri = uri,
        ioDispatcher = testDispatcher,
        mainDispatcher = testDispatcher
      ) { success, _ ->
        assertTrue(success)
      }
    exportJob.join()

    // Clear local database
    repository.clearAllLocalData()
    val clearedGists = repository.allGists.first()
    assertTrue(clearedGists.isEmpty())

    // Restore from backup file
    val importJob =
      BackupImporter.importBackup(
        scope = testScope,
        repository = repository,
        context = context,
        uri = uri,
        ioDispatcher = testDispatcher,
        mainDispatcher = testDispatcher
      ) { success, _, count ->
        assertTrue(success)
        assertEquals(1, count)
      }
    importJob.join()

    val restoredGists = repository.allGists.first()
    assertEquals(1, restoredGists.size)
    assertEquals("Roundtrip description", restoredGists[0].gist.description)
    assertEquals("Test.kt", restoredGists[0].files[0].filename)
    assertEquals("fun main() = println(42)", restoredGists[0].files[0].content)
  }
}
