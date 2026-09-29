package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles

/**
 * A reusable Compose-based list view to display fetched Gists.
 *
 * Each item displays rich metadata and visual sync status indicators (synced, pending, or error)
 * using icons and accessible Material 3 design tokens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GistListView(
  gists: List<GistWithFiles>,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
  errorGistIds: Set<String> = emptySet(),
  onGistClick: (GistWithFiles) -> Unit = {},
  onTogglePin: (String) -> Unit = {},
  onToggleStar: (String) -> Unit = {},
  onEdit: (GistWithFiles) -> Unit = {},
  onDelete: (String) -> Unit = {},
  onSyncIndicatorClick: ((GistWithFiles) -> Unit)? = null,
  enableSwipeToDismiss: Boolean = true,
  contentPadding: PaddingValues = PaddingValues(bottom = 80.dp),
  isRefreshing: Boolean = false,
  onRefresh: () -> Unit = {},
  onCreateDraftClick: (() -> Unit)? = null,
  emptyContent: (@Composable () -> Unit)? = null
) {
  if (gists.isEmpty()) {
    if (emptyContent != null) {
      emptyContent()
    } else {
      Box(
        modifier = modifier.fillMaxSize().testTag("gist_list_empty_container"),
        contentAlignment = Alignment.Center
      ) {
        GistListEmptyState(
          onFetchClick = onRefresh,
          isRefreshing = isRefreshing,
          onCreateDraftClick = onCreateDraftClick
        )
      }
    }
  } else {
    LazyColumn(
      state = listState,
      modifier = modifier.fillMaxWidth().testTag("gist_list_view"),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = contentPadding
    ) {
      items(gists, key = { it.gist.id }) { item ->
        val hasError = errorGistIds.contains(item.gist.id)

        if (enableSwipeToDismiss) {
          val dismissState =
            rememberSwipeToDismissBoxState(
              confirmValueChange = { dismissValue ->
                if (
                  dismissValue == SwipeToDismissBoxValue.EndToStart ||
                    dismissValue == SwipeToDismissBoxValue.StartToEnd
                ) {
                  onDelete(item.gist.id)
                  false
                } else {
                  false
                }
              }
            )

          SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.fillMaxWidth().testTag("swipe_to_dismiss_${item.gist.id}"),
            backgroundContent = {
              val isDismissed = dismissState.targetValue != SwipeToDismissBoxValue.Settled
              val backgroundColor =
                if (isDismissed) MaterialTheme.colorScheme.errorContainer else Color.Transparent

              Box(
                modifier =
                  Modifier.fillMaxSize()
                    .background(backgroundColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment =
                  if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                  } else {
                    Alignment.CenterEnd
                  }
              ) {
                if (isDismissed) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                      Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Swipe to delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                      )
                      Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                      )
                    } else {
                      Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                      )
                      Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Swipe to delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                      )
                    }
                  }
                }
              }
            }
          ) {
            GistCard(
              item = item,
              onTogglePin = { onTogglePin(item.gist.id) },
              onToggleStar = { onToggleStar(item.gist.id) },
              onEdit = { onEdit(item) },
              onDelete = { onDelete(item.gist.id) },
              onPreview = { onGistClick(item) },
              hasSyncError = hasError,
              onSyncIndicatorClick =
                if (onSyncIndicatorClick != null) {
                  { onSyncIndicatorClick(item) }
                } else {
                  null
                }
            )
          }
        } else {
          GistCard(
            item = item,
            onTogglePin = { onTogglePin(item.gist.id) },
            onToggleStar = { onToggleStar(item.gist.id) },
            onEdit = { onEdit(item) },
            onDelete = { onDelete(item.gist.id) },
            onPreview = { onGistClick(item) },
            hasSyncError = hasError,
            onSyncIndicatorClick =
              if (onSyncIndicatorClick != null) {
                { onSyncIndicatorClick(item) }
              } else {
                null
              }
          )
        }
      }
    }
  }
}
