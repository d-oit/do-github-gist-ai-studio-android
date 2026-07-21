package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.model.GistResponse
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.ActivePurpleContainer
import com.example.ui.theme.DarkPurpleText

@Composable
fun GitHubGistApiList(
  gists: List<GistResponse>,
  isFetching: Boolean,
  error: String?,
  onRefresh: () -> Unit,
  isForking: String?,
  onForkClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth().testTag("github_gist_api_list_container"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Live GitHub API Explorer",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Direct real-time query using stored GITHUB_PAT",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(
          onClick = onRefresh,
          enabled = !isFetching,
          modifier = Modifier.testTag("remote_gist_refresh_button").size(48.dp)
        ) {
          if (isFetching) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = ActivePurple
            )
          } else {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh remote list",
              tint = ActivePurple
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Error state
      if (error != null) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
          Text(
            text = "Error fetching live list: $error",
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 12.sp,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      // List Content
      if (gists.isEmpty()) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(160.dp)
              .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
              ),
          contentAlignment = Alignment.Center
        ) {
          if (isFetching) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = ActivePurple)
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Contacting GitHub...",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(16.dp)
            ) {
              Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = "No Remote Gists",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "No live Gists loaded.",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Click refresh icon to retrieve directly via your token.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
              )
            }
          }
        }
      } else {
        // We use a Column inside a scrollable card, or limit height to present nicely within a
        // screen container.
        // Let's make a beautiful responsive list layout of the top Gists
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          gists.take(10).forEach { gist ->
            val filename = gist.files?.keys?.firstOrNull() ?: "untitled"
            val description = gist.description ?: "No description provided"
            val isPublic = gist.isPublic ?: true
            val totalFiles = gist.files?.size ?: 0

            Card(
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
              border =
                androidx.compose.foundation.BorderStroke(
                  1.dp,
                  MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("remote_gist_card_${gist.id}")
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                  ) {
                    Text(
                      text = filename,
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                      imageVector = if (isPublic) Icons.Default.Public else Icons.Default.Lock,
                      contentDescription = if (isPublic) "Public Gist" else "Secret Gist",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(12.dp)
                    )
                  }

                  // Owner avatar + login
                  gist.owner?.let { owner ->
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.End
                    ) {
                      AsyncImage(
                        model = owner.avatarUrl,
                        contentDescription = "User Avatar",
                        modifier =
                          Modifier.size(18.dp)
                            .clip(CircleShape)
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = owner.login ?: "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                  text = description,
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier =
                        Modifier.background(ActivePurpleContainer, RoundedCornerShape(6.dp))
                          .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text(
                        text = "$totalFiles File(s)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkPurpleText
                      )
                    }

                    val gistId = gist.id
                    if (gistId != null) {
                      val isThisGistForking = isForking == gistId
                      OutlinedButton(
                        onClick = { onForkClick(gistId) },
                        enabled = isForking == null,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier =
                          Modifier.height(28.dp).testTag("remote_gist_fork_button_$gistId"),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ActivePurple),
                        border =
                          androidx.compose.foundation.BorderStroke(
                            1.dp,
                            ActivePurple.copy(alpha = 0.5f)
                          )
                      ) {
                        if (isThisGistForking) {
                          CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = ActivePurple
                          )
                        } else {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                              imageVector = Icons.AutoMirrored.Filled.CallSplit,
                              contentDescription = "Fork Gist",
                              modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fork", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                          }
                        }
                      }
                    }
                  }

                  gist.createdAt?.let { dateStr ->
                    // Simple substring of timestamp e.g. "2026-07-11T12:51:17Z" -> "2026-07-11
                    // 12:51"
                    val formattedDate =
                      try {
                        if (dateStr.length >= 16) {
                          dateStr.replace("T", " ").substring(0, 16)
                        } else {
                          dateStr
                        }
                      } catch (e: Exception) {
                        dateStr
                      }
                    Text(
                      text = "Created: $formattedDate",
                      fontSize = 9.sp,
                      color = MaterialTheme.colorScheme.outline
                    )
                  }
                }
              }
            }
          }

          if (gists.size > 10) {
            Text(
              text = "Showing top 10 Gists. View other Gists on your GitHub profile.",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.outline,
              modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
            )
          }
        }
      }
    }
  }
}
