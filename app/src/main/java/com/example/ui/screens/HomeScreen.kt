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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles
import com.example.ui.components.GistCard
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.ActivePurpleContainer
import com.example.ui.theme.DarkPurpleText

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
fun HomeScreen(
  gists: List<GistWithFiles>,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  selectedTag: String? = null,
  allTags: List<String> = emptyList(),
  onSelectedTagChange: (String?) -> Unit = {},
  isRefreshing: Boolean,
  onRefresh: () -> Unit,
  onTogglePin: (String) -> Unit,
  onToggleStar: (String) -> Unit,
  onEdit: (GistWithFiles) -> Unit,
  onDelete: (String) -> Unit,
  onPreview: (GistWithFiles) -> Unit,
  lastSyncTime: Long = 0L
) {
  val filtered =
    remember(gists, searchQuery, selectedTag) {
      if (searchQuery.isBlank()) {
        gists.sortedWith(
          compareByDescending<GistWithFiles> { it.gist.isPinned }
            .thenByDescending { it.gist.createdAt }
        )
      } else {
        gists
          .filter { item ->
            val desc = item.gist.description ?: ""
            val matchesDescription = desc.contains(searchQuery, ignoreCase = true)
            val matchesFiles =
              item.files.any { file ->
                file.filename.contains(searchQuery, ignoreCase = true) ||
                  file.content.contains(searchQuery, ignoreCase = true)
              }
            matchesDescription || matchesFiles
          }
          .sortedWith(
            compareByDescending<GistWithFiles> { it.gist.isPinned }
              .thenByDescending { it.gist.createdAt }
          )
      }
    }

  val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)

  Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
      // Persistent Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search gists by filename or description...", fontSize = 14.sp) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search icon",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { onSearchQueryChange("") },
              modifier = Modifier.testTag("clear_search_button")
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
          Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp).testTag("home_search_bar"),
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

      // Horizontal Scrollable Tag Filters Row
      if (allTags.isNotEmpty()) {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          item {
            FilterChip(
              selected = selectedTag == null,
              onClick = { onSelectedTagChange(null) },
              label = { Text("All", fontSize = 12.sp) },
              modifier = Modifier.testTag("tag_filter_all")
            )
          }
          items(allTags) { tag ->
            FilterChip(
              selected = selectedTag == tag,
              onClick = {
                if (selectedTag == tag) {
                  onSelectedTagChange(null)
                } else {
                  onSelectedTagChange(tag)
                }
              },
              label = { Text("#$tag", fontSize = 12.sp) },
              modifier = Modifier.testTag("tag_filter_$tag")
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Local Cache & Sync Status",
            fontSize = 12.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = ActivePurple,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Last Synced: ${formatLastSynced(lastSyncTime)}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("last_synced_timestamp")
          )
        }
        Box(
          modifier =
            Modifier.background(ActivePurpleContainer, RoundedCornerShape(10.dp))
              .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text(
            text = "${filtered.size} Items",
            fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = DarkPurpleText
          )
        }
      }

      if (filtered.isEmpty()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          if (gists.isEmpty()) {
            // PRIMARY EMPTY STATE: No local gists at all in the database
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier.fillMaxWidth().padding(24.dp)
            ) {
              // Friendly, high-fidelity decorative illustration/avatar container
              Box(
                modifier =
                  Modifier.size(96.dp)
                    .background(
                      color = ActivePurpleContainer.copy(alpha = 0.3f),
                      shape = RoundedCornerShape(48.dp)
                    ),
                contentAlignment = Alignment.Center
              ) {
                Box(
                  modifier =
                    Modifier.size(72.dp)
                      .background(
                        color = ActivePurpleContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(36.dp)
                      ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "Cloud Synchronize",
                    tint = ActivePurple,
                    modifier = Modifier.size(36.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.height(24.dp))
              Text(
                text = "Your Gist Library is Empty",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text =
                  "Securely synchronize your GitHub snippets to work offline, or start drafting local code blocks immediately. Your drafts persist locally and can be synced anytime.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
              )
              Spacer(modifier = Modifier.height(28.dp))

              // Primary Call-To-Action Button: Fetch from GitHub
              Button(
                onClick = onRefresh,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = ActivePurple,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                  ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("fetch_from_github_btn")
              ) {
                if (isRefreshing) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text("Fetching Gists...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                  Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "Download icon",
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Fetch from GitHub", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
              }
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Or tap the '+' button in the bottom right to create a new draft offline.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
              )
            }
          } else {
            // SECONDARY EMPTY STATE: Gists exist but none match search filters
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier.fillMaxWidth().padding(24.dp)
            ) {
              Box(
                modifier =
                  Modifier.size(72.dp)
                    .background(
                      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                      shape = RoundedCornerShape(36.dp)
                    ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = "No matches",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(28.dp)
                )
              }
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "No Matching Gists",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text =
                  "We couldn't find any local gists matching \"$searchQuery\"${if (selectedTag != null) " with tag #$selectedTag" else ""}. Try verifying the spelling or reset your filters.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
              )
              Spacer(modifier = Modifier.height(24.dp))

              // Reset filters CTA
              Button(
                onClick = {
                  onSearchQueryChange("")
                  onSelectedTagChange(null)
                },
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                  ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(40.dp).testTag("reset_filters_btn")
              ) {
                Text("Reset Search & Filters", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(filtered, key = { it.gist.id }) { item ->
            GistCard(
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

    PullRefreshIndicator(
      refreshing = isRefreshing,
      state = pullRefreshState,
      modifier = Modifier.align(Alignment.TopCenter).testTag("pull_refresh_indicator"),
      backgroundColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.primary
    )
  }
}

private fun formatLastSynced(timestamp: Long): String {
  if (timestamp <= 0L) return "Never"
  return try {
    val date = java.util.Date(timestamp)
    val formatter =
      java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
    formatter.format(date)
  } catch (e: Exception) {
    "Never"
  }
}
