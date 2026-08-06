package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formats a last sync timestamp into a human-readable relative or absolute time string. */
fun formatLastSyncTime(timestamp: Long, currentTime: Long = System.currentTimeMillis()): String {
  if (timestamp <= 0L) return "Never synced"

  val diffMs = currentTime - timestamp
  if (diffMs < 0L) return "Just now"

  val diffSec = diffMs / 1000L
  val diffMin = diffSec / 60L
  val diffHours = diffMin / 60L
  val diffDays = diffHours / 24L

  return when {
    diffSec < 60L -> "Just now"
    diffMin < 60L -> "${diffMin}m ago"
    diffHours < 24L -> "${diffHours}h ago"
    diffDays < 7L -> "${diffDays}d ago"
    else -> {
      val formatter = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
      formatter.format(Date(timestamp))
    }
  }
}

private data class StatusUiState<A, B, C, D>(val text: A, val bg: B, val fg: C, val icon: D)

/**
 * A status bar component that displays the 'Last Synced' timestamp from the local Room database and
 * current sync status to improve user trust in data freshness.
 */
@Composable
fun SyncStatusBar(
  lastSyncTime: Long,
  syncStatus: SyncStatus,
  isOfflineOnly: Boolean = false,
  modifier: Modifier = Modifier
) {
  val formattedTime = formatLastSyncTime(lastSyncTime)

  val uiState =
    when {
      isOfflineOnly ->
        StatusUiState(
          "Paused",
          MaterialTheme.colorScheme.errorContainer,
          MaterialTheme.colorScheme.onErrorContainer,
          Icons.Default.CloudOff
        )
      syncStatus is SyncStatus.Syncing ->
        StatusUiState(
          "Syncing...",
          MaterialTheme.colorScheme.primaryContainer,
          MaterialTheme.colorScheme.onPrimaryContainer,
          null
        )
      syncStatus is SyncStatus.Success ->
        StatusUiState(
          "Synced",
          MaterialTheme.colorScheme.secondaryContainer,
          MaterialTheme.colorScheme.onSecondaryContainer,
          Icons.Default.CloudDone
        )
      syncStatus is SyncStatus.Error ->
        StatusUiState(
          "Sync Error",
          MaterialTheme.colorScheme.errorContainer,
          MaterialTheme.colorScheme.onErrorContainer,
          Icons.Default.ErrorOutline
        )
      else -> {
        if (lastSyncTime > 0L) {
          StatusUiState(
            "Synced",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Default.CloudDone
          )
        } else {
          StatusUiState(
            "Not Synced",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.CloudOff
          )
        }
      }
    }

  val statusText = uiState.text
  val statusBg = uiState.bg
  val statusFg = uiState.fg
  val statusIcon = uiState.icon

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .testTag("sync_status_bar"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
      Icon(
        imageVector = Icons.Default.Sync,
        contentDescription = "Last Synced Icon",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Last Synced: $formattedTime",
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("last_synced_text")
      )
    }

    // Status Badge
    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(6.dp))
          .background(statusBg)
          .padding(horizontal = 6.dp, vertical = 2.dp)
          .testTag("sync_status_indicator"),
      contentAlignment = Alignment.Center
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (syncStatus is SyncStatus.Syncing) {
          CircularProgressIndicator(
            modifier = Modifier.size(10.dp),
            strokeWidth = 1.5.dp,
            color = statusFg
          )
          Spacer(modifier = Modifier.width(4.dp))
        } else if (statusIcon != null) {
          Icon(
            imageVector = statusIcon,
            contentDescription = null,
            tint = statusFg,
            modifier = Modifier.size(11.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text = statusText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusFg)
      }
    }
  }
}
