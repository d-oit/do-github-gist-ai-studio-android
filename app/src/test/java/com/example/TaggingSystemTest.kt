package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.local.converters.RoomConverters
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity
import com.example.data.local.entity.GistWithFiles
import com.example.ui.screens.LocalGistsScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TaggingSystemTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun roomConverters_correctlySerializesAndDeserializesTags() {
    val converters = RoomConverters()
    val tags = listOf("kotlin", "compose", "room", "security")

    val jsonString = converters.fromStringList(tags)
    assertNotNull("JSON string should not be null", jsonString)

    val restoredTags = converters.toStringList(jsonString)
    assertNotNull("Restored tags should not be null", restoredTags)
    assertEquals(tags, restoredTags)
  }

  @Test
  fun gistEntity_supportsCustomTagsList() {
    val gist =
      GistEntity(
        id = "tag_test_1",
        description = "Gist with custom labels",
        htmlUrl = "",
        url = "",
        createdAt = "2026-08-01T00:00:00Z",
        updatedAt = "2026-08-01T00:00:00Z",
        nodeId = "",
        isPublic = true,
        isPinned = false,
        isLocalOnly = true,
        isDirty = false,
        tags = listOf("android", "ui", "compose"),
        ownerLogin = "developer",
        ownerId = 1,
        ownerAvatarUrl = ""
      )

    assertEquals(3, gist.tags.size)
    assertEquals(listOf("android", "ui", "compose"), gist.tags)
  }

  @Test
  fun localGistsScreen_rendersTagFilterChips_andFiltersByTag() {
    val gist1 =
      GistWithFiles(
        gist =
          GistEntity(
            id = "gist_1",
            description = "Kotlin Security Snippet",
            htmlUrl = "",
            url = "",
            createdAt = "2026-08-01T00:00:00Z",
            updatedAt = "2026-08-01T00:00:00Z",
            nodeId = "",
            isPublic = true,
            isPinned = false,
            isLocalOnly = true,
            isDirty = false,
            tags = listOf("kotlin", "security"),
            ownerLogin = "dev",
            ownerId = 1,
            ownerAvatarUrl = ""
          ),
        files =
          listOf(
            GistFileEntity(
              fileId = "f1",
              gistId = "gist_1",
              filename = "Security.kt",
              type = "text/plain",
              language = "Kotlin",
              rawUrl = "",
              size = 10,
              content = "val x = 1"
            )
          )
      )

    val gist2 =
      GistWithFiles(
        gist =
          GistEntity(
            id = "gist_2",
            description = "UI Design Component",
            htmlUrl = "",
            url = "",
            createdAt = "2026-08-01T00:00:00Z",
            updatedAt = "2026-08-01T00:00:00Z",
            nodeId = "",
            isPublic = true,
            isPinned = false,
            isLocalOnly = true,
            isDirty = false,
            tags = listOf("ui", "compose"),
            ownerLogin = "dev",
            ownerId = 1,
            ownerAvatarUrl = ""
          ),
        files =
          listOf(
            GistFileEntity(
              fileId = "f2",
              gistId = "gist_2",
              filename = "Component.kt",
              type = "text/plain",
              language = "Kotlin",
              rawUrl = "",
              size = 10,
              content = "fun UI() {}"
            )
          )
      )

    composeTestRule.setContent {
      LocalGistsScreen(
        gists = listOf(gist1, gist2),
        searchQuery = "",
        onSearchQueryChange = {},
        onTogglePin = {},
        onToggleStar = {},
        onEdit = {},
        onDelete = {},
        onPreview = {}
      )
    }

    composeTestRule.onNodeWithTag("tag_filter_row").assertExists()
    composeTestRule.onNodeWithTag("tag_filter_chip_all").assertExists()
    composeTestRule.onNodeWithTag("tag_filter_chip_kotlin").assertExists()
    composeTestRule.onNodeWithTag("tag_filter_chip_security").assertExists()
    composeTestRule.onNodeWithTag("tag_filter_chip_ui").assertExists()
    composeTestRule.onNodeWithTag("tag_filter_chip_compose").assertExists()

    composeTestRule.onNodeWithTag("tag_filter_chip_kotlin").performClick()
  }
}
