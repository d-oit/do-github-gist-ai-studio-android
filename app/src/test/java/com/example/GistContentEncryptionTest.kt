package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.security.GistContentEncryptor
import com.example.data.local.AppDatabase
import com.example.data.local.dao.GistDao
import com.example.data.local.pref.ConfigPrefs
import com.example.data.repository.GistRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GistContentEncryptionTest {

  private lateinit var context: Context
  private lateinit var database: AppDatabase
  private lateinit var gistDao: GistDao
  private lateinit var configPrefs: ConfigPrefs
  private lateinit var encryptor: GistContentEncryptor
  private lateinit var repository: GistRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    database =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    gistDao = database.gistDao()
    configPrefs = ConfigPrefs(context)
    encryptor = GistContentEncryptor(context)
    repository =
      GistRepository(
        gistDao = gistDao,
        apiService = FakeGitHubApiService(),
        configPrefs = configPrefs,
        contentEncryptor = encryptor
      )
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun testGistContentEncryptorEncryptAndDecrypt() {
    val originalText = "val secretApiKey = \"sk_test_1234567890\""

    val encrypted = encryptor.encrypt(originalText)
    assertTrue("Encrypted text should start with GIST_ENC_v1:", encryptor.isEncrypted(encrypted))
    assertNotEquals("Encrypted text should not equal original", originalText, encrypted)

    val decrypted = encryptor.decrypt(encrypted)
    assertEquals("Decrypted text should match original", originalText, decrypted)
  }

  @Test
  fun testDecryptUnencryptedTextPassesThrough() {
    val plainText = "fun main() { println(\"Hello\") }"
    val result = encryptor.decrypt(plainText)
    assertEquals("Unencrypted text should pass through unchanged", plainText, result)
  }

  @Test
  fun testEncryptAlreadyEncryptedTextDoesNotReEncrypt() {
    val plainText = "sensitive content"
    val encryptedOnce = encryptor.encrypt(plainText)
    val encryptedTwice = encryptor.encrypt(encryptedOnce)
    assertEquals(
      "Encrypting already encrypted text should be idempotent",
      encryptedOnce,
      encryptedTwice
    )
  }

  @Test
  fun testRepositoryEncryptsInRoomDatabaseAndDecryptsInRepository() = runBlocking {
    val secretContent = "private const val DB_PASSWORD = \"super_secret_p@ss\""
    val draftId =
      repository.createLocalDraft(
        description = "Secret Config Gist",
        files = listOf("config.kt" to secretContent),
        isPublic = false,
        isPinned = true
      )

    // Verify raw Room database record is encrypted
    val rawRoomData = gistDao.getGistById(draftId)
    assertTrue("Room database record must exist", rawRoomData != null)
    val rawFileContent = rawRoomData!!.files.first().content
    assertTrue(
      "Raw content in Room database must be encrypted",
      rawFileContent.startsWith("GIST_ENC_v1:")
    )
    assertNotEquals(
      "Raw content in Room database must not be plain text",
      secretContent,
      rawFileContent
    )

    // Verify repository accessor decrypts content seamlessly
    val retrievedFromRepo = repository.getGist(draftId)
    assertTrue("Retrieved gist from repository must exist", retrievedFromRepo != null)
    val repoFileContent = retrievedFromRepo!!.files.first().content
    assertEquals("Repository should return decrypted content", secretContent, repoFileContent)

    // Verify Flow observe returns decrypted content as well
    val observedGist = repository.observeGist(draftId).first()
    assertEquals(
      "Observed flow should return decrypted content",
      secretContent,
      observedGist?.files?.first()?.content
    )
  }
}
