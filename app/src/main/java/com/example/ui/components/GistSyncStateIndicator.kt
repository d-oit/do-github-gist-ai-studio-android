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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistEntity
import com.example.ui.theme.DarkRedText
import com.example.ui.theme.LightPinkContainer

/** Sync state model representing Room database sync status flags. */
enum class GistSyncState {
  LOCAL_ONLY,
  DIRTY,
  PENDING_DELETE,
  PENDING,
  SYNCED,
  ERROR
}

/** Unified 3-state sync status: synced, pending, or error. */
enum class SyncStatusType {
  SYNCED,
  PENDING,
  ERROR
}

/**
 * Determines [GistSyncState] from Room flags [isLocalOnly], [isDirty], [isDeleted], and [hasError].
 */
fun resolveGistSyncState(
  isLocalOnly: Boolean,
  isDirty: Boolean,
  isDeleted: Boolean = false,
  hasError: Boolean = false
): GistSyncState {
  return when {
    hasError -> GistSyncState.ERROR
    isDeleted -> GistSyncState.PENDING_DELETE
    isLocalOnly -> GistSyncState.LOCAL_ONLY
    isDirty -> GistSyncState.DIRTY
    else -> GistSyncState.SYNCED
  }
}

/** Resolves unified 3-state [SyncStatusType] from sync flags. */
fun resolveSyncStatusType(
  isLocalOnly: Boolean,
  isDirty: Boolean,
  isDeleted: Boolean = false,
  hasError: Boolean = false
): SyncStatusType {
  return when {
    hasError -> SyncStatusType.ERROR
    isLocalOnly || isDirty || isDeleted -> SyncStatusType.PENDING
    else -> SyncStatusType.SYNCED
  }
}

/** Determines [GistSyncState] directly from a [GistEntity]. */
fun GistEntity.toSyncState(): GistSyncState = resolveGistSyncState(isLocalOnly, isDirty, isDeleted)

/** Visual attributes for displaying sync state badges and banners. */
data class SyncStateVisuals(
  val title: String,
  val description: String,
  val icon: ImageVector,
  val containerColor: Color,
  val borderColor: Color,
  val contentColor: Color,
  val testTagSuffix: String
)

@Composable
fun getSyncStateVisuals(state: GistSyncState): SyncStateVisuals {
  return when (state) {
    GistSyncState.LOCAL_ONLY ->
      SyncStateVisuals(
        title = "Local Only",
        description = "Created offline. Pending initial push to GitHub.",
        icon = Icons.Default.CloudOff,
        containerColor = LightPinkContainer,
        borderColor = Color(0xFFF2B8B5),
        contentColor = DarkRedText,
        testTagSuffix = "local_only"
      )
    GistSyncState.DIRTY ->
      SyncStateVisuals(
        title = "Unpushed Edits",
        description = "Modified locally. Changes pending push to GitHub.",
        icon = Icons.Default.Sync,
        containerColor = Color(0xFFFFF3E0),
        borderColor = Color(0xFFFFE082),
        contentColor = Color(0xFFE65100),
        testTagSuffix = "dirty"
      )
    GistSyncState.PENDING ->
      SyncStateVisuals(
        title = "Pending Sync",
        description = "Changes queued for synchronization with GitHub.",
        icon = Icons.Default.Sync,
        containerColor = Color(0xFFFFF3E0),
        borderColor = Color(0xFFFFE082),
        contentColor = Color(0xFFE65100),
        testTagSuffix = "pending"
      )
    GistSyncState.PENDING_DELETE ->
      SyncStateVisuals(
        title = "Pending Delete",
        description = "Marked for deletion. Pending removal from GitHub.",
        icon = Icons.Default.Delete,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        borderColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        testTagSuffix = "deleted"
      )
    GistSyncState.ERROR ->
      SyncStateVisuals(
        title = "Sync Error",
        description = "Synchronization failed. Tap to retry or review error.",
        icon = Icons.Default.ErrorOutline,
        containerColor = Color(0xFFFFEBEE),
        borderColor = Color(0xFFEF9A9A),
        contentColor = Color(0xFFC62828),
        testTagSuffix = "error"
      )
    GistSyncState.SYNCED ->
      SyncStateVisuals(
        title = "Pushed to GitHub",
        description = "In sync. Local content matches GitHub.",
        icon = Icons.Default.CloudDone,
        containerColor = Color(0xFFE8F5E9),
        borderColor = Color(0xFFA5D6A7),
        contentColor = Color(0xFF2E7D32),
        testTagSuffix = "synced"
      )
  }
}

