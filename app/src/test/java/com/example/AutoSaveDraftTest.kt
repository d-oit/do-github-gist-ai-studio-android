package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.pref.AutoSavedDraft
import com.example.data.local.pref.ConfigPrefs
import com.example.data.local.pref.DraftFile
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AutoSaveDraftTest {

  @Test
  fun `test auto-save and retrieval of new gist draft`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val configPrefs = ConfigPrefs(context)

    // Clear any prior state
    configPrefs.clearAutoSavedDraft(null)

    val draft =
      AutoSavedDraft(
        editingGistId = null,
        description = "My test draft description",
        files = listOf(DraftFile("test.txt", "Hello World!"), DraftFile("another.md", "# Heading")),
        isPublic = false,
        isPinned = true,
        tags = listOf("test", "autosave"),
        timestamp = System.currentTimeMillis()
      )

    configPrefs.saveAutoSavedDraft(draft)

    val retrieved = configPrefs.getAutoSavedDraft(null)
    assertNotNull(retrieved)
    assertEquals(draft.editingGistId, retrieved?.editingGistId)
    assertEquals(draft.description, retrieved?.description)
    assertEquals(draft.files.size, retrieved?.files?.size)
    assertEquals(draft.files[0].filename, retrieved?.files?.get(0)?.filename)
    assertEquals(draft.files[0].content, retrieved?.files?.get(0)?.content)
    assertEquals(draft.isPublic, retrieved?.isPublic)
    assertEquals(draft.isPinned, retrieved?.isPinned)
    assertEquals(draft.tags, retrieved?.tags)

    // Clear and verify
    configPrefs.clearAutoSavedDraft(null)
    assertNull(configPrefs.getAutoSavedDraft(null))
  }

  @Test
  fun `test auto-save and retrieval of editing gist draft`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val configPrefs = ConfigPrefs(context)

    val id = "gist_12345"
    configPrefs.clearAutoSavedDraft(id)

    val draft =
      AutoSavedDraft(
        editingGistId = id,
        description = "Editing some existing gist",
        files = listOf(DraftFile("index.js", "console.log('hi');")),
        isPublic = true,
        isPinned = false,
        tags = listOf("js", "existing"),
        timestamp = System.currentTimeMillis()
      )

    configPrefs.saveAutoSavedDraft(draft)

    val retrieved = configPrefs.getAutoSavedDraft(id)
    assertNotNull(retrieved)
    assertEquals(id, retrieved?.editingGistId)
    assertEquals(draft.description, retrieved?.description)
    assertEquals(draft.files[0].filename, retrieved?.files?.get(0)?.filename)
    assertEquals(draft.files[0].content, retrieved?.files?.get(0)?.content)

    // Clear and verify
    configPrefs.clearAutoSavedDraft(id)
    assertNull(configPrefs.getAutoSavedDraft(id))
  }
}
