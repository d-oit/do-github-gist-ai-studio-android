package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles
import com.example.ui.theme.ActivePurple

/**
 * Individual Card component representing a single local Gist, displaying its title, code snippet
 * preview, and sync status icon badge.
 */
@Composable
fun LocalGistItemCard(
  item: GistWithFiles,
  onTogglePin: () -> Unit,
  onToggleStar: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onPreview: () -> Unit,
  modifier: Modifier = Modifier
) {
  val titleText =
    remember(item) {
      item.gist.description?.takeIf { it.isNotBlank() }
        ?: item.files.firstOrNull()?.filename
        ?: "Untitled Gist"
    }

  val primaryFile = remember(item) { item.files.firstOrNull() }
  val snippetText =
    remember(item) {
      val rawContent = primaryFile?.content ?: ""
      if (rawContent.isBlank()) "Empty file"
      else rawContent.lines().take(4).joinToString("\n").take(250)
    }

  Card(
    onClick = onPreview,
    modifier = modifier.fillMaxWidth().testTag("local_gist_card_${item.gist.id}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border =
      androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
      ),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      // Header: Title, Visibility, Sync Status Icon Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
          ) {
            Text(
              text = titleText,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("local_gist_title_${item.gist.id}")
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = if (item.gist.isPublic) Icons.Default.Public else Icons.Default.Lock,
              contentDescription = if (item.gist.isPublic) "Public" else "Secret",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp)
            )
          }

          if (primaryFile != null && !item.gist.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = primaryFile.filename,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Medium
            )
          }
        }

        // Sync Status Icon Badge
        Box(modifier = Modifier.testTag("local_gist_sync_icon_${item.gist.id}")) {
          GistSyncStateIndicator(
            isLocalOnly = item.gist.isLocalOnly,
            isDirty = item.gist.isDirty,
            isDeleted = item.gist.isDeleted,
            compact = true
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Snippet Code Box
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
              1.dp,
              MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
              RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
            .testTag("local_gist_snippet_${item.gist.id}")
      ) {
        Text(
          text = snippetText,
          fontSize = 12.sp,
          fontFamily = FontFamily.Monospace,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 16.sp
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Bottom Row: Tag Badges & Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (item.files.size > 1) {
            Box(
              modifier =
                Modifier.background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    RoundedCornerShape(6.dp)
                  )
                  .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "${item.files.size} files",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
            }
          }

          item.gist.tags.take(2).forEach { tag ->
            Box(
              modifier =
                Modifier.background(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    RoundedCornerShape(6.dp)
                  )
                  .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "#$tag",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          IconButton(
            onClick = onToggleStar,
            modifier = Modifier.size(32.dp).testTag("local_star_button_${item.gist.id}")
          ) {
            Icon(
              imageVector =
                if (item.gist.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
              contentDescription = "Star",
              tint =
                if (item.gist.isStarred) Color(0xFFFFA000)
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = onTogglePin,
            modifier = Modifier.size(32.dp).testTag("local_pin_button_${item.gist.id}")
          ) {
            Icon(
              imageVector =
                if (item.gist.isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
              contentDescription = "Pin",
              tint =
                if (item.gist.isPinned) ActivePurple
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = onEdit,
            modifier = Modifier.size(32.dp).testTag("local_edit_button_${item.gist.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit Gist",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp).testTag("local_delete_button_${item.gist.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete Gist",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}
