package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles
import com.example.data.repository.SyncStatus

/**
 * Status dashboard view displaying the current synchronization state ('Synced', 'Pending',
 * 'Offline') for locally modified Gists.
 */
@Composable
fun SyncStatusDashboardView(
  gists: List<GistWithFiles>,
  isSyncing: Boolean,
  isOnline: Boolean,
  onSyncClick: () -> Unit,
  lastSyncTime: Long = 0L,
  syncStatus: SyncStatus = SyncStatus.Idle,
  modifier: Modifier = Modifier
) {
  val unsyncedGists =
    remember(gists) { gists.filter { it.gist.isLocalOnly || it.gist.isDirty || it.gist.isDeleted } }

  val overallStateLabel =
    when {
      !isOnline -> "Offline"
      unsyncedGists.isNotEmpty() -> "Pending"
      else -> "Synced"
    }

  val overallStateColor =
    when {
      !isOnline -> MaterialTheme.colorScheme.outline
      unsyncedGists.isNotEmpty() -> MaterialTheme.colorScheme.tertiary
      else -> MaterialTheme.colorScheme.primary
    }

  val overallStateContainerColor =
    when {
      !isOnline -> MaterialTheme.colorScheme.surfaceVariant
      unsyncedGists.isNotEmpty() -> MaterialTheme.colorScheme.tertiaryContainer
      else -> MaterialTheme.colorScheme.primaryContainer
    }

  Card(
    modifier = modifier.fillMaxWidth().testTag("sync_status_dashboard"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      // Header with Overall Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Synchronization Dashboard",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text =
              if (lastSyncTime > 0) "Last synced: ${formatTimestamp(lastSyncTime)}"
              else "Not synced yet",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          shape = RoundedCornerShape(20.dp),
          color = overallStateContainerColor,
          modifier = Modifier.testTag("overall_status_badge")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(overallStateColor))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = overallStateLabel,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.testTag("status_text_${overallStateLabel.lowercase()}")
            )
          }
        }
      }

      if (syncStatus is SyncStatus.Error) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth().testTag("dashboard_sync_error_banner")
        ) {
          Text(
            text = "Sync status: ${syncStatus.errorMessage}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(8.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Summary Metrics Row
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricBox(
          label = "Total Local",
          value = "${gists.size}",
          modifier = Modifier.weight(1f).testTag("metric_total_gists")
        )
        MetricBox(
          label = "Pending Sync",
          value = "${unsyncedGists.size}",
          highlightColor =
            if (unsyncedGists.isNotEmpty()) MaterialTheme.colorScheme.tertiary else null,
          modifier = Modifier.weight(1f).testTag("metric_pending_sync")
        )
        MetricBox(
          label = "Network",
          value = if (isOnline) "Online" else "Offline",
          highlightColor = if (!isOnline) MaterialTheme.colorScheme.error else null,
          modifier = Modifier.weight(1f).testTag("metric_network_state")
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // List of Locally Modified Gists
      if (unsyncedGists.isNotEmpty()) {
        Text(
          text = "Locally Modified Gists (${unsyncedGists.size})",
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          unsyncedGists.forEach { item ->
            val filename = item.files.firstOrNull()?.filename ?: "untitled"
            val itemStateLabel =
              when {
                item.gist.isDeleted -> "Pending Delete"
                item.gist.isLocalOnly -> "Draft"
                item.gist.isDirty -> "Modified"
                else -> "Synced"
              }
            val syncStateText =
              when {
                !isOnline -> "Offline"
                isSyncing -> "Syncing..."
                else -> "Pending"
              }

            Surface(
              modifier =
                Modifier.fillMaxWidth()
                  .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp)
                  )
                  .testTag("locally_modified_gist_item_${item.gist.id}"),
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = filename,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = item.gist.description?.takeIf { it.isNotBlank() } ?: "No description",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color =
                      when {
                        item.gist.isDeleted -> MaterialTheme.colorScheme.errorContainer
                        item.gist.isLocalOnly -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                      }
                  ) {
                    Text(
                      text = itemStateLabel,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }

                  Spacer(modifier = Modifier.width(6.dp))

                  Text(
                    text = "($syncStateText)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }

      // Sync Action Button
      Button(
        onClick = onSyncClick,
        enabled = !isSyncing && unsyncedGists.isNotEmpty() && isOnline,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("dashboard_sync_button")
      ) {
        if (isSyncing) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Syncing with GitHub...", fontSize = 14.sp)
        } else {
          Icon(
            imageVector = if (!isOnline) Icons.Default.CloudOff else Icons.Default.Sync,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text =
              when {
                !isOnline -> "Offline (Sync Unavailable)"
                unsyncedGists.isEmpty() -> "All Gists Synced"
                else -> "Push ${unsyncedGists.size} Pending Change(s)"
              },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun MetricBox(
  label: String,
  value: String,
  highlightColor: androidx.compose.ui.graphics.Color? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = highlightColor?.copy(alpha = 0.15f) ?: MaterialTheme.colorScheme.surfaceVariant,
    border =
      BorderStroke(
        1.dp,
        highlightColor?.copy(alpha = 0.3f) ?: MaterialTheme.colorScheme.outlineVariant
      )
  ) {
    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = value,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = highlightColor ?: MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = label,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}

private fun formatTimestamp(timeMs: Long): String {
  if (timeMs <= 0L) return "Never"
  val date = java.util.Date(timeMs)
  val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
  return sdf.format(date)
}