/**
 * A compact badge component that tracks whether local Gist changes have been pushed to GitHub using
 * the Room database sync status flags or unified 3-state sync status (synced, pending, or error).
 */
@Composable
fun GistSyncStateIndicator(
  isLocalOnly: Boolean = false,
  isDirty: Boolean = false,
  isDeleted: Boolean = false,
  hasError: Boolean = false,
  statusType: SyncStatusType? = null,
  modifier: Modifier = Modifier,
  compact: Boolean = true,
  onClick: (() -> Unit)? = null
) {
  val syncState =
    when {
      statusType != null ->
        when (statusType) {
          SyncStatusType.SYNCED -> GistSyncState.SYNCED
          SyncStatusType.PENDING -> GistSyncState.PENDING
          SyncStatusType.ERROR -> GistSyncState.ERROR
        }
      else -> resolveGistSyncState(isLocalOnly, isDirty, isDeleted, hasError)
    }
  val visuals = getSyncStateVisuals(syncState)

  val rootModifier =
    modifier.testTag("sync_status_${visuals.testTagSuffix}").run {
      if (onClick != null) clickable { onClick() } else this
    }

  if (compact) {
    Box(
      modifier =
        rootModifier
          .background(visuals.containerColor, RoundedCornerShape(8.dp))
          .border(1.dp, visuals.borderColor, RoundedCornerShape(8.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = visuals.icon,
          contentDescription = visuals.title,
          tint = visuals.contentColor,
          modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = visuals.title,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = visuals.contentColor
        )
      }
    }
  } else {
    GistSyncStateBanner(syncState = syncState, modifier = rootModifier)
  }
}

/**
 * Dedicated visual icon component representing the sync state:
 * - [SyncStatusType.SYNCED]: CloudDone icon in green
 * - [SyncStatusType.PENDING]: Sync icon in amber/orange
 * - [SyncStatusType.ERROR]: ErrorOutline icon in red
 */
@Composable
fun SyncStatusIcon(
  status: SyncStatusType,
  modifier: Modifier = Modifier,
  contentDescription: String? = null
) {
  val (icon, tint) =
    when (status) {
      SyncStatusType.SYNCED -> Icons.Default.CloudDone to Color(0xFF2E7D32)
      SyncStatusType.PENDING -> Icons.Default.Sync to Color(0xFFE65100)
      SyncStatusType.ERROR -> Icons.Default.ErrorOutline to Color(0xFFC62828)
    }
  Icon(
    imageVector = icon,
    contentDescription = contentDescription ?: status.name.lowercase(),
    tint = tint,
    modifier = modifier.size(16.dp).testTag("sync_status_icon_${status.name.lowercase()}")
  )
}

/** A dedicated interactive chip representing the 3-state sync status (synced, pending, error). */
@Composable
fun SyncStatusChip(
  status: SyncStatusType,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null
) {
  GistSyncStateIndicator(
    statusType = status,
    modifier = modifier,
    compact = true,
    onClick = onClick
  )
}

/**
 * A detailed banner card displaying the current synchronization status of local Gist changes
 * relative to GitHub.
 */
@Composable
fun GistSyncStateBanner(
  syncState: GistSyncState,
  modifier: Modifier = Modifier,
  showDescription: Boolean = true
) {
  val visuals = getSyncStateVisuals(syncState)

  Card(
    modifier = modifier.fillMaxWidth().testTag("gist_sync_state_banner_${visuals.testTagSuffix}"),
    colors = CardDefaults.cardColors(containerColor = visuals.containerColor),
    border = androidx.compose.foundation.BorderStroke(1.dp, visuals.borderColor),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start
    ) {
      Box(
        modifier =
          Modifier.background(visuals.borderColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp)
      ) {
        Icon(
          imageVector = visuals.icon,
          contentDescription = null,
          tint = visuals.contentColor,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = visuals.title,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = visuals.contentColor
        )
        if (showDescription) {
          Text(
            text = visuals.description,
            fontSize = 11.sp,
            color = visuals.contentColor.copy(alpha = 0.8f)
          )
        }
      }
    }
  }
}
