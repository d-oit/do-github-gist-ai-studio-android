package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles
import com.example.ui.components.GistListEmptyState
import com.example.ui.components.GistSyncState
import com.example.ui.components.LocalGistItemCard
import com.example.ui.components.resolveGistSyncState

enum class SyncFilterCategory {
  ALL,
  LOCAL_ONLY,
  UNPUSHED,
  SYNCED
}

/**
 * Screen that lists all local gists stored in Room database, showing their title, snippet preview,
 * and sync status icon indicator.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun LocalGistsScreen(
  gists: List<GistWithFiles>,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  onTogglePin: (String) -> Unit,
  onToggleStar: (String) -> Unit,
  onEdit: (GistWithFiles) -> Unit,
  onDelete: (String) -> Unit,
  onPreview: (GistWithFiles) -> Unit,
  onCreateDraftClick: (() -> Unit)? = null,
  isRefreshing: Boolean = false,
  onRefresh: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var selectedFilter by remember { mutableStateOf(SyncFilterCategory.ALL) }
  var selectedTag by remember { mutableStateOf<String?>(null) }

  val pullRefreshState =
    if (onRefresh != null) {
      rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)
    } else null

  val allAvailableTags =
    remember(gists) {
      gists.flatMap { it.gist.tags }.filter { it.isNotBlank() }.distinct().sorted()
    }

  val filteredGists =
    remember(gists, searchQuery, selectedFilter, selectedTag) {
      gists
        .filter { item ->
          val syncState =
            resolveGistSyncState(item.gist.isLocalOnly, item.gist.isDirty, item.gist.isDeleted)
          when (selectedFilter) {
            SyncFilterCategory.ALL -> true
            SyncFilterCategory.LOCAL_ONLY -> syncState == GistSyncState.LOCAL_ONLY
            SyncFilterCategory.UNPUSHED ->
              syncState == GistSyncState.DIRTY || syncState == GistSyncState.LOCAL_ONLY
            SyncFilterCategory.SYNCED -> syncState == GistSyncState.SYNCED
          }
        }
        .filter { item -> if (selectedTag == null) true else item.gist.tags.contains(selectedTag) }
        .filter { item ->
          if (searchQuery.isBlank()) true
          else {
            val title = item.gist.description ?: ""
            val matchesTitle = title.contains(searchQuery, ignoreCase = true)
            val matchesFiles =
              item.files.any { file ->
                file.filename.contains(searchQuery, ignoreCase = true) ||
                  file.content.contains(searchQuery, ignoreCase = true)
              }
            val matchesTags =
              item.gist.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
            matchesTitle || matchesFiles || matchesTags
          }
        }
        .sortedWith(
          compareByDescending<GistWithFiles> { it.gist.isPinned }
            .thenByDescending { it.gist.updatedAt }
        )
    }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .then(if (pullRefreshState != null) Modifier.pullRefresh(pullRefreshState) else Modifier)
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).testTag("local_gists_screen")
    ) {
      // Top Bar Header & Count Badge
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Storage,
            contentDescription = "Local Storage",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Local Gists Repository",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onCreateDraftClick != null) {
            IconButton(
              onClick = onCreateDraftClick,
              modifier = Modifier.testTag("create_local_draft_button")
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Draft",
                tint = MaterialTheme.colorScheme.primary
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
          }

          Box(
            modifier =
              Modifier.background(
                  MaterialTheme.colorScheme.primaryContainer,
                  RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("local_gists_count_badge")
          ) {
            Text(
              text = "${filteredGists.size} Local",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }

      // Search input field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search local gists by title or content...", fontSize = 14.sp) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { onSearchQueryChange("") },
              modifier = Modifier.testTag("clear_local_search_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        modifier =
          Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("local_gists_search_field"),
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
          )
      )

      // Sync state filter chips
      LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        item {
          FilterChip(
            selected = selectedFilter == SyncFilterCategory.ALL,
            onClick = { selectedFilter = SyncFilterCategory.ALL },
            label = { Text("All (${gists.size})") },
            colors = FilterChipDefaults.filterChipColors(),
            modifier = Modifier.testTag("filter_chip_all")
          )
        }
        item {
          FilterChip(
            selected = selectedFilter == SyncFilterCategory.LOCAL_ONLY,
            onClick = { selectedFilter = SyncFilterCategory.LOCAL_ONLY },
            label = { Text("Local Only") },
            colors = FilterChipDefaults.filterChipColors(),
            modifier = Modifier.testTag("filter_chip_local_only")
          )
        }
        item {
          FilterChip(
            selected = selectedFilter == SyncFilterCategory.UNPUSHED,
            onClick = { selectedFilter = SyncFilterCategory.UNPUSHED },
            label = { Text("Unpushed Edits") },
            colors = FilterChipDefaults.filterChipColors(),
            modifier = Modifier.testTag("filter_chip_unpushed")
          )
        }
        item {
          FilterChip(
            selected = selectedFilter == SyncFilterCategory.SYNCED,
            onClick = { selectedFilter = SyncFilterCategory.SYNCED },
            label = { Text("Synced") },
            colors = FilterChipDefaults.filterChipColors(),
            modifier = Modifier.testTag("filter_chip_synced")
          )
        }
      }

      if (allAvailableTags.isNotEmpty()) {
        LazyRow(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("tag_filter_row"),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          item {
            FilterChip(
              selected = selectedTag == null,
              onClick = { selectedTag = null },
              label = { Text("All Tags") },
              modifier = Modifier.testTag("tag_filter_chip_all")
            )
          }
          items(allAvailableTags) { tag ->
            FilterChip(
              selected = selectedTag == tag,
              onClick = { selectedTag = if (selectedTag == tag) null else tag },
              label = { Text("#$tag") },
              modifier = Modifier.testTag("tag_filter_chip_$tag")
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // List of local gists
      if (filteredGists.isEmpty()) {
        if (gists.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            GistListEmptyState(
              onFetchClick = { onRefresh?.invoke() },
              isRefreshing = isRefreshing,
              onCreateDraftClick = onCreateDraftClick
            )
          }
        } else {
          Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.Code,
                contentDescription = "No Local Gists",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "No matching local gists",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "No gists match your search query or sync filters.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
              )
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize().testTag("local_gists_list"),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(filteredGists, key = { it.gist.id }) { item ->
            LocalGistItemCard(
              item = item,
              onTogglePin = { onTogglePin(item.gist.id) },
              onToggleStar = { onToggleStar(item.gist.id) },
              onEdit = { onEdit(item) },
              onDelete = { onDelete(item.gist.id) },
              onPreview = { onPreview(item) }
            )
          }
          item { Spacer(modifier = Modifier.height(80.dp)) }
        }
      }
    }

    if (pullRefreshState != null) {
      PullRefreshIndicator(
        refreshing = isRefreshing,
        state = pullRefreshState,
        modifier =
          Modifier.align(Alignment.TopCenter).testTag("local_gists_pull_refresh_indicator"),
        backgroundColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
      )
    }
  }
}
