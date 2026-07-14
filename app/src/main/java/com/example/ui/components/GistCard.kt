package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.ActivePurpleContainer
import com.example.ui.theme.DarkPurpleText
import com.example.ui.theme.DarkRedText
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.LightPinkContainer

@Composable
fun borderButtonStroke() =
  androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GistCard(
  item: GistWithFiles,
  onTogglePin: () -> Unit,
  onToggleStar: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onPreview: () -> Unit
) {
  Card(
    onClick = onEdit,
    modifier = Modifier.fillMaxWidth().testTag("gist_card_${item.gist.id}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = borderButtonStroke(),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = item.files.firstOrNull()?.filename ?: "untitled.kt",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = if (item.gist.isPublic) Icons.Default.Public else Icons.Default.Lock,
              contentDescription = "Visibility",
              tint = GraySecondary,
              modifier = Modifier.size(14.dp)
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = item.gist.description ?: "No description provided",
            fontSize = 13.sp,
            color = GraySecondary
          )
          if (item.gist.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              item.gist.tags.forEach { tag ->
                Box(
                  modifier =
                    Modifier.background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(6.dp)
                      )
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "#$tag",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                  )
                }
              }
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          IconButton(
            onClick = onToggleStar,
            modifier = Modifier.testTag("star_button_${item.gist.id}")
          ) {
            Icon(
              imageVector =
                if (item.gist.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
              contentDescription = "Star",
              tint = if (item.gist.isStarred) Color(0xFFFFA000) else GraySecondary,
              modifier = Modifier.size(20.dp)
            )
          }
          IconButton(
            onClick = onTogglePin,
            modifier = Modifier.testTag("pin_button_${item.gist.id}")
          ) {
            Icon(
              imageVector =
                if (item.gist.isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
              contentDescription = "Pin",
              tint = if (item.gist.isPinned) ActivePurple else GraySecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Badges
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (item.gist.isPinned) {
            Box(
              modifier =
                Modifier.background(ActivePurpleContainer, RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ActivePurple))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Pinned",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = DarkPurpleText
                )
              }
            }
          }

          if (item.gist.isStarred) {
            Box(
              modifier =
                Modifier.background(Color(0xFFFFF8E1), RoundedCornerShape(8.dp))
                  .border(1.dp, Color(0xFFFFE082), RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = "Starred Badge",
                  tint = Color(0xFFFFA000),
                  modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (item.gist.isStarredDirty) "Star Pending" else "Starred",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFE65100)
                )
              }
            }
          }

          if (item.gist.isLocalOnly) {
            Box(
              modifier =
                Modifier.background(LightPinkContainer, RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "Local Only",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DarkRedText
              )
            }
          } else if (item.gist.isDirty) {
            Box(
              modifier =
                Modifier.border(1.dp, ErrorRed, RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Dirty",
                  tint = ErrorRed,
                  modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Dirty",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = ErrorRed
                )
              }
            }
          }
        }

        // Action buttons
        Row {
          IconButton(onClick = onPreview) {
            Icon(
              imageVector = Icons.Default.Visibility,
              contentDescription = "Preview Code",
              tint = GraySecondary,
              modifier = Modifier.size(18.dp)
            )
          }
          IconButton(onClick = onEdit) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit",
              tint = GraySecondary,
              modifier = Modifier.size(18.dp)
            )
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("delete_button_${item.gist.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete",
              tint = ErrorRed,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun TabButton(
  label: String,
  icon: ImageVector,
  isActive: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier =
      modifier
        .width(72.dp)
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier =
        Modifier.width(48.dp)
          .height(32.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(
            if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
          ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint =
          if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
          else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp)
      )
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
      color =
        if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
