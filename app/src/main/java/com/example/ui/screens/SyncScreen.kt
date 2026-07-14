package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles
import com.example.ui.components.GitHubGistApiList

@Composable
fun SyncScreen(
  gists: List<GistWithFiles>,
  isSyncing: Boolean,
  onSyncClick: () -> Unit,
  remoteGists: List<com.example.data.remote.model.GistResponse>,
  isFetchingRemote: Boolean,
  remoteError: String?,
  onRefreshRemote: () -> Unit
) {
  val unsynced =
    remember(gists) { gists.filter { it.gist.isLocalOnly || it.gist.isDirty || it.gist.isDeleted } }
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Top
  ) {
    // Local state card
    Spacer(modifier = Modifier.height(16.dp))
    Box(
      modifier =
        Modifier.size(100.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector =
          if (unsynced.isEmpty()) Icons.Default.CloudDone else Icons.Default.CloudUpload,
        contentDescription = "Sync state",
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(48.dp)
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = if (unsynced.isEmpty()) "All Gists Synchronized" else "Offline Changes Pending",
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text =
        if (unsynced.isEmpty()) {
          "Your local repository matches your cloud profile perfectly."
        } else {
          "You have ${unsynced.size} unsaved changes waiting to be pushed online."
        },
      fontSize = 14.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      modifier = Modifier.padding(horizontal = 24.dp)
    )

    if (unsynced.isNotEmpty()) {
      Spacer(modifier = Modifier.height(16.dp))

      Surface(
        modifier =
          Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "Pending Queue",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          unsynced.forEach { item ->
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = item.files.firstOrNull()?.filename ?: "untitled",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )
              Box(
                modifier =
                  Modifier.background(
                      when {
                        item.gist.isDeleted -> MaterialTheme.colorScheme.errorContainer
                        item.gist.isLocalOnly -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primaryContainer
                      },
                      RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text =
                    when {
                      item.gist.isDeleted -> "Delete"
                      item.gist.isLocalOnly -> "Draft"
                      else -> "Dirty"
                    },
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color =
                    when {
                      item.gist.isDeleted -> MaterialTheme.colorScheme.onErrorContainer
                      item.gist.isLocalOnly -> MaterialTheme.colorScheme.onError
                      else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      onClick = onSyncClick,
      enabled = !isSyncing && unsynced.isNotEmpty(),
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sync_submit_button")
    ) {
      if (isSyncing) {
        CircularProgressIndicator(
          modifier = Modifier.size(24.dp),
          color = MaterialTheme.colorScheme.onPrimary
        )
      } else {
        Text("Sync with GitHub", fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Direct GitHub Gists list component from GITHUB_PAT
    GitHubGistApiList(
      gists = remoteGists,
      isFetching = isFetchingRemote,
      error = remoteError,
      onRefresh = onRefreshRemote,
      modifier = Modifier.padding(bottom = 24.dp)
    )
  }
}
